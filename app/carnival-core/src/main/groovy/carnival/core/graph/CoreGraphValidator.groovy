package carnival.core.graph



import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__



/** */
@Slf4j
public class CoreGraphValidator extends GremlinGraphValidator {

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
		List<GraphValidationError> errors = super.checkConstraints(g, graphSchema)

		// check that Identifier.hasCreationFacility and Identifier.hasScope are mutualy exclusive
		verts = g.V()
			.hasLabel("IdentifierClass")
			.has('hasScope', true)
			.has('hasCreationFacility', true)
		.toList()
		if (verts) {
			errors << new GraphValidationError(message:"Found IdentiferClass with both 'hasScope' = true and 'hasCreationFacility' = true: $verts")
		}

		// check that identifiers are unique
		def scopes = g.V().hasLabel("IdentifierScope").toList()
		def facilities = g.V().hasLabel("IdentifierFacility").toList()
		g.V().hasLabel("IdentifierClass").toList().each { idClassVert ->
			// unique by class/scope
			if (idClassVert.properties(Core.PX.HAS_SCOPE.label).size() == 1 && idClassVert.property(Core.PX.HAS_SCOPE.label).value()) {
				scopes.each { scopeVert ->
					def distinctValsCount = g.V(idClassVert)
						.in("is_instance_of")
						.where(
							__.out("is_scoped_by")
							.is(scopeVert)
						).values("value")
						.dedup().count().next()
					def idVertCount = g.V(idClassVert)
						.in("is_instance_of")
						.where(
							__.out("is_scoped_by")
							.is(scopeVert)
						).dedup().count().next()
					if (distinctValsCount != idVertCount) {
							errors << new GraphValidationError(message:"Id values not unique for IdClass: ${idClassVert.property('name')}-$idClassVert, Scope: ${scopeVert.property('name')}  Number vertices: $idVertCount  Number distinct values: $distinctValsCount")
					}
				}
			}
			// unique by class/facility
			else if (idClassVert.properties("hasCreationFacility").size() == 1 && idClassVert.property("hasCreationFacility").value()) {
				facilities.each { fVert ->
					def distinctValsCount = g.V(idClassVert)
						.in("is_instance_of")
						.where(
							__.out("was_created_by")
							.is(fVert)
						)
					.values("value").dedup().count().next()
					def idVertCount = g.V(idClassVert).in("is_instance_of").where(__.out("was_created_by").is(fVert)).count().next()
					if (distinctValsCount != idVertCount) {
							errors << new GraphValidationError(message:"Id values not unique for IdClass: ${idClassVert.property('name')}-$idClassVert, Facility: ${fVert.property('name')}  Number vertices: $idVertCount  Number distinct values: $distinctValsCount")
					}
				}
			}
			// unique by class
			else {
				def distinctValsCount = g.V(idClassVert).in("is_instance_of").values("value").dedup().count().next()
				def idVertCount = g.V(idClassVert).in("is_instance_of").dedup().count().next()
				if (distinctValsCount != idVertCount) {
						errors << new GraphValidationError(message:"Id values not unique for IdClass: ${idClassVert.property('name')}-${idClassVert}  Number vertices: ${idVertCount}  Number distinct values: ${distinctValsCount}")
				}
			}
		}

		//log.trace "$errors"
		log.trace "checkConstraints() total errors: ${errors.size()}"
		return errors
	}

}
