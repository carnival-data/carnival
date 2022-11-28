package carnival.core.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j

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
 * The core graph.  See the documentation for model details.
 *
 */
@Slf4j
class DefaultGraphSchema implements GraphSchema {

	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** */
	public DefaultGraphSchema() { }

	/** */
	public DefaultGraphSchema(Collection<VertexBuilder> vertexBuilders) {
		assert vertexBuilders
		assert vertexBuilders.size() > 0
		this.vertexBuilders.addAll(vertexBuilders)
	}


	///////////////////////////////////////////////////////////////////////////
	// SINGLETON VERTEX BUILDERS
	///////////////////////////////////////////////////////////////////////////

	/** */
	private Set<VertexBuilder> vertexBuilders = new HashSet<VertexBuilder>()

	/** */
	public Set<VertexBuilder> getVertexBuilders() {
		return vertexBuilders
	}
	

	///////////////////////////////////////////////////////////////////////////
	// VERTEX CONSTRAINTS
	///////////////////////////////////////////////////////////////////////////

	/** vertex constraints */
	private Set<VertexConstraint> vertexConstraints = new HashSet<VertexConstraint>()

	/* get vertex constraints */
	public Set<VertexConstraint> getVertexConstraints() {
		return vertexConstraints
	}

	/** */
	boolean containsConstraint(VertexDefinition vDef) {
		vertexConstraints.find {
			it.label == vDef.label && it.nameSpace == vDef.nameSpace
		}
	}


	///////////////////////////////////////////////////////////////////////////
	// EDGE CONSTRAINTS
	///////////////////////////////////////////////////////////////////////////

	/** edge constraints */
	private Set<EdgeConstraint> edgeConstraints = new HashSet<EdgeConstraint>()

	/* get edge constraints */
	public Set<EdgeConstraint> getEdgeConstraints() {
		return edgeConstraints
	}


}