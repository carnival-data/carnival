package carnival.core.graph



import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.PropertyDefinition
import carnival.graph.Base



/** */
@Slf4j
public class DefaultGraphValidator implements GraphValidator {

	///////////////////////////////////////////////////////////////////////////
	// STATIC METOHDS
	///////////////////////////////////////////////////////////////////////////

	/** */
	static boolean sameAs(String a, String b) {
		if (a == null && b == null) return true
		if (a == null || b == null) return false
		a.equals(b)
	}


	///////////////////////////////////////////////////////////////////////////
	// MODEL CHECKING
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Check property existence constraints
	 * Check relationship constraints
	 * Check that singleton vertices exist only once
	 * Check that the combinaiton of identifier.value/identifierClass/IdentifierScope is unique
	 */
	public List<GraphValidationError> checkConstraints(GraphTraversalSource g, GraphSchema graphSchema) {
		log.trace "checkConstraints()"
		
		assert g
		assert graphSchema

		List<Vertex> verts
		List<Edge> edges
		List<GraphValidationError> errors = new ArrayList<GraphValidationError>()

		// check all property existence constraints
		def filteredVertexConstraints = filteredModels(graphSchema.vertexConstraints.toSet())
		filteredVertexConstraints.each { labelDef ->
			labelDef.requiredPropertyKeys.each { property ->
				def traversal = g.V()
					.hasLabel(labelDef.label)
					.hasNot(property)
				if (!labelDef.isGlobal()) traversal.has(Base.PX.NAME_SPACE.label, labelDef.nameSpace)

				verts = traversal.toList()
				if (verts) {
					errors << new GraphValidationError(message:"Property existence constraint ${labelDef.label}.$property violated: $verts.")
				}
    		}
    	}

		// check that singleton vertices exist only once
		graphSchema.vertexBuilders.each { instance ->

	        def lbl = instance.vertexDef.label
	        //log.debug "GraphValidator lbl: $lbl"

	        def traversal = g.V()
	        	.hasLabel(lbl)
	        	.has(Base.PX.NAME_SPACE.label, instance.vertexDef.nameSpace)

			//log.debug "GraphValidator.checkConstraints instance.propertyValues: ${instance.propertyValues}"

	        instance.propertyValues.each { PropertyDefinition vp, Object val -> 
	        	//log.debug "GraphValidator.checkConstraints instance.propertyValues.each ${vp.label} $val"
	        	traversal.has(vp.label, val) 
	        }

	        verts = traversal.toList()

			//def traverse = g.V().hasLabel(instance.label)
			//instance.properties.each {k, v -> traverse.has(k, v)}
			//verts = traverse.toList()

			//log.trace "verts: $verts"
			//log.trace "size: ${verts.size()}"

			if (verts.size() == 0) {
				errors << new GraphValidationError(message:"Controlled instance $instance does not exist.")
			}
			if (verts.size() > 1) {
				errors << new GraphValidationError(message:"Multiple controlled instances $instance exist: $verts")
			}

		}

		// check relationship domain and range constraints
		//def relationshipDefsByLabel = modelsByLabel(graphSchema.edgeConstraints)
		def filteredRelationshipModels = filteredModels(graphSchema.edgeConstraints.toSet())
		filteredRelationshipModels.each { edgeDef ->
			edges = []
			def ns = edgeDef.nameSpace

			// domain check
			if (edgeDef.domainLabels) {
				def traversal = g.V()
					.has(T.label, P.without(edgeDef.domainLabels))
					.outE(edgeDef.label)
				if (!edgeDef.isGlobal()) traversal.has(Base.PX.NAME_SPACE.label, ns)
				edges = traversal.toList()
				if (edges.size() > 0) {
					def domainLabels = g.E(edges).outV().toList().collect({"${it.id()}:${it.label()}"}).unique()
					errors << new GraphValidationError(message:"Edge domain constraint $edgeDef violated by: $edges $domainLabels")
				}
			}

			// range check
			if (edgeDef.rangeLabels) {
				def traversal = g.V()
					.has(T.label, P.without(edgeDef.rangeLabels))
					.inE(edgeDef.label)
				if (!edgeDef.isGlobal()) traversal.has(Base.PX.NAME_SPACE.label, ns)
				edges = traversal.toList()
				if (edges.size() > 0) {
					//  ${edges*.label()}
					def rangeLabels = g.E(edges).inV().toList().collect({"${it.id()}:${it.label()}"}).unique()
					errors << new GraphValidationError(message:"Edge range constraint $edgeDef violated by: ${edges} ${rangeLabels}")
				}
			}
		}

		//log.trace "$errors"
		log.trace "checkConstraints() total errors: ${errors.size()}"
		return errors
	}


	/**
	 * Check to see if there are any vertex or edge labels that are not defined in the model
	 *
	 */
	public Collection<String> checkModel(GraphTraversalSource g, GraphSchema graphSchema) {
		log.trace "checkModel()"

		assert g
		assert graphSchema

		def warnings = []

		// global defs are consistent
		assertGlobalDefsAreUnique(g, graphSchema)

		// vertices, edges, and properties
		warnings.addAll(checkModelVertices(g, graphSchema))
		warnings.addAll(checkModelEdges(g, graphSchema))
		warnings.addAll(checkModelProperties(g, graphSchema))

		log.trace "checkModel() total warnings: ${warnings.size()}"
		return warnings
	}


	/** */
	public void assertGlobalDefsAreUnique(GraphTraversalSource g, GraphSchema graphSchema) {
		// each global def for a given label should be unique!!!
	}


	/** */
	public Collection<String> checkModelEdges(GraphTraversalSource g, GraphSchema graphSchema) {
		assert g
		assert graphSchema

		def warnings = []
		
		def unmodeledRelTypes = unmodeledElements(
			g.E(), 
			graphSchema.edgeConstraints
		)

		if (unmodeledRelTypes) warnings << "Unmodeled relationshipTypes: $unmodeledRelTypes"
		return warnings
	}



	/** */
	public Collection<String> checkModelProperties(GraphTraversalSource g, GraphSchema graphSchema) {
		assert g
		assert graphSchema

		def warnings = []

		/*
		// Apparently the keys returned by "CALL db.propertyKeys()" consist of any key that was ever created...
		// if a vertex with a new key is created but then that vertex is deleted or the transaction is rolled back,
		// that key is still returned.  This causes tests to fail, so commenting out for now.

		def modeledPropertyKeys = []
		vertexConstraints.each {modeledPropertyKeys.addAll(it.properties*.name ?: [])}
		def dbPropertyKeys = graph.cypher("CALL db.propertyKeys()").toList()*.propertyKey
		def unmodeledPropertyKeys = dbPropertyKeys - modeledPropertyKeys
		unmodeledPropertyKeys = unmodeledPropertyKeys - ["~gremlin.neo4j.multiProperties", "~gremlin.neo4j.metaProperties"] // default properties
		if (unmodeledPropertyKeys) warnings << "Unmodeled property labels: $unmodeledPropertyKeys"
		*/

		return warnings
	}



	/** */
	public Collection<String> checkModelVertices(GraphTraversalSource g, GraphSchema graphSchema) {
		assert g
		assert graphSchema

		def warnings = []

		def unmodeledVertices = unmodeledElements(
			g.V(), 
			graphSchema.vertexConstraints
		)

		if (unmodeledVertices) warnings << "Unmodeled vertex labels: $unmodeledVertices"
		return warnings
	}


	///////////////////////////////////////////////////////////////////////////
	// UTILITY
	///////////////////////////////////////////////////////////////////////////

	/** */
	Set<String> unmodeledElements(Traversal traversal, Collection<ElementConstraint> allModels) {

		def modelsByLabel = modelsByLabel(allModels)
		log.debug "modelsByLabel: $modelsByLabel"

		Set<DefaultElementConstraint> dbElementConstraints = new HashSet<DefaultElementConstraint>()
		traversal.each { e ->
			def lbl = e.label()
			def ns = e.property(Base.PX.NAME_SPACE.label).orElse(Base.GLOBAL_NAME_SPACE)
			dbElementConstraints << new DefaultElementConstraint(label:lbl, nameSpace:ns)
		}

		//Set<DefaultElementConstraint> dbElementConstraints = new HashSet<DefaultElementConstraint>()
		//dbElementConstraints.addAll(dbElementConstraintMap.values())
		log.trace "dbElementConstraints: ${dbElementConstraints?.size()} ${dbElementConstraints?.take(100)}"

		Set<String> unmodeledElements = new HashSet<String>()
		dbElementConstraints.each { dbr ->
			if (!modelsByLabel.containsKey(dbr.label)) {
				unmodeledElements << "${dbr.nameSpace}.${dbr.label}"
				return
			}

			def models = modelsByLabel.get(dbr.label)
			if (models.find({ 
				sameAs(it.label, dbr.label) && (it.isGlobal() || sameAs(it.nameSpace, dbr.nameSpace)) 
			})) return

			unmodeledElements << "${dbr.nameSpace}.${dbr.label}"
		}

		return unmodeledElements		
	}

	/** */
	Map<String,Set<ElementConstraint>> modelsByLabel(Collection<ElementConstraint> allElementConstraints) {
		Map<String,List<ElementConstraint>> mds = new HashMap<String,List<ElementConstraint>>()
		allElementConstraints.each { ElementConstraint edef ->
			if (!mds.containsKey(edef.label)) {
				mds.put(edef.label, [edef])
				return
			}
			mds.get(edef.label) << edef
		}
		//mds.each { log.debug "${it}" }
		return mds
	}


	/** */
	Set<ElementConstraint> filteredModels(Set<ElementConstraint> allModels) {
		def modelsByLabel = modelsByLabel(allModels)
		filteredModels(modelsByLabel)
	}


	/** */
	Set<ElementConstraint> filteredModels(Map<String,Set<ElementConstraint>> modelsByLabel) {
		List<ElementConstraint> filteredModels = new ArrayList<ElementConstraint>()
		modelsByLabel.each { lbl, edefs ->
			if (edefs.size() == 1) {
				filteredModels << edefs.first()
				return
			}
			def globalDef = edefs.find { it.isGlobal() }
			if (globalDef) filteredModels << globalDef
			else filteredModels.addAll(edefs)
		}
		//filteredModels.each { log.debug "${it}" }
		return filteredModels
	}


}
