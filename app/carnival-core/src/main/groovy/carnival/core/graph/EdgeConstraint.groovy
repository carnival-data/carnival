package carnival.core.graph



import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.EdgeDefinition
import carnival.graph.EdgeDefinition.Multiplicity
import carnival.graph.PropertyDefinition
import carnival.graph.VertexDefinition
import carnival.graph.VertexBuilder



/** 
 * Defined the valid vertex domain and range labels for a given
 * edge label.
 *
 */
@ToString
class EdgeConstraint implements ElementConstraint {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Create an edge constraint from an edge definition.
	 * @param edgeDef The edge definition
	 * @return The edge constraint
	 */
	static public EdgeConstraint create(EdgeDefinition edgeDef) {
		assert edgeDef

		def propDefs = []
		edgeDef.edgeProperties.each { PropertyDefinition pdef ->
			propDefs << new EdgePropertyConstraint(
				name: pdef.label,
				unique: pdef.unique,
				required: pdef.required,
				index: pdef.index
			)
		}

		EdgeConstraint rd = new EdgeConstraint(
			edgeDef:edgeDef,
			label:edgeDef.label,
			nameSpace:edgeDef.nameSpace,
			domainLabels: edgeDef.domainLabels,
			rangeLabels: edgeDef.rangeLabels,
			multiplicity: edgeDef.multiplicity,
			properties: propDefs
		)
		return rd
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** The edge labels to which this constraint applies */
	String label

	/** The name space to which this constraint applies */
	String nameSpace

	/** The edge definition that defines the constraint parameters */ 
	EdgeDefinition edgeDef

	/** The allowable in-vertex labels; null indicates any allowed */
	List<String> domainLabels

	/** The allowable out-vertex labels; null indicates any allowed */
	List<String> rangeLabels

	/** The multiplicity of the relationthip */
	Multiplicity multiplicity

	/** List of allowed vertex properties */
	List<EdgePropertyConstraint> properties


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

	/** Returns true if this edge constraint should be applied globally */
	boolean isGlobal() { 
		nameSpace == null || edgeDef == null || edgeDef.isGlobal()
	}

}


