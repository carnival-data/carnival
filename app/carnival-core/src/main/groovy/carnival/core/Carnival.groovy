package carnival.core



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.reflections.Reflections

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.structure.Graph.Features.GraphFeatures

import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.ElementDefinition
import carnival.graph.VertexDefinition
import carnival.graph.DynamicVertexDef
import carnival.graph.VertexBuilder
import carnival.core.graph.GremlinTrait
import carnival.core.graph.GraphSchema
import carnival.core.graph.DefaultGraphSchema
import carnival.core.graph.GraphValidator
import carnival.core.graph.GraphValidationError
import carnival.core.graph.DefaultGraphValidator
import carnival.core.graph.EdgeConstraint
import carnival.core.graph.VertexConstraint





/**
 * The Carnival object.
 *
 * A Carnival is comprosed of three main components:
 *    - a Gremlin graph
 *    - a graph schema
 *    - a graph validator
 *
 */
@Slf4j
abstract class Carnival implements GremlinTrait {

	///////////////////////////////////////////////////////////////////////////
	// UTILITY
	///////////////////////////////////////////////////////////////////////////

	/** */
	static public void withTransaction(Graph graph, Closure cl) {
		def tx = graph.tx()
		if (tx.isOpen()) tx.close()
		tx.open()

        def maxClosureParams = cl.getMaximumNumberOfParameters()

		try {
			if (maxClosureParams == 0) {
				cl()
			} else if (maxClosureParams == 1) {
				cl(tx)
			}
		} finally {
			tx.commit()
			tx.close()
		}
	}


	/** */
	static public void withTransactionIfSupported(Graph graph, Closure cl) {
		def transactionsAreSupported = graph.features().graph().supportsTransactions()
		log.trace "transactionsAreSupported:${transactionsAreSupported}"

		def tx
		if (transactionsAreSupported) {
			tx = graph.tx()
			if (tx.isOpen()) tx.close()
			tx.open()
		}

        def maxClosureParams = cl.getMaximumNumberOfParameters()

		try {
			if (maxClosureParams == 0) {
				cl()
			} else if (maxClosureParams == 1) {
				if (transactionsAreSupported) cl(tx)
				else cl()
			}
		} finally {
			if (transactionsAreSupported) {
				tx.commit()
				tx.close()
			}
		}
	}


	/** */
	static protected void combine(EdgeConstraint relDef, EdgeDefinition edgeDef) {
		assert relDef
		assert edgeDef

		edgeDef.domainLabels.each { dl ->
			if (!relDef.domainLabels.contains(dl)) relDef.domainLabels << dl
		}

		edgeDef.rangeLabels.each { rl ->
			if (!relDef.rangeLabels.contains(rl)) relDef.rangeLabels << rl
		}
	}



	///////////////////////////////////////////////////////////////////////////
	// INSTANCE
	///////////////////////////////////////////////////////////////////////////

	/** A gremlin Graph object is added by GremlinTrait. */
	//Graph graph

	/** */
	GraphSchema graphSchema

	/** */
	GraphValidator graphValidator



	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** */
	protected Carnival(Graph graph, GraphSchema graphSchema, GraphValidator graphValidator) {
		assert graph
		assert graphSchema
		assert graphValidator
		this.graph = graph
		this.graphSchema = graphSchema
		this.graphValidator = graphValidator
	}



	///////////////////////////////////////////////////////////////////////////
	// INITIALIZATION
	//
	// Initialize the graph from models defined in code.  Operates at the Java
	// package level. 
	///////////////////////////////////////////////////////////////////////////

	/** */
	public void initializeGremlinGraph(Graph graph, GraphTraversalSource g) {
		log.info "Carnival initializeGremlinGraph graph:$graph g:$g"
		initializeGremlinGraph(graph, g, 'carnival')
	}

	
	/** */
	public void initializeGremlinGraph(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "Carnival initializeGremlinGraph graph:$graph g:$g packageName:$packageName"

		initializeDefinedVertices(graph, g, packageName)
		initializeDefinedEdges(graph, g, packageName)
		initializeGraphSchemaVertices(graph, g)
	}


	/** */
    public void initializeDefinedVertices(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "Carnival initializeDefinedVertices graph:$graph g:$g packageName:$packageName"
		
		assert graph
		assert g
		assert packageName

		Set<VertexConstraint> vertexConstraints = findNewVertexConstraints(packageName)
		vertexConstraints.each { vc ->
			addConstraint(vc)
		}

		withTransactionIfSupported(graph) {
			initializeClassVertices(graph, g, vertexConstraints)
		}
    }


    /** */
    public Collection<VertexConstraint> findNewVertexConstraints(String packageName) {
		log.info "Carnival findNewVertexConstraints packageName:$packageName"
		assert packageName

    	Set<Class<VertexDefinition>> vertexDefClasses = findVertexDefClases(packageName)
		if (!vertexDefClasses) return new HashSet<VertexConstraint>()

		findNewVertexConstraints(vertexDefClasses)
	}


    /** */
    public Set<Class<VertexDefinition>> findVertexDefClases(String packageName) {
    	// find all vertex defs
    	Reflections reflections = new Reflections(packageName)
    	Set<Class<VertexDefinition>> classes = reflections.getSubTypesOf(VertexDefinition.class)
    	log.trace "findVertexDefClases classes(${classes?.size()}): $classes"

    	// remove genralized classes
    	classes.remove(carnival.graph.DynamicVertexDef)
    	log.trace "findVertexDefClases classes(${classes?.size()}): $classes"

    	return classes
    }


	/** */
    public void initializeDefinedEdges(Graph graph, GraphTraversalSource g, String packageName) {
		assert graph
		assert g
		assert packageName

		Set<EdgeConstraint> edgeConstraints = findNewEdgeConstraints(packageName)
		edgeConstraints.each { ec ->
			addConstraint(ec)
		}
    }	


    /** */
    public Collection<EdgeConstraint> findNewEdgeConstraints(String packageName) {
		log.info "Carnival findNewEdgeConstraints packageName:$packageName"
		assert packageName
    	
		Set<Class<EdgeDefinition>> defClasses = findEdgeDefClases(packageName)
		if (!defClasses) return new HashSet<EdgeConstraint>()
		
		findNewEdgeConstraints(defClasses)
	}


	/** */
    public Set<Class<EdgeDefinition>> findEdgeDefClases(String packageName) {
    	// find all vertex defs
    	Reflections reflections = new Reflections(packageName)
    	Set<Class<EdgeDefinition>> classes = reflections.getSubTypesOf(EdgeDefinition.class)
    	log.trace "findEdgeDefClases classes: $classes"

    	// remove genralized classes
    	classes.remove(carnival.graph.DynamicVertexDef)
    	log.trace "findEdgeDefClases classes: $classes"

    	return classes
    }



	///////////////////////////////////////////////////////////////////////////
	// GRAPH CONSTRAINTS - GENERIC
	///////////////////////////////////////////////////////////////////////////

	public void addModel(Class<ElementDefinition> defClass) {
		assert defClass
		withGremlin { graph, g ->
			addModel(graph, g, defClass)
		}
	}

	/**
	 * Add a model.
	 *
	 */
	public void addModel(Graph graph, GraphTraversalSource g, Class<ElementDefinition> defClass) {
		assert graph
		assert g
		assert defClass

		def defInterfaces = defClass.getInterfaces()
		if (defInterfaces.contains(VertexDefinition)) addVertexModel(graph, g, defClass)
		else if (defInterfaces.contains(EdgeDefinition)) addEdgeModel(graph, g, defClass)
		else throw new RuntimeException("unrecognized definition class: $defClass")

	}


	public void addVertexModel(Graph graph, GraphTraversalSource g, Class<VertexDefinition> defClass) {
		assert graph
		assert g
		assert defClass

		Set<VertexConstraint> vertexConstraints = findNewVertexConstraints(defClass)
		vertexConstraints.each { vc ->
			addConstraint(vc)
		}
		withTransactionIfSupported(graph) {
			initializeClassVertices(graph, g, vertexConstraints)
		}
	}


	public void addEdgeModel(Graph graph, GraphTraversalSource g, Class<EdgeDefinition> defClass) {
		assert graph
		assert g
		assert defClass

		Set<EdgeConstraint> edgeConstraints = findNewEdgeConstraints(defClass)
		edgeConstraints.each { ec ->
			addConstraint(ec)
		}
	}

	/*public void addConstraints(Graph graph, GraphTraversalSource g, Class defClass) {
		assert graph
		assert g
		assert defClass

		def defInterfaces = defClass.getInterfaces()
		if (defInterfaces.contains(VertexDefinition)) addVertexConstraints(graph, g, defClass)
		else if (defInterfaces.contains(EdgeDefinition)) addEdgeConstraints(graph, g, defClass)
		else throw new RuntimeException("unrecognized definition class: $defClass")
	}

	public void addConstraints(Class defClass) {
		assert defClass
		withGremlin { graph, g ->
			addConstraints(graph, g, defClass)
		}
	}

	public void addVertexConstraints(Graph graph, GraphTraversalSource g, Class<VertexDefinition> vdc) {
		Set<VertexConstraint> existingDefinitions = graphSchema.getVertexConstraints()
		vdc.values().each { VertexDefinition vdef ->
			def found = existingDefinitions.find {
				it.label == vdef.label && it.nameSpace == vdef.nameSpace
			}
			if (!found) {
				def vld = VertexConstraint.create(vdef)
				addConstraint(graph, g, vld)
			}
		}
	}

	public void addEdgeConstraints(Class<EdgeDefinition> edc) {
		Set<EdgeConstraint> existingDefinitions = graphSchema.getEdgeConstraints()
		edc.values().each { EdgeDefinition edef ->
			def found = existingDefinitions.find {
				it.label == edef.label && it.nameSpace == edef.nameSpace
			}
			if (!found) {
				def edgeConst = EdgeConstraint.create(edef)
				addConstraint(edgeConst)
			}
		}
	}

	public void addVertexModel(Graph graph, GraphTraversalSource g, Class modelClass) {
		assert graph
		assert g
		assert defClass

		Set<VertexConstraint> newConstraints = findNewVertexConstraints(modelClass)
	}

	public void addVertexModel(Class modelClass) {
	}*/



	///////////////////////////////////////////////////////////////////////////
	// GRAPH CONSTRAINTS - VERTEX
	///////////////////////////////////////////////////////////////////////////

	/** */
	public void addConstraint(VertexConstraint vertexConstraint) {
		log.trace "addConstraint vertexConstraint: ${vertexConstraint.label} $vertexConstraint"
		graphSchema.vertexConstraints << vertexConstraint
	}


    /** */
    public Collection<VertexConstraint> findNewVertexConstraints(Set<Class<VertexDefinition>> vertexDefClasses) {
		Set<VertexConstraint> allNewConstraints = new HashSet<VertexConstraint>()
		vertexDefClasses.each { vdc ->
			Set<VertexConstraint> newConstraints = findNewVertexConstraints(vdc)
			allNewConstraints.addAll(newConstraints)
		}
		allNewConstraints
	}


    /** */
    public Collection<VertexConstraint> findNewVertexConstraints(Class<VertexDefinition> vertexDefClass) {
		assert vertexDefClass
    	
		Set<VertexConstraint> existingConstraints = graphSchema.getVertexConstraints()
		Set<VertexConstraint> newConstraints = new HashSet<VertexConstraint>()

		toVertexConstraints(vertexDefClass).each { vc ->
			log.trace "findNewVertexConstraints vc: $vc"
			def exists = existsInGraphSchema(vc)
			if (!exists) newConstraints.add(vc)
		}

        return newConstraints
    }


	/** */
	boolean existsInGraphSchema(VertexConstraint vc) {
		graphSchema.vertexConstraints.find {
			it.label == vc.label && it.nameSpace == vc.nameSpace
		}
	}


    /** */
    public Collection<VertexConstraint> toVertexConstraints(Class<VertexDefinition> vertexDefClass) {
		assert vertexDefClass

		Set<VertexConstraint> constraints = new HashSet<VertexConstraint>()
		vertexDefClass.values().each { VertexDefinition vdef ->
			log.trace "toVertexConstraints vdef: $vdef"

			def vld = VertexConstraint.create(vdef)
			constraints.add(vld)
		}

		return constraints
	}



	///////////////////////////////////////////////////////////////////////////
	// GRAPH CONSTRAINTS - EDGES
	///////////////////////////////////////////////////////////////////////////

	public void addConstraint(EdgeConstraint edgeConst) {
		log.trace "addConstraint edgeConst: ${edgeConst.label} $edgeConst"

		log.trace "adding edge definition to graph schema ${edgeConst.label} ${edgeConst}"
		graphSchema.edgeConstraints.add(edgeConst)
	}


	public Collection<EdgeConstraint> findNewEdgeConstraints(Class<EdgeDefinition> edgeDefClass) {
		assert edgeDefClass
		Set<Class<EdgeDefinition>> edcs = new HashSet<Class<EdgeDefinition>>()
		edcs.add(edgeDefClass)
		findNewEdgeConstraints(edcs)
	}


	/** */
    public Collection<EdgeConstraint> findNewEdgeConstraints(Set<Class<EdgeDefinition>> edgeDefClasses) {
		assert edgeDefClasses
    	
		Set<EdgeConstraint> existingDefinitions = graphSchema.getEdgeConstraints()
		Set<EdgeConstraint> newDefinitions = new HashSet<EdgeConstraint>()

        edgeDefClasses.each { Class edc ->
        	log.trace "findNewEdgeConstraints edc: $edc"

            edc.values().each { EdgeDefinition edef ->
            	log.trace "findNewEdgeConstraints edef: $edef"

            	// check if already defined
            	def found = existingDefinitions.find {
            		it.label == edef.label && it.nameSpace == edef.nameSpace
            	}
            	if (!found) {
					def relDef = EdgeConstraint.create(edef)
					log.trace "found new relationship definition ${relDef.label} ${relDef}"
	            	newDefinitions.add(relDef)
            	}
            }
        }

        return newDefinitions
    }

    
    



	///////////////////////////////////////////////////////////////////////////
	// VERTEX INSTANCES
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * If the graph schema contains vertex builders that should be used to
	 * create initial vertices in a graph, create the vertices from them.
	 *
	 */
	public List<Vertex> initializeGraphSchemaVertices(Graph graph, GraphTraversalSource g) {
		log.trace "initializeGraphSchemaVertices vertexBuilders:${graphSchema.vertexBuilders}"

		List<Vertex> verts = new ArrayList<Vertex>()
		
		withTransactionIfSupported(graph) {
			graphSchema.vertexBuilders.each { ci ->
				log.trace "creating controlled instance ${ci.class.simpleName} $ci"
				assert ci instanceof VertexBuilder
				verts.add(ci.vertex(graph, g))
			}
		}

		return verts
	}


	/** */
	public void initializeClassVertices(Graph graph, GraphTraversalSource g, Set<VertexConstraint> vcs) {
		withTransactionIfSupported(graph) {
	        vcs.each { vc ->
				createClassVertex(graph, g, vc)
			}
	        vcs.each { vc ->
				connectClassVertices(graph, g, vc)
			}
		}
	}


	/** 
	 * Create the singleton vertex for a vertex constraint for a vertex definition if
	 * it represents a class and has no required properties.
	 *
	 */
	public void createClassVertex(Graph graph, GraphTraversalSource g, VertexConstraint vertexConstraint) {
		log.trace "createClassVertex vertexConstraint: ${vertexConstraint.label} $vertexConstraint"

		// create class singleton vertex, which can only be done if there
		// are no required properties
		def vdef = vertexConstraint.vertexDef
		if (vdef.isClass() && vdef.requiredProperties.size() == 0) {
			def ci = graphSchema.vertexBuilders.find {
				it instanceof VertexDefinition && it.vertexDef == vdef
			}

			if (!ci) {
				ci = vdef.instance()
				log.trace "created controlled instance ${ci.vertexDef.label} ${ci}"
				graphSchema.vertexBuilders << ci
			}
			
			vdef.vertex = ci.vertex(graph, g)
			log.trace "created controlled instance vertex ${vdef.label} ${vdef.nameSpace} ${vdef.vertex}"
		}
	}


	/** */
	public void connectClassVertices(Graph graph, GraphTraversalSource g, VertexConstraint vertexConstraint) {
		def vdef = vertexConstraint.vertexDef

		if (vdef.superClass) {
			log.trace "set superclass to ${vdef.superClass}"
			assert vdef.isClass()
			assert vdef.superClass.isClass()
			assert vdef.vertex
			assert vdef.superClass.vertex
			vdef.setSubclassOf(g, vdef.superClass)
		}
	}


	///////////////////////////////////////////////////////////////////////////
	// GRAPH VALIDATION
	///////////////////////////////////////////////////////////////////////////

	/** */
	public List<GraphValidationError> checkConstraints() {
		def g = graph.traversal()
		def res
		try {
			res = this.graphValidator.checkConstraints(g, this.graphSchema)
		} finally {
			if (g) g.close()
		}
		return res
	}


	/** */
	public Collection<String> checkModel() {
		def g = graph.traversal()
		def res
		try {
			res = this.graphValidator.checkModel(g, this.graphSchema)
		} finally {
			if (g) g.close()
		}
		return res
	}


	/** */
	public GraphTraversalSource traversal() {
		return graph.traversal()
	}


	/** */
	public void withTransaction(Closure cl) {
		def tx = graph.tx()
		if (tx.isOpen()) tx.close()
		tx.open()

        def maxClosureParams = cl.getMaximumNumberOfParameters()

        def g
		try {
			if (maxClosureParams == 0) {
				cl()
			} else if (maxClosureParams == 1) {
				cl(tx)
			} else if (maxClosureParams == 2) {
				g = traversal()
				cl(tx, g)
			} else {
				g = traversal()
				cl(tx, g, graph)
			}
		} finally {
			tx.commit()
			tx.close()
			if (g) g.close()
		}
	}

}
