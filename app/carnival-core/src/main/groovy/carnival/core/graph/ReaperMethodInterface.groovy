package carnival.core.graph



import groovy.transform.Synchronized
import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.T

import carnival.graph.VertexDefTrait
import carnival.graph.DynamicVertexDef



/** */
public interface ReaperMethodInterface {

    /**
     * Check that the pre-conditions to running this reaper method with
     * the given args hold.
     *
     * @return Collection of GraphValidationError objects, each of which
     * represents a validation failure.  If there are no failures, an
     * empty collection is returned.
     *
     */
    Collection<GraphValidationError> checkPreConditions(Map args)


    /**
     * Run the reaper method with the given arguments.
     *
     */
    Map reap(Map args)


    /**
     * Check that the post-conditions to running this reaper method with
     * the given args hold.
     *
     * @return Collection of GraphValidationError objects, each of which
     * represents a validation failure.  If there are no failures, an
     * empty collection is returned.
     *
     */
    Collection<GraphValidationError> checkPostConditions(Map args, Map result)
}

