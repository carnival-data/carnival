package carnival.core



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.reflections.Reflections

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

import carnival.graph.Base
import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.ElementDefinition
import carnival.graph.VertexDefinition
import carnival.graph.DynamicVertexDef
import carnival.graph.VertexBuilder
import carnival.core.graph.GremlinTrait
import carnival.core.graph.GremlinTraitUtilities
import carnival.core.graph.GraphSchema
import carnival.core.graph.DefaultGraphSchema
import carnival.core.graph.GraphValidator
import carnival.core.graph.GraphValidationError
import carnival.core.graph.DefaultGraphValidator
import carnival.core.graph.EdgeConstraint
import carnival.core.graph.VertexConstraint
import carnival.core.util.DuplicateModelException





/**
 * The Carnival object.
 *
 * A Carnival is comprosed of three main components: a Gremlin graph, a graph 
 * schema, and a graph validator.
 *
 */
@Slf4j
abstract class Carnival implements GremlinTrait {

	///////////////////////////////////////////////////////////////////////////
	// INSTANCE
	///////////////////////////////////////////////////////////////////////////

	/** A gremlin Graph object is added by GremlinTrait. */
	//Graph graph

	/** The graph schema of this Carnival */
	GraphSchema graphSchema

	/** The graph validator of this Carnival */
	GraphValidator graphValidator

	/** 
	 * If true, if duplicate models are added, ignore the duplicates.  If
	 * false, if duplicate models are added, throw an exception
	 */
	boolean ignoreDuplicateModels = false



	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Create a Carnival from a gremlin graph, a graph schema, and a graph 
	 * validator.
	 * @param graph A gremlin graph.
	 * @param graphSchema A graph schema
	 * @param graphValidator A graph validator
	 */
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
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Initialize a gremlin graph with the core Carnival graph model.
	 * @param graph The gremlin graph to initialize
	 * @param g A graph traversal source to use during initialization.
	 */
	public void initialize(Graph graph, GraphTraversalSource g) {
		log.info "Carnival initialize graph:$graph g:$g"
		[Base.EX, Core.EX, Core.VX].each {
			addModel(graph, g, it)
		}
	} 



	///////////////////////////////////////////////////////////////////////////
	// GRAPH MODEL - PACKAGES
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Add graph models from a package of the given name via package 
	 * introspection.
	 * @param graph The gremlin graph to add models.
	 * @param g A graph traversal source to use.
	 * @packageName The name of the package in which to search for models.
	 */
	public void addModelsFromPackage(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "Carnival addModelsFromPackage graph:$graph g:$g packageName:$packageName"
		addVertexModelsFromPackage(graph, g, packageName)
		addEdgeModelsFromPackage(graph, g, packageName)
	}


	/** 
	 * Add vertex models from a package of the given name.
	 * @param graph The gremlin graph to add models.
	 * @param g A graph traversal source to use.
	 * @packageName The name of the package in which to search for models.
	 */
    public void addVertexModelsFromPackage(Graph graph, GraphTraversalSource g, String packageName) {
		log.info "Carnival addVertexModelsFromPackage graph:$graph g:$g packageName:$packageName"
		
		assert graph
		assert g
		assert packageName

		Set<VertexConstraint> vertexConstraints = findNewVertexConstraints(packageName)
		vertexConstraints.each { vc ->
			addConstraint(vc)
		}

		GremlinTraitUtilities.withGremlin(graph, g) {
			addClassVertices(graph, g, vertexConstraints)
		}
    }


    /** 
	 * Find vertex constraints in the package with the given name that are not
	 * already part of the graph schema.
	 * @param packageName The name of the package to search.
	 * @return A collection of zero or more vertex constraints.
	 */
    public Collection<VertexConstraint> findNewVertexConstraints(String packageName) {
		log.info "Carnival findNewVertexConstraints packageName:$packageName"
		assert packageName

    	Set<Class<VertexDefinition>> vertexDefClasses = findVertexDefClases(packageName)
		if (!vertexDefClasses) return new HashSet<VertexConstraint>()

		findNewVertexConstraints(vertexDefClasses)
	}


    /** 
	 * Find all vertex definition classes in the package with the given name.
	 * @param packageName The name of the package to search.
	 * @return A set of vertex definition classes.
	 */
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


	/** 
	 * Add edge models to the provided gremlin graph from the package named per
	 * the provided name.
	 * @param graph A gremlin graph.
	 * @param g A graph traversal source to use.
	 * @param packageName The name of the package in which to search.
	 */
    public void addEdgeModelsFromPackage(Graph graph, GraphTraversalSource g, String packageName) {
		assert graph
		assert g
		assert packageName

		Set<EdgeConstraint> edgeConstraints = findNewEdgeConstraints(packageName)
		edgeConstraints.each { ec ->
			addConstraint(ec)
		}
    }	


    /** 
	 * Find edge constraints in the package named per the provided name that do
	 * not already exist in the graph schema.
	 * @param packageName The name of the package in which to search.
	 * @return A collection of edge constraints.
	 */
    public Collection<EdgeConstraint> findNewEdgeConstraints(String packageName) {
		log.info "Carnival findNewEdgeConstraints packageName:$packageName"
		assert packageName
    	
		Set<Class<EdgeDefinition>> defClasses = findEdgeDefClases(packageName)
		if (!defClasses) return new HashSet<EdgeConstraint>()
		
		findNewEdgeConstraints(defClasses)
	}


	/** 
	 * Find edge definition classes in the package named per the provided name.
	 * @param packageName The name of the package in which to search.
	 * @return A set of edge definition classes.
	 */
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
	// GRAPH MODEL - CLASSES
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Add the model defined in the given class to this Carnival.
	 * @param defClass The element definition class.
	 */
	public void addModel(Class<ElementDefinition> defClass) {
		assert defClass
		withGremlin { graph, g ->
			addModel(graph, g, defClass)
		}
	}

	/**
	 * Add a model defined in the given class to this Carnival using the
	 * provided graph and graph traversal source.  This is an internal method;
	 * it is expected that client code will use addModel(Class) to add models.
	 * @see #addModel(Class<ElementDefinition>)
	 * @param graph A gremlin graph.
	 * @param g A grpah traversal source to use.
	 * @param defClass The element definition class.
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


	/**
	 * Add a vertex model defined in the given vertex definition class to this
	 * Carnival using the provided graph and graph traversal source.  This is
	 * an internal method; it is expected that client code will use
	 * addModel(Class) to add models.
	 * @see #addModel(Class<ElementDefinition>)
	 * @param defClass The vertex definition class.
	 * @param graph A gremlin graph.
	 * @param g A graph traversal source to use.
	 */
	public void addVertexModel(Graph graph, GraphTraversalSource g, Class<VertexDefinition> defClass) {
		assert graph
		assert g
		assert defClass

		Set<VertexConstraint> vertexConstraints = findNewVertexConstraints(defClass)
		vertexConstraints.each { vc ->
			addConstraint(vc)
		}
		GremlinTraitUtilities.withGremlin(graph, g) {
			addClassVertices(graph, g, vertexConstraints)
		}
	}


	/**
	 * Add the edge models in the provided edge definition class to this
	 * Carnival using the provided graph and graph traversal source. This is an
	 * internal method and not expected to be called by client code.
	 * @param graph A gremlin graph.
	 * @param g A graph traversal source.
	 * @param defClass An edge definition class.
	 */
	public void addEdgeModel(Graph graph, GraphTraversalSource g, Class<EdgeDefinition> defClass) {
		assert graph
		assert g
		assert defClass

		Set<EdgeConstraint> edgeConstraints = findNewEdgeConstraints(defClass)
		edgeConstraints.each { ec ->
			addConstraint(ec)
		}
	}


	///////////////////////////////////////////////////////////////////////////
	// GRAPH CONSTRAINTS - VERTEX
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Add the provided vertex constraint to this Carnival.
	 * @param vertexConstraint The Vertex constraint to add.
	 */
	public void addConstraint(VertexConstraint vertexConstraint) {
		log.trace "addConstraint vertexConstraint: ${vertexConstraint.label} $vertexConstraint"
		graphSchema.vertexConstraints << vertexConstraint
	}


    /** 
	 * Find vertex constraints in the given set of vertex definition classes
	 * that do not already exist in this Carnival.
	 * @param vertexDefClasses A set of vertex definition classes.
	 * @return A set of vertex constraints.
	 */
    public Collection<VertexConstraint> findNewVertexConstraints(Set<Class<VertexDefinition>> vertexDefClasses) {
		Set<VertexConstraint> allNewConstraints = new HashSet<VertexConstraint>()
		vertexDefClasses.each { vdc ->
			Set<VertexConstraint> newConstraints = findNewVertexConstraints(vdc)
			allNewConstraints.addAll(newConstraints)
		}
		allNewConstraints
	}


    /** 
	 * Find vertex constraints in the provided vertex definition class that do
	 * not already exist in this Carnival.
	 * @param vertexDefClass The vertex definition class.
	 * @return A collection of vertex constraints.
	 */
    public Collection<VertexConstraint> findNewVertexConstraints(Class<VertexDefinition> vertexDefClass) {
		assert vertexDefClass
    	
		Set<VertexConstraint> existingConstraints = graphSchema.getVertexConstraints()
		Set<VertexConstraint> newConstraints = new HashSet<VertexConstraint>()

		toVertexConstraints(vertexDefClass).each { vc ->
			log.trace "findNewVertexConstraints vc: $vc"
			def exists = existsInGraphSchema(vc)
			if (!exists) {
				log.trace "new vertex constraint: ${vc}"
				newConstraints.add(vc)
			}
			if (exists && !ignoreDuplicateModels) throw new DuplicateModelException(
				"vertex constraint already exists: ${vc}"
			)
		}

        return newConstraints
    }


	/** 
	 * Return true if the provided vertex constraint exists in the graph 
	 * schema.
	 * @param vc The vertex constraint
	 * @return True if the vertex constraint exists
	 */
	boolean existsInGraphSchema(VertexConstraint vc) {
		graphSchema.vertexConstraints.find {
			it.label == vc.label && it.nameSpace == vc.nameSpace
		}
	}


    /** 
	 * Scan the vertex definition class for vertex constraint definitions, 
	 * return them in a collection.
	 * @param vertexDefClass The vertex definition class to scan.
	 * @return A collection of vertex constraints.
	 */
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

	/**
	 * Add the provided edge constraint to this carnival.
	 * @param edgeConst The edge constraint to add.
	 */
	public void addConstraint(EdgeConstraint edgeConst) {
		log.trace "addConstraint edgeConst: ${edgeConst.label} $edgeConst"

		log.trace "adding edge definition to graph schema ${edgeConst.label} ${edgeConst}"
		graphSchema.edgeConstraints.add(edgeConst)
	}


	/**
	 * Find edge constraints in an edge definition class that are not already
	 * present in this Carnival.
	 * @param edgeDefClass The edge definition class.
	 * @return A collection of edge constraints.
	 */
	public Collection<EdgeConstraint> findNewEdgeConstraints(Class<EdgeDefinition> edgeDefClass) {
		assert edgeDefClass
		Set<Class<EdgeDefinition>> edcs = new HashSet<Class<EdgeDefinition>>()
		edcs.add(edgeDefClass)
		findNewEdgeConstraints(edcs)
	}


	/** 
	 * Find edge constraints in a provided set of edge definition classes that
	 * are not already present in this Carnival.
	 * @param edgeDefClasses A set of edge definition classes.
	 * @return A collection of edge constraints.
	 */
    public Collection<EdgeConstraint> findNewEdgeConstraints(Set<Class<EdgeDefinition>> edgeDefClasses) {
		assert edgeDefClasses
    	
		Set<EdgeConstraint> newConstraints = new HashSet<EdgeConstraint>()

        edgeDefClasses.each { Class edc ->
        	log.trace "findNewEdgeConstraints edc: $edc"

            edc.values().each { EdgeDefinition edef ->
            	log.trace "findNewEdgeConstraints edef: $edef"

				EdgeConstraint ec = EdgeConstraint.create(edef)
				def exists = existsInGraphSchema(ec)
				if (!exists) {
					log.trace "new edge constraint ${ec.label} ${ec}"
					newConstraints.add(ec)
				}
				if (exists && !ignoreDuplicateModels) throw new DuplicateModelException(
					"edge constraint already exists: ${ec}"
				)
            }
        }

        return newConstraints
    }

    
	/** 
	 * Return true if the provided edge constraint exists in the graph 
	 * schema.
	 * @param vc The edge constraint
	 * @return True if the edge constraint exists
	 */
	boolean existsInGraphSchema(EdgeConstraint ec) {
		graphSchema.edgeConstraints.find {
			it.label == ec.label && it.nameSpace == ec.nameSpace
		}
	}
    



	///////////////////////////////////////////////////////////////////////////
	// VERTEX INSTANCES
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * If the graph schema contains vertex builders that should be used to
	 * create initial vertices in a graph, create the vertices from them.
	 * @param graph The graph to add vertices to.
	 * @param g A graph traversal to use.
	 * @return A list of added vertices.
	 */
	public List<Vertex> addGraphSchemaVertices(Graph graph, GraphTraversalSource g) {
		log.trace "addGraphSchemaVertices vertexBuilders:${graphSchema.vertexBuilders}"

		List<Vertex> verts = new ArrayList<Vertex>()
		
		GremlinTraitUtilities.withGremlin(graph, g) {
			graphSchema.vertexBuilders.each { ci ->
				log.trace "creating controlled instance ${ci.class.simpleName} $ci"
				assert ci instanceof VertexBuilder
				verts.add(ci.vertex(graph, g))
			}
		}

		return verts
	}


	/** 
	 * Look in the set of vertex constraints for those that define classes;
	 * add a verte in the graph to represent each of them.
	 * @param vcs A set of vertex constraints.
	 * @param graph A gremlin graph to which to add vertices.
	 * @param g A graph traversal to use.
	 */
	public void addClassVertices(Graph graph, GraphTraversalSource g, Set<VertexConstraint> vcs) {
		GremlinTraitUtilities.withGremlin(graph, g) {
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
	 * @param vertexConstraint The vertex constraint representint a class.
	 * @param graph The gremlin graph to use.
	 * @param g The graph traversal to use.
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


	/** 
	 * Connect the class vertex which represents the provided vertex constraint
	 * to a superclass vertex if it exists.
	 * @param vertexConstraint The vertex constraint.
	 * @param graph A gremlin graph to use.
	 * @param g A graph traversal source to use.
	 */
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

	/** 
	 * Check that the gremlin graph of this carnival obeys all graph
	 * graph constraints.
	 * @return A list of graph validation errors.
	 */
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


	/** 
	 * Check that the vertices and edges in the gremlin graph obey all
	 * specified models.
	 * @return A collection of model violation descriptions.
	 */
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


	/** 
	 * Return a graph traversal source that can be used with this carnival.
	 * @return A grpah traversal souece.
	 */
	public GraphTraversalSource traversal() {
		return graph.traversal()
	}


}
