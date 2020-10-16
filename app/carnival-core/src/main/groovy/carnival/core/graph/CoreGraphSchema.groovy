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

import carnival.util.Defaults
import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.VertexDefTrait
import carnival.graph.ControlledInstance




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
	public CoreGraphSchema(Collection<ControlledInstance> controlledInstances) {
		assert controlledInstances
		assert controlledInstances.size() > 0
		this.controlledInstances = controlledInstances
	}


	///////////////////////////////////////////////////////////////////////////
	// CONTROLLED INSTANCES
	///////////////////////////////////////////////////////////////////////////

	/** */
	private List<ControlledInstance> controlledInstances = new ArrayList<ControlledInstance>()

	/** */
	public List<ControlledInstance> getControlledInstances() {
		return controlledInstances
	}
	

	///////////////////////////////////////////////////////////////////////////
	// LABEL DEFINITIONS
	///////////////////////////////////////////////////////////////////////////

	/* all label defs */
	public List<VertexLabelDefinition> getLabelDefinitions() {
		def ldefs = new ArrayList<VertexLabelDefinition>()
		ldefs.addAll(staticLabelDefinitions)
		ldefs.addAll(dynamicLabelDefinitions)
		return ldefs
	}

	/** dynamic label defs */
	Set<VertexLabelDefinition> dynamicLabelDefinitions = new HashSet<VertexLabelDefinition>()

	/** static label definitions */
	final List<VertexLabelDefinition> staticLabelDefinitions = []


	///////////////////////////////////////////////////////////////////////////
	// RELATIONSHIP DEFINITIONS
	///////////////////////////////////////////////////////////////////////////

	/* all label defs */
	public List<RelationshipDefinition> getRelationshipDefinitions() {
		def defs = new ArrayList<RelationshipDefinition>()
		defs.addAll(staticRelationshipDefinitions)
		defs.addAll(dynamicRelationshipDefinitions)
		return defs
	}
	

	/** dynamic label defs */
	Set<RelationshipDefinition> dynamicRelationshipDefinitions = new HashSet<RelationshipDefinition>()


	/** */
	List<RelationshipDefinition> staticRelationshipDefinitions = []

}
