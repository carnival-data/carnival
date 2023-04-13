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

import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.VertexDefinition
import carnival.graph.VertexBuilder




/** 
 * Interface that defines a graph schema object.
 */
interface GraphSchema {

	/** 
	 * Controlled instances are singleton vertices that are governed by rules
	 * of cardinality and required properties.  There can be only a single
	 * instance of a vertex with the specific values for the defined required 
	 * properties.  For example, if there is a controlled instance with label
	 * IdentifierClass and required boolean property hasScope, there can be
	 * only a single vertex (IdentifierClass {hasScope:true}) and a single
	 * vertex (IdentifierClass {hasScope:false}).
	 *
	 *
	 */
	Collection<VertexBuilder> getVertexBuilders()

	/** 
	 * Return the constraints on vertices.
	 * @return The collection of vertex constraints
	 */
	Collection<VertexConstraint> getVertexConstraints()

	/** 
	 * Return true if the graph schema contains the provided vertex definition.
	 * @param vDef The vertex definition
	 * @return The boolean value
	 */
	boolean containsConstraint(VertexDefinition vDef)

	/** 
	 * Return the constraints on edges.
	 * @return The collection of edge constraints
	 */
	Collection<EdgeConstraint> getEdgeConstraints()

}



