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
class GraphMethodDynamic extends GraphMethodBase {   


    /** */
    static public final GM = new GraphMethodDynamic()


    ///////////////////////////////////////////////////////////////////////////
    // CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Calls the execute() method and represents the call in the graph.
     *
     */
    public GraphMethodCall call(Graph graph, GraphTraversalSource g, Closure cl) {
        assert graph != null
        assert g != null
        assert cl != null

        Map result 
        Instant stopTime
        Exception exception
        Instant startTime
        
        // execute the graph method recording the start
        // and stop times
        try {
            startTime = Instant.now()
            if (cl.getMaximumNumberOfParameters() == 0) {
                result = cl()
            } else if (cl.getMaximumNumberOfParameters() == 1) {
                result = cl(this.arguments)
            } else if (cl.getMaximumNumberOfParameters() == 2) {
                result = cl(this.arguments, graph)
            } else {
                result = cl(this.arguments, graph, g)
            }
        } catch (Exception e) {
            exception = e
        } finally {
            stopTime = Instant.now()
        }

        graphMethodCall(graph, g, startTime, stopTime, result, exception)
    }


    ///////////////////////////////////////////////////////////////////////////
    // ENSURE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public GraphMethodCall ensure(Graph graph, GraphTraversalSource g, Closure cl) {
        assert graph != null
        assert g != null
        assert cl != null
        
        Set<Vertex> existingProcessVs = processes(g)
        if (existingProcessVs.size() == 0) return call(graph, g, cl)
        else return null
    }


}