package carnival.core.graph



import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.Core



/**
 * An object wrapper for an executed graph method, which relies on the graph
 * representation of the process for all data.
 *
 */
class GraphMethodProcess {

    /** The vertex representing the graph method process */
    public Vertex vertex


    /** 
     * Return the graph process vertex.
     * @return The vertex representing the graph process.
     */
    public Vertex vertex() {
        this.vertex
    }


    /** 
     * Get the outputs of the graph method process from the graph.
     * @param g The graph traversal source to use
     * @return The set of output vertices
     */
    public Set<Vertex> outputs(GraphTraversalSource g) {
        assert g
        assert vertex
        g.V(vertex).in(Core.EX.IS_OUTPUT_OF).toSet()
    }


    /** 
     * Get the inputs of the graph method process from the graph.
     * @param g The graph traversal source to use
     * @return The set of input vertices
     */
    public Set<Vertex> inputs(GraphTraversalSource g) {
        assert g
        assert vertex
        g.V(vertex).in(Core.EX.IS_INPUT_OF).toSet()
    }

}