package carnival.core.graph



import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource



/**
 *
 *
 */
class GraphMethodCall {

    /**
     * The arguments that were used in the execution of the method logic and
     * for unique naming of graph representation.
     */
    Map arguments


    /**
     *
     *
     */
    Map result


    /**
     * The graph representation entry point of the executed method. 
     */
    Vertex processVertex

    
    /**
     * Return the arguments used by this method.
     *
     */
    public Map arguments() {
        this.arguments
    }


    /**
     * Return a GraphMethodProcess object wrapper for the graph representation
     * of the executed method.
     *
     */
    public GraphMethodProcess process(GraphTraversalSource g) {
        new GraphMethodProcess(vertex:processVertex)
    }

}