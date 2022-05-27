package carnival.core.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.reflections.Reflections

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
 * The core graph.  See the documentation for model details.
 *
 */
@Slf4j
class CoreGraphSchema implements GraphSchema {

	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** */
	public CoreGraphSchema() { }

	/** */
	public CoreGraphSchema(Collection<VertexBuilder> controlledInstances) {
		assert controlledInstances
		assert controlledInstances.size() > 0
		this.controlledInstances = controlledInstances
	}


	///////////////////////////////////////////////////////////////////////////
	// CONTROLLED INSTANCES
	///////////////////////////////////////////////////////////////////////////

	/** */
	private List<VertexBuilder> controlledInstances = new ArrayList<VertexBuilder>()

	/** */
	public List<VertexBuilder> getVertexBuilders() {
		return controlledInstances
	}
	

	///////////////////////////////////////////////////////////////////////////
	// LABEL DEFINITIONS
	///////////////////////////////////////////////////////////////////////////

	/* all label defs */
	public List<VertexConstraint> getVertexConstraints() {
		def ldefs = new ArrayList<VertexConstraint>()
		ldefs.addAll(staticLabelDefinitions)
		ldefs.addAll(dynamicLabelDefinitions)
		return ldefs
	}

	/** dynamic label defs */
	Set<VertexConstraint> dynamicLabelDefinitions = new HashSet<VertexConstraint>()

	/** static label definitions */
	final List<VertexConstraint> staticLabelDefinitions = []


	///////////////////////////////////////////////////////////////////////////
	// RELATIONSHIP DEFINITIONS
	///////////////////////////////////////////////////////////////////////////

	/* all label defs */
	public List<EdgeConstraint> getEdgeConstraints() {
		def defs = new ArrayList<EdgeConstraint>()
		defs.addAll(staticEdgeConstraints)
		defs.addAll(dynamicEdgeConstraints)
		return defs
	}
	

	/** dynamic label defs */
	Set<EdgeConstraint> dynamicEdgeConstraints = new HashSet<EdgeConstraint>()


	/** */
	List<EdgeConstraint> staticEdgeConstraints = []

}
