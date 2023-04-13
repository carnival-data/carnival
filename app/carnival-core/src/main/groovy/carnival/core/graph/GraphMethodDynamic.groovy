package carnival.core.graph



import java.time.Instant
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.Base
import carnival.graph.VertexDefinition



/**
 * A dymamically created graph method.
 */
class GraphMethodDynamic extends GraphMethodBase {   


    /** A singleton GraphMethodDynamic that can be shared */
    static public final GM = new GraphMethodDynamic()


    ///////////////////////////////////////////////////////////////////////////
    // CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Calls the execute() method and represents the call in the graph.
     * @param cl The closure to call
     * @param g The grpah traversal source to pass to the closure
     * @param graph The graph to pass to the closure
     * @return The graph method call object representing the execution.
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

    /** 
     * Call the graph method if it has not already been called.
     * @param cl The closure to call
     * @param g The grpah traversal source to pass to the closure
     * @param graph The graph to pass to the closure
     * @return The graph method call object representing the execution if it
     * happened; null otherwise
     */
    public GraphMethodCall ensure(Graph graph, GraphTraversalSource g, Closure cl) {
        assert graph != null
        assert g != null
        assert cl != null
        
        Set<Vertex> existingProcessVs = processes(g)
        if (existingProcessVs.size() == 0) return call(graph, g, cl)
        else return null
    }


}