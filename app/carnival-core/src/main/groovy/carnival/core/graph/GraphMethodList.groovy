package carnival.core.graph



import java.time.Instant
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.util.CoreUtil
import carnival.graph.Base
import carnival.graph.VertexDefTrait



/**
 *
 */
class GraphMethodList {    

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    List<GraphMethod> graphMethods = new ArrayList<GraphMethod>()


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Set the arguments of the graph methods prior to executing them.
     *
     */
    public GraphMethodList arguments(Map args) {
        assert args != null

        graphMethods.each {
            it.arguments(args)
        }

        this
    }



    /**
     *
     */
    public List<GraphMethodCall> call(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null

        List<GraphMethodCall> methodCalls = new ArrayList<GraphMethodCall>()

        graphMethods.each {
            GraphMethodCall gmc = it.call(graph, g)
            methodCalls.add(gmc)
        }

        return methodCalls
    }


    /**
     *
     */
    public List<GraphMethodCall> ensure(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null

        List<GraphMethodCall> methodCalls = new ArrayList<GraphMethodCall>()

        graphMethods.each {
            GraphMethodCall gmc = it.ensure(graph, g)
            methodCalls.add(gmc)
        }

        return methodCalls
    }

}