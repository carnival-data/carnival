package carnival.core.graph



import java.time.Instant
import groovy.util.logging.Slf4j
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.Base




/**
 * A list of graph methods.
 */
@Slf4j
class GraphMethodList {    

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** The underlying list of graph methods */
    List<GraphMethod> graphMethods = new ArrayList<GraphMethod>()


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Set the arguments of the graph methods prior to executing them.
     * @param args The  map of arguments to use for each call.
     * @return This object
     */
    public GraphMethodList arguments(Map args) {
        assert args != null

        graphMethods.each {
            it.arguments(args)
        }

        this
    }



    /**
     * Call each of the graph methods in this list.
     * @param g The graph traversal source to use
     * @param graph The graph to use
     * @return The list of graph method call objects
     */
    public List<GraphMethodCall> call(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null

        List<GraphMethodCall> methodCalls = new ArrayList<GraphMethodCall>()

        graphMethods.each {
            log.trace "call graph method ${it.name}"
            GraphMethodCall gmc = it.call(graph, g)
            methodCalls.add(gmc)
        }

        return methodCalls
    }


    /**
     * Call each of the graph methods in this list if it has not already been
     * called.
     * @param g The graph traversal source to use
     * @param graph The graph to use
     * @return The list of graph method call objects
     */
    public List<GraphMethodCall> ensure(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null

        List<GraphMethodCall> methodCalls = new ArrayList<GraphMethodCall>()

        graphMethods.each {
            log.trace "ensure graph method ${it.name}"
            GraphMethodCall gmc = it.ensure(graph, g)
            methodCalls.add(gmc)
        }

        return methodCalls
    }

}