package carnival.core.graph



import java.time.Instant
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.util.CoreUtil
import carnival.graph.Base
import carnival.graph.VertexDefTrait



/**
 * GraphMethod encapsulates a unit of business logic that modifies the graph.
 * GraphMethods may return a Map result, but the fundamental result of a graph
 * method is a mutation of the graph.  An executed graph method will be
 * represented in the graph as a "process" vertex with optional links to
 * outputs.
 *
 * 
 *
 */
abstract class GraphMethod extends GraphMethodBase {    


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * An abstract method to be implemented by the concretizing class to
     * implement the logic of the method.
     *
     */
    abstract void execute(Graph graph, GraphTraversalSource g) 


    ///////////////////////////////////////////////////////////////////////////
    // RESULT
    ///////////////////////////////////////////////////////////////////////////

    Map result = [:]


    ///////////////////////////////////////////////////////////////////////////
    // CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Calls the execute() method and represents the call in the graph.
     *
     */
    public GraphMethodCall call(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null

        Instant stopTime
        Exception exception
        Instant startTime
        
        // execute the graph method recording the start
        // and stop times
        try {
            startTime = Instant.now()
            execute(graph, g)
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
    public GraphMethodCall ensure(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null
        
        Set<Vertex> existingProcessVs = processes(g)
        if (existingProcessVs.size() == 0) return call(graph, g)
        else return null
    }


}