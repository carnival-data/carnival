package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource



/** 
 * An interface for a graph validator object.
 */
interface GraphValidator {

	/** 
	 * Check the provided graph schema constraints using the provided graph
	 * traversal source.
	 * @param graphSchema The graph schema
	 * @param g The graph traversal source
	 * @return A list of graph validation errors
	 */
	public List<GraphValidationError> checkConstraints(GraphTraversalSource g, GraphSchema graphSchema)

	/** 
	 * Check the provided graph schema model using the provided graph traversal
	 * source.
	 * @param graphSchema The graph schema
	 * @param g The graph traversal source
	 * @return A list of graph model error strings
	 */
	public Collection<String> checkModel(GraphTraversalSource g, GraphSchema graphSchema)

}


