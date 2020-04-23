package carnival.core.graph



import groovy.util.AntBuilder
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

		reaperMethodLabelDefinitions(graph, g, packageName)
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
	// DEFINED GRAPH MODEL
	///////////////////////////////////////////////////////////////////////////

	/** */
    public void initializeDefinedVertices(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "CoreGraph initializeDefinedVertices graph:$graph g:$g packageName:$packageName"
		
		assert graph
		assert g
		assert packageName

		List<VertexLabelDefinition> newDefinitions = findNewVertexLabelDefinitions(graph, g, packageName)

		withTransactionIfSupported(graph) {
	        newDefinitions.each { vld ->
	        	log.trace "initializeDefinedVertices vld: ${vld.label} $vld"

	        	log.trace "adding vertex label definition to graph schema ${vld.label} ${vld}"
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
					assert vdef.isClass()
					assert vdef.superClass.isClass()
					assert vdef.vertex
					assert vdef.superClass.vertex
					vdef.setSubclassOf(g, vdef.superClass)
				}
	        }
		}
    }


    /** */
    public Collection<VertexLabelDefinition> findNewVertexLabelDefinitions(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "CoreGraph findNewVertexLabelDefinitions graph:$graph g:$g packageName:$packageName"

		assert graph
		assert g
		assert packageName

    	def vertexDefClasses = findVertexDefClases(packageName)
    	
		List<VertexLabelDefinition> existingDefinitions = graphSchema.getLabelDefinitions()
		List<VertexLabelDefinition> newDefinitions = new ArrayList<VertexLabelDefinition>()

        vertexDefClasses.each { vdc ->
        	log.trace "findNewVertexLabelDefinitions vdc: $vdc"

            vdc.values().each { vdef ->
            	log.trace "findNewVertexLabelDefinitions vdef: $vdef"

            	// check if already defined
            	def found = existingDefinitions.find {
            		it.label == vdef.label && it.nameSpace == vdef.nameSpace
            	}
            	if (!found) {
					def vld = VertexLabelDefinition.create(vdef)
					log.trace "found new vertex label definition ${vld.label} ${vld}"
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


	/** */
    public void initializeDefinedEdges(Graph graph, GraphTraversalSource g, String packageName) {
		assert graph
		assert g
		assert packageName

		def edgeDefClasses = findEdgeDefClases(packageName)
		log.trace "edgeDefClasses: $edgeDefClasses"

		List<RelationshipDefinition> existingDefinitions = graphSchema.getRelationshipDefinitions()
		log.trace "existing relationship definitions: $existingDefinitions"

		withTransactionIfSupported(graph) {
	        edgeDefClasses.each { edc ->
	        	log.trace "initializeDefinedEdges edc: $edc"

	            edc.values().each { edgeDef ->
	            	log.trace "initializeDefinedEdges edgeDef: $edgeDef"

	            	// check if already defined
	            	def found = existingDefinitions.find {
						it.label == edgeDef.label && it.nameSpace == edgeDef.nameSpace
	            	}
	            	if (found) {
	            		throw new RuntimeException("edge definition already exists: ${edgeDef} == ${found}")
	            	}

	            	// add the dynamic label definition
					def relDef = RelationshipDefinition.create(edgeDef)
	            	log.trace "adding RelationshipDefinition ${edgeDef.label} $relDef"
					graphSchema.dynamicRelationshipDefinitions << relDef
					existingDefinitions << relDef
	            }
	        }
		}
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
	// REAPER METHOD GRAPH MODEL
	///////////////////////////////////////////////////////////////////////////


	/** */
    public List<VertexLabelDefinition> reaperMethodLabelDefinitions(Graph graph, GraphTraversalSource g, String packageName) {
    	def rmcs = findReaperMethodClasses(packageName)
    	log.trace "rmcs: $rmcs"

    	reaperMethodLabelDefinitions(graph, g, rmcs)
    }


	/** */
    public List<VertexLabelDefinition> reaperMethodLabelDefinitions(Graph graph, GraphTraversalSource g, Set<Class<ReaperMethod>> rmcs) {

    	List<VertexLabelDefinition> labelDefs = new ArrayList<VertexLabelDefinition>()
    	def existingDefinitions = graphSchema.labelDefinitions

    	withTransactionIfSupported(graph) {
			rmcs.each { rmc ->

	        	// try to create a reaper method instance
	        	def rm
	        	try {
	        		rm = rmc.newInstance()
	        		log.trace "rm: $rm"
	    		} catch (Throwable t) {
	    			log.trace "rmc:${rmc.simpleName} t:${t.message}"
	    		}
	    		if (!rm) return

	    		// try to get hard coded defs
	    		def tpcd
	    		def tpd
	        	try {
	        		tpcd = rm.getTrackedProcessClassDef()
	        		tpd = rm.getTrackedProcessDef()
	    		} catch (Throwable t) {
	    			log.trace "rmc:${rmc.simpleName} t:${t.message}"
	    		}

	    		// they both need to be defined or none of them
	    		if ((tpcd == null && tpd != null) || (tpcd != null && tpd == null)) throw new RuntimeException("both or none must be defined, tracked process def and tracked process class def, tpcd:$tpcd tpd:$tpd")
	    		
	    		// if neither is defined, create new defs
	    		// DynamicVertexDef.singletonFromCamelCase will create the singleton vertex
	    		// it is assumed that if tpcd and tpd are defined, then they will have been
	    		// created by the initializeDefinedVertices machinery
	    		if (!(tpcd && tpd)) {
	        		def tpcn = rm.getTrackedProcessClassName()
	        		log.trace "tpcn:$tpcn"
	        		tpcd = DynamicVertexDef.singletonFromCamelCase(graph, g, tpcn)

	        		def tpn = rm.getTrackedProcessName()
	        		log.trace "tpn:$tpn"
	        		tpd = DynamicVertexDef.singletonFromCamelCase(graph, g, tpn)
	    		}

	    		// HACKY: add new vertex label definition iff it looks like one was created,
	    		// ie, it's a DynamicVertexDef
	    		log.trace "tpcd:$tpcd"
	    		log.trace "tpd:$tpd"
	    		[tpcd, tpd].each { vdef ->
	    			if (!(vdef instanceof DynamicVertexDef)) return

	            	def found = existingDefinitions.find {
	            		it.label == vdef.label && it.nameSpace == vdef.nameSpace
	            	}
	            	if (!found) {
		    			def vld = VertexLabelDefinition.create(vdef)
		                //def vld = new VertexLabelDefinition(label:vdef.label)
		                log.trace "adding VertexLabelDefinition ${vdef.label} $vld"
		                labelDefs << vld
	            	}
	    		}

	        }    		
    	}
        
        graphSchema.dynamicLabelDefinitions.addAll(labelDefs)

        return labelDefs
    }	


    /** */
    public Set<Class<ReaperMethod>> findReaperMethodClasses(String packageName) {
    	// find all vertex defs
    	Reflections reflections = new Reflections(packageName)
    	Set<Class<ReaperMethod>> targetClasses = reflections.getSubTypesOf(ReaperMethod.class)
    	log.trace "findReaperMethodClasses packageName:$packageName targetClasses:$targetClasses"

    	return targetClasses
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
