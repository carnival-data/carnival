@Grab('org.pmbb:carnival-graph:2.0.1-SNAPSHOT')
@Grab('org.pmbb:carnival-core:2.0.1-SNAPSHOT')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.8')


import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import carnival.graph.VertexDefinition
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod




@VertexDefinition
enum VX {
    SOME_REAPER_PROCESS_CLASS,
    SOME_REAPER_PROCESS,
    SOME_REAPER_OUTPUT,
    SOME_THING
}


class GmsTestMethods implements GraphMethods {

    class TestGraphMethod extends GraphMethod {
        public Map execute(Graph graph, GraphTraversalSource g) {
            VX.SOME_THING.instance().create(graph)
        }
    }

    class TestGraphMethodThrowsException extends GraphMethod {
        public Map execute(Graph graph, GraphTraversalSource g) {
            throw new Exception('boom')
        }
    }

}


println "HELLOOOOO"
