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

import carnival.util.Defaults
import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.VertexDefTrait
import carnival.graph.DynamicVertexDef
import carnival.graph.ControlledInstance





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
	static protected void combine(RelationshipDefinition relDef, EdgeDefTrait edgeDef) {
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
		createControlledInstances(graph, g)
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
	public void addDefinitions(Graph graph, GraphTraversalSource g, Class defClass) {
		assert graph
		assert g
		assert defClass

		def defInterfaces = defClass.getInterfaces()
		if (defInterfaces.contains(VertexDefTrait)) addVertexDefinitions(graph, g, defClass)
		else if (defInterfaces.contains(EdgeDefTrait)) addEdgeDefinitions(graph, g, defClass)
		else throw new RuntimeException("unrecognized definition class: $defClass")
	}


	/** */
	public void addDefinitions(Class defClass) {
		assert defClass
		withGremlin { graph, g ->
			addDefinitions(graph, g, defClass)
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

		List<VertexLabelDefinition> newDefinitions = findNewVertexLabelDefinitions(packageName)

		withTransactionIfSupported(graph) {
	        newDefinitions.each { vld ->
				addDefinition(graph, g, vld)
	        }
		}
    }


	/** */
	public void addDefinition(Graph graph, GraphTraversalSource g, VertexLabelDefinition vld) {
		log.trace "addDefinition vld: ${vld.label} $vld"

		log.trace "adding vertex definition to graph schema ${vld.label} ${vld}"
		graphSchema.dynamicLabelDefinitions << vld

		// add the controlled instance, which can only be done if there
		// are no required properties
		def vdef = vld.vertexDef
		if (vdef.isClass() && vdef.requiredProperties.size() == 0) {
			def ci = graphSchema.controlledInstances.find {
				it instanceof VertexDefTrait && it.vertexDef == vdef
			}

			if (!ci) {
				ci = vdef.controlledInstance()
				log.trace "created controlled instance ${ci.vertexDef.label} ${ci}"
				graphSchema.controlledInstances << ci
			}
			
			vdef.vertex = ci.vertex(graph, g)
			log.trace "created controlled instance vertex ${vdef.label} ${vdef.nameSpace} ${vdef.vertex}"
		}

		// attempt to set super/sub class relationship
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
	public void addVertexDefinitions(Graph graph, GraphTraversalSource g, Class<VertexDefTrait> vdc) {
		List<VertexLabelDefinition> existingDefinitions = graphSchema.getLabelDefinitions()
		vdc.values().each { VertexDefTrait vdef ->
			def found = existingDefinitions.find {
				it.label == vdef.label && it.nameSpace == vdef.nameSpace
			}
			if (!found) {
				def vld = VertexLabelDefinition.create(vdef)
				addDefinition(graph, g, vld)
			}
		}
	}


    /** */
    public Collection<VertexLabelDefinition> findNewVertexLabelDefinitions(String packageName) {
		log.info "CoreGraph findNewVertexLabelDefinitions packageName:$packageName"
		assert packageName

    	Set<Class<VertexDefTrait>> vertexDefClasses = findVertexDefClases(packageName)
		if (!vertexDefClasses) return new ArrayList<VertexLabelDefinition>()

		findNewVertexLabelDefinitions(vertexDefClasses)
	}


    /** */
    public Collection<VertexLabelDefinition> findNewVertexLabelDefinitions(Set<Class<VertexDefTrait>> vertexDefClasses) {
		assert vertexDefClasses
    	
		List<VertexLabelDefinition> existingDefinitions = graphSchema.getLabelDefinitions()
		List<VertexLabelDefinition> newDefinitions = new ArrayList<VertexLabelDefinition>()

        vertexDefClasses.each { Class vdc ->
        	log.trace "findNewVertexLabelDefinitions vdc: $vdc"

            vdc.values().each { VertexDefTrait vdef ->
            	log.trace "findNewVertexLabelDefinitions vdef: $vdef"

            	// check if already defined
            	def found = existingDefinitions.find {
            		it.label == vdef.label && it.nameSpace == vdef.nameSpace
            	}
            	if (!found) {
					def vld = VertexLabelDefinition.create(vdef)
					log.trace "found new vertex definition ${vld.label} ${vld}"
	            	newDefinitions << vld
            	}
            }
        }

        return newDefinitions
    }


    /** */
    public Set<Class<VertexDefTrait>> findVertexDefClases(String packageName) {
    	// find all vertex defs
    	Reflections reflections = new Reflections(packageName)
    	Set<Class<VertexDefTrait>> classes = reflections.getSubTypesOf(VertexDefTrait.class)
    	log.trace "findVertexDefClases classes(${classes?.size()}): $classes"

    	// remove genralized classes
    	classes.remove(carnival.graph.DynamicVertexDef)
    	log.trace "findVertexDefClases classes(${classes?.size()}): $classes"

    	return classes
    }



	///////////////////////////////////////////////////////////////////////////
	// GRAPH MODEL - EDGES
	///////////////////////////////////////////////////////////////////////////

	public void addDefinition(Graph graph, GraphTraversalSource g, RelationshipDefinition relDef) {
		log.trace "addDefinition relDef: ${relDef.label} $relDef"

		log.trace "adding edge definition to graph schema ${relDef.label} ${relDef}"
		graphSchema.dynamicRelationshipDefinitions << relDef
	}


	/** */
	public void addEdgeDefinitions(Graph graph, GraphTraversalSource g, Class<EdgeDefTrait> edc) {
		List<RelationshipDefinition> existingDefinitions = graphSchema.getRelationshipDefinitions()
		edc.values().each { EdgeDefTrait edef ->
			def found = existingDefinitions.find {
				it.label == edef.label && it.nameSpace == edef.nameSpace
			}
			if (!found) {
				def relDef = RelationshipDefinition.create(edef)
				addDefinition(graph, g, relDef)
			}
		}
	}


	/** */
    public void initializeDefinedEdges(Graph graph, GraphTraversalSource g, String packageName) {
		assert graph
		assert g
		assert packageName

		List<RelationshipDefinition> newDefinitions = findNewRelationshipDefinitions(packageName)

		withTransactionIfSupported(graph) {
	        newDefinitions.each { eld ->
				addDefinition(graph, g, eld)
	        }
		}
    }	


    /** */
    public Collection<RelationshipDefinition> findNewRelationshipDefinitions(String packageName) {
		log.info "CoreGraph findNewRelationshipDefinitions packageName:$packageName"
		assert packageName
    	
		Set<Class<EdgeDefTrait>> defClasses = findEdgeDefClases(packageName)
		if (!defClasses) return new ArrayList<RelationshipDefinition>()
		
		findNewRelationshipDefinitions(defClasses)
	}


	/** */
    public Collection<RelationshipDefinition> findNewRelationshipDefinitions(Set<Class<EdgeDefTrait>> edgeDefClasses) {
		assert edgeDefClasses
    	
		List<RelationshipDefinition> existingDefinitions = graphSchema.getRelationshipDefinitions()
		List<RelationshipDefinition> newDefinitions = new ArrayList<RelationshipDefinition>()

        edgeDefClasses.each { Class edc ->
        	log.trace "findNewRelationshipDefinitions edc: $edc"

            edc.values().each { EdgeDefTrait edef ->
            	log.trace "findNewRelationshipDefinitions edef: $edef"

            	// check if already defined
            	def found = existingDefinitions.find {
            		it.label == edef.label && it.nameSpace == edef.nameSpace
            	}
            	if (!found) {
					def relDef = RelationshipDefinition.create(edef)
					log.trace "found new relationship definition ${relDef.label} ${relDef}"
	            	newDefinitions << relDef
            	}
            }
        }

        return newDefinitions
    }

    
    /** */
    public Set<Class<EdgeDefTrait>> findEdgeDefClases(String packageName) {
    	// find all vertex defs
    	Reflections reflections = new Reflections(packageName)
    	Set<Class<EdgeDefTrait>> classes = reflections.getSubTypesOf(EdgeDefTrait.class)
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
	public List<Vertex> createControlledInstances(Graph graph, GraphTraversalSource g) {
		log.trace "createControlledInstances controlledInstances:${graphSchema.controlledInstances}"

		def verts = []
		
		withTransactionIfSupported(graph) {
			graphSchema.controlledInstances.each { ci ->
				log.trace "creating controlled instance ${ci.class.simpleName} $ci"
				if (ci instanceof ControlledInstance) verts << ci.vertex(graph, g)
				else verts << createControlledInstanceVertex(graph, g, ci)
			}
		}

		return verts
	}


	/** */
	public void createControlledInstanceVertex(Graph graph, GraphTraversalSource g, VertexInstanceDefinition instance) {
		log.trace "controlled instance: $instance"

		def trv = g.V().hasLabel(instance.label)
		instance.properties.each {k, v -> trv.has(k, v)}
		def verts = trv.toList()
		def vert = verts.size() > 0 ? verts[0] : null

		if (vert) {
			log.trace "graph initilization: vertex already exists ${vert} ${vert.label()} ${vert.keys()}"
		}
		else {
			vert = graph.addVertex(T.label, instance.label)
			instance.properties.each {k, v -> vert.property(k, v)}

			log.trace "graph initilization: created vertex ${vert} ${vert.label()} ${vert.keys()}"
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
