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
import carnival.graph.PropertyDefinition
import carnival.graph.VertexDefinition
import carnival.graph.VertexBuilder



/** 
 * VertexConstraint binds a vertex label with a list
 * of VertexPropertyConstraints, the rules for which are
 * scoped by vertex label.
 *
 */
@ToString(includeNames=true)
class VertexConstraint implements ElementConstraint {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Create a vertex constraint from a vertex definition
	 * @param vdef The Vertex definition
	 * @return A vertex constraint
	 */
	static public VertexConstraint create(VertexDefinition vdef) {
		assert vdef

		def propDefs = []
		vdef.vertexProperties.each { PropertyDefinition pdef ->
			propDefs << new VertexPropertyConstraint(
				propertyDef: pdef,
				name: pdef.label,
				unique: pdef.unique,
				required: pdef.required,
				index: pdef.index
			)
		}

		def vld = new VertexConstraint(
			vertexDef: vdef,
			label: vdef.label,
			nameSpace: vdef.nameSpace,
			properties: propDefs
		)

		return vld
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** The vertex label to which this constraint applies */
	String label

	/** The namespace to which this constraint applies */
	String nameSpace

	/**
	 * The vertex definition specifying the parameters of the constraint.
	 */
	VertexDefinition vertexDef

	/** List of allowed vertex properties */
	List<VertexPropertyConstraint> properties


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

	/** Returns true if this constraint should be applied globally */
	boolean isGlobal() { 
		nameSpace == null || vertexDef == null || vertexDef.isGlobal()
	}

	/** The list of properties that must have unique values */
	List<String> getUniquePropertyKeys() {
		return properties.findAll({it.unique})*.name
	}

	/** The list of properties that are required by this constraint */
	List<String> getRequiredPropertyKeys() {
		return properties.findAll({it.required})*.name
	}

	/** The list of properties that should be indexed by the graph database */
	List<String> getIndexedPropertyKeys() {
		return properties.findAll({it.index})*.name
	}
}

