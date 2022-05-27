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

import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.VertexDefTrait
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
	static public EdgeConstraint create(EdgeDefTrait edgeDef) {
		assert edgeDef
		EdgeConstraint rd = new EdgeConstraint(
			edgeDef:edgeDef,
			label:edgeDef.label,
			nameSpace:edgeDef.nameSpace,
			domainLabels: edgeDef.domainLabels,
			rangeLabels: edgeDef.rangeLabels//,
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


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
	boolean isGlobal() { 
		nameSpace == null || edgeDef == null || edgeDef.isGlobal()
	}

}


