package carnival.core.graph



import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.graph.Core



class GraphMethodProcess {


    public Vertex vertex


    public Vertex vertex() {
        this.vertex
    }


    public Set<Vertex> outputs(GraphTraversalSource g) {
        assert g
        assert vertex
        g.V(vertex).in(Core.EX.IS_OUTPUT_OF).toSet()
    }

}