package carnival.core.graph



import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

import org.slf4j.Logger
import org.slf4j.LoggerFactory


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

import carnival.core.config.Defaults

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
	static public VertexConstraint create(VertexDefinition vdef) {
		def propDefs = []
		vdef.vertexProperties.each { PropertyDefinition pdef ->
			propDefs << new VertexPropertyConstraint(
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
	String label
	String nameSpace
	VertexDefinition vertexDef
	List<VertexPropertyConstraint> properties


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
	boolean isGlobal() { 
		nameSpace == null || vertexDef == null || vertexDef.isGlobal()
	}

	List<String> getUniquePropertyKeys() {
		return properties.findAll({it.unique})*.name
	}

	List<String> getRequiredPropertyKeys() {
		return properties.findAll({it.required})*.name
	}

	List<String> getIndexedPropertyKeys() {
		return properties.findAll({it.index})*.name
	}
}

