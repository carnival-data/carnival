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




