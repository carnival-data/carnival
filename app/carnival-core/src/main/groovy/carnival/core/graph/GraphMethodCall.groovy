package carnival.core.graph



import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource



/**
 * A class to represent the execution of a graph method.
 *
 */
class GraphMethodCall {

    /**
     * The arguments that were used in the execution of the method logic and
     * for unique naming of graph representation.
     */
    Map arguments


    /**
     * The result of the execution.
     */
    Map result


    /**
     * The graph representation entry point of the executed method. 
     */
    Vertex processVertex

    
    /**
     * Return the arguments used by this method.
     */
    public Map arguments() {
        this.arguments
    }


    /**
     * Return a GraphMethodProcess object wrapper for the graph representation
     * of the executed method.
     * @param g The graph traversal source to use
     * @return The graph method process object
     */
    public GraphMethodProcess process(GraphTraversalSource g) {
        new GraphMethodProcess(vertex:processVertex)
    }

}