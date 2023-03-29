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

    /** */
    public Vertex vertex


    /** */
    public Vertex vertex() {
        this.vertex
    }


    /** */
    public Set<Vertex> outputs(GraphTraversalSource g) {
        assert g
        assert vertex
        g.V(vertex).in(Core.EX.IS_OUTPUT_OF).toSet()
    }


    /** */
    public Set<Vertex> inputs(GraphTraversalSource g) {
        assert g
        assert vertex
        g.V(vertex).in(Core.EX.IS_INPUT_OF).toSet()
    }

}