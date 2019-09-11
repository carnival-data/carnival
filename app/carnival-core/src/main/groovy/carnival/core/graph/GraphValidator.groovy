package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource



/** */
interface GraphValidator {

	/** */
	public List<GraphValidationError> checkConstraints(GraphTraversalSource g, GraphSchema graphSchema)

	/** */
	public Collection<String> checkModel(GraphTraversalSource g, GraphSchema graphSchema)

}


