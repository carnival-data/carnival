package carnival.core.graph



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

import carnival.core.config.Defaults
import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.VertexDefinition
import carnival.graph.DynamicVertexDef
import carnival.graph.VertexBuilder





/**
 * The core graph.  See the documentation for model details.
 *
 */
@Slf4j
abstract class CoreGraph implements GremlinTrait {

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

	/** Graph object is added by GremlinTrait. */
	//Graph graph

	/** */
	CoreGraphSchema graphSchema

	/** */
	GraphValidator graphValidator



	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** */
	protected CoreGraph(Graph graph, CoreGraphSchema graphSchema, GraphValidator graphValidator) {
		assert graph
		assert graphSchema
		assert graphValidator
		this.graph = graph
		this.graphSchema = graphSchema
		this.graphValidator = graphValidator
	}



	///////////////////////////////////////////////////////////////////////////
	// INITIALIZATION 
	///////////////////////////////////////////////////////////////////////////

	/** */
	public void initializeGremlinGraph(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "CoreGraph initializeGremlinGraph graph:$graph g:$g packageName:$packageName"

		initializeDefinedVertices(graph, g, packageName)
		initializeDefinedEdges(graph, g, packageName)
		createVertexBuilders(graph, g)
	}

	/** */
	public void initializeGremlinGraph(Graph graph, GraphTraversalSource g) {
		log.info "CoreGraph initializeGremlinGraph graph:$graph g:$g"
		initializeGremlinGraph(graph, g, 'carnival')
	}
	

	///////////////////////////////////////////////////////////////////////////
	// GRAPH MODEL - GENERIC METHODS
	///////////////////////////////////////////////////////////////////////////

	/** */
	public void addConstraints(Graph graph, GraphTraversalSource g, Class defClass) {
		assert graph
		assert g
		assert defClass

		def defInterfaces = defClass.getInterfaces()
		if (defInterfaces.contains(VertexDefinition)) addVertexConstraints(graph, g, defClass)
		else if (defInterfaces.contains(EdgeDefinition)) addEdgeConstraints(graph, g, defClass)
		else throw new RuntimeException("unrecognized definition class: $defClass")
	}


	/** */
	public void addConstraints(Class defClass) {
		assert defClass
		withGremlin { graph, g ->
			addConstraints(graph, g, defClass)
		}
	}



	///////////////////////////////////////////////////////////////////////////
	// GRAPH MODEL - VERTICES
	///////////////////////////////////////////////////////////////////////////

	/** */
    public void initializeDefinedVertices(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "CoreGraph initializeDefinedVertices graph:$graph g:$g packageName:$packageName"
		
		assert graph
		assert g
		assert packageName

		Set<VertexConstraint> newDefinitions = findNewVertexConstraints(packageName)

		withTransactionIfSupported(graph) {
	        newDefinitions.each { vld ->
				addConstraint(graph, g, vld)
	        }
	        newDefinitions.each { vld ->
				createClassVertex(graph, g, vld)
			}
	        newDefinitions.each { vld ->
				connectClassVertices(graph, g, vld)
			}
		}
    }


	/** */
	public void addConstraint(Graph graph, GraphTraversalSource g, VertexConstraint vld) {
		log.trace "addConstraint vld: ${vld.label} $vld"

		log.trace "adding vertex definition to graph schema ${vld.label} ${vld}"
		graphSchema.vertexConstraints << vld
	}


	/** */
	public void createClassVertex(Graph graph, GraphTraversalSource g, VertexConstraint vld) {
		log.trace "createClassVertex vld: ${vld.label} $vld"

		// create class singleton vertex, which can only be done if there
		// are no required properties
		def vdef = vld.vertexDef
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
	public void connectClassVertices(Graph graph, GraphTraversalSource g, VertexConstraint vld) {
		def vdef = vld.vertexDef

		if (vdef.superClass) {
			log.trace "set superclass to ${vdef.superClass}"
			assert vdef.isClass()
			assert vdef.superClass.isClass()
			assert vdef.vertex
			assert vdef.superClass.vertex
			vdef.setSubclassOf(g, vdef.superClass)
		}
	}



	/** */
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


    /** */
    public Collection<VertexConstraint> findNewVertexConstraints(String packageName) {
		log.info "CoreGraph findNewVertexConstraints packageName:$packageName"
		assert packageName

    	Set<Class<VertexDefinition>> vertexDefClasses = findVertexDefClases(packageName)
		if (!vertexDefClasses) return new HashSet<VertexConstraint>()

		findNewVertexConstraints(vertexDefClasses)
	}


    /** */
    public Collection<VertexConstraint> findNewVertexConstraints(Set<Class<VertexDefinition>> vertexDefClasses) {
		assert vertexDefClasses
    	
		Set<VertexConstraint> existingDefinitions = graphSchema.getVertexConstraints()
		Set<VertexConstraint> newDefinitions = new HashSet<VertexConstraint>()

        vertexDefClasses.each { Class vdc ->
        	log.trace "findNewVertexConstraints vdc: $vdc"

            vdc.values().each { VertexDefinition vdef ->
            	log.trace "findNewVertexConstraints vdef: $vdef"

            	// check if already defined
            	def found = existingDefinitions.find {
            		it.label == vdef.label && it.nameSpace == vdef.nameSpace
            	}
            	if (!found) {
					def vld = VertexConstraint.create(vdef)
					log.trace "found new vertex definition ${vld.label} ${vld}"
	            	newDefinitions.add(vld)
            	}
            }
        }

        return newDefinitions
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



	///////////////////////////////////////////////////////////////////////////
	// GRAPH MODEL - EDGES
	///////////////////////////////////////////////////////////////////////////

	public void addConstraint(Graph graph, GraphTraversalSource g, EdgeConstraint relDef) {
		log.trace "addConstraint relDef: ${relDef.label} $relDef"

		log.trace "adding edge definition to graph schema ${relDef.label} ${relDef}"
		graphSchema.edgeConstraints.add(relDef)
	}


	/** */
	public void addEdgeConstraints(Graph graph, GraphTraversalSource g, Class<EdgeDefinition> edc) {
		Set<EdgeConstraint> existingDefinitions = graphSchema.getEdgeConstraints()
		edc.values().each { EdgeDefinition edef ->
			def found = existingDefinitions.find {
				it.label == edef.label && it.nameSpace == edef.nameSpace
			}
			if (!found) {
				def relDef = EdgeConstraint.create(edef)
				addConstraint(graph, g, relDef)
			}
		}
	}


	/** */
    public void initializeDefinedEdges(Graph graph, GraphTraversalSource g, String packageName) {
		assert graph
		assert g
		assert packageName

		Set<EdgeConstraint> newDefinitions = findNewEdgeConstraints(packageName)

		withTransactionIfSupported(graph) {
	        newDefinitions.each { eld ->
				addConstraint(graph, g, eld)
	        }
		}
    }	


    /** */
    public Collection<EdgeConstraint> findNewEdgeConstraints(String packageName) {
		log.info "CoreGraph findNewEdgeConstraints packageName:$packageName"
		assert packageName
    	
		Set<Class<EdgeDefinition>> defClasses = findEdgeDefClases(packageName)
		if (!defClasses) return new HashSet<EdgeConstraint>()
		
		findNewEdgeConstraints(defClasses)
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
	// CONTROLLED INSTANCES
	///////////////////////////////////////////////////////////////////////////

	/** */
	public List<Vertex> createVertexBuilders(Graph graph, GraphTraversalSource g) {
		log.trace "createVertexBuilders vertexBuilders:${graphSchema.vertexBuilders}"

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
