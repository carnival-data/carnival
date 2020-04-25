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
class DefaultReaperMethod extends ReaperMethod {


    Collection<GraphValidationError> checkPreConditions(Map args) {
        return []
    }


    Map reap(Map args) {
        return [:]
    }



    Collection<GraphValidationError> checkPostConditions(Map args, Map result) {
        return []
    }

}




