package carnival.core.graph



import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource



class GraphMethodCall {

    Map arguments

    Vertex processVertex

    
    public Map arguments() {
        this.arguments
    }


    public GraphMethodProcess process(GraphTraversalSource g) {
        new GraphMethodProcess(vertex:processVertex)
    }


}