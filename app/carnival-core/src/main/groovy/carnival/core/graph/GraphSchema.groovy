package carnival.core.graph



import groovy.util.AntBuilder
import groovy.transform.ToString

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

import carnival.util.Defaults

import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.VertexDefTrait
import carnival.graph.ControlledInstance
import carnival.graph.ConstrainedPropertyDefTrait




/** */
interface GraphSchema {

	/** 
	 * Controlled instances are singleton vertices that are governed by rules
	 * of cardinality and required properties.  There can be only a single
	 * instance of a vertex with the specific values for the refined required 
	 * properties.  For example, if there is a controlled instance with label
	 * IdentifierClass and required boolean property hasScope, there can be
	 * only a single vertex (IdentifierClass {hasScope:true}) and a single
	 * vertex (IdentifierClass {hasScope:false}).
	 *
	 *
	 */
	Collection<ControlledInstance> getControlledInstances()


	/** */
	Collection<VertexLabelDefinition> getLabelDefinitions()


	/** */
	Collection<RelationshipDefinition> getRelationshipDefinitions()

}


/** */
interface ElementDef {
	String getLabel()
	String getNameSpace()
	boolean isGlobal()
}


/** */
@ToString
class DefaultElementDef implements ElementDef {
	String label
	String nameSpace
	boolean global
	boolean isGlobal() { return global }
}


/**
 * Defines the rules governing the given property.
 *   - Unique asserts that the property values must be unique
 *   across all vertices with a given label that have that
 *   property.
 *   - Required asserts that the property must be present
 *   and non-null(?)
 *   - Index instructs Carnival to use the underlying graph
 *   database services to index the given property for 
 *   efficient lookup.
 * 
 */
@ToString
class VertexPropertyDefinition {
	String name
	Boolean unique = false
	Boolean required = false
	Boolean index = false
}



/** 
 * VertexLabelDefinition binds a vertex label with a list
 * of VertexPropertyDefinitions, the rules for which are
 * scoped by vertex label.
 *
 */
@ToString(includeNames=true)
class VertexLabelDefinition implements ElementDef {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////
	static public VertexLabelDefinition create(VertexDefTrait vdef) {
		def propDefs = []
		vdef.vertexProperties.each { PropertyDefTrait pdef ->
			if (pdef instanceof ConstrainedPropertyDefTrait) {
				propDefs << new VertexPropertyDefinition(
					name: pdef.label,
					unique: pdef.unique,
					required: pdef.required,
					index: pdef.index
				)
			} else {
				propDefs << new VertexPropertyDefinition(name: pdef.label)
			}
		}

		def vld = new VertexLabelDefinition(
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
	VertexDefTrait vertexDef
	List<VertexPropertyDefinition> properties


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



/** 
 * Defined the valid vertex domain and range labels for a given
 * edge label.
 *
 */
@ToString
class RelationshipDefinition implements ElementDef {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////
	static public RelationshipDefinition create(EdgeDefTrait edgeDef) {
		assert edgeDef
		RelationshipDefinition rd = new RelationshipDefinition(
			edgeDef:edgeDef,
			label:edgeDef.label,
			nameSpace:edgeDef.nameSpace,
			domainLabels: edgeDef.domainLabels,
			rangeLabels: edgeDef.rangeLabels,
			constraint: edgeDef.constraint
		)
		return rd
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////
	String label
	String nameSpace
	EdgeDefTrait edgeDef
	List<String> domainLabels // null indicates any allowed
	List<String> rangeLabels // null indicates any allowed
	String constraint // additional constraints, just represent as a string for now


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
	boolean isGlobal() { 
		nameSpace == null || edgeDef == null || edgeDef.isGlobal()
	}

}





/**
 * Defined a single vertex instance.
 * For example, the definition of a vertex that represents the
 * class of EMPI identifiers may be defined:
 *   VertexInstanceDefinition {
 *     label: 'IdentifierClass'
 *     properties:[name:'empi']
 *   }
 * 
 */
@ToString
class VertexInstanceDefinition {

	/** */
	String label

	/** */
	Map<String,Object> properties

	/** */
	public VertexInstanceDefinition(Map m) {
		assert m.label != null
		this.label = m.label
		if (m.properties) this.properties = m.properties
	}

	/** */
	public VertexInstanceDefinition(String label, Map<String,Object> properties = [:]) {
		assert label != null
		this.label = label
		this.properties = properties
	}

	/** */
	public VertexInstanceDefinition(VertexDefTrait labelDef, Map<String,Object> properties = [:]) {
		assert labelDef != null
		this.label = labelDef.label
		this.properties = properties
	}

}




