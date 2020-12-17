package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.graph.Core
import carnival.graph.VertexDefinition



public class GraphMethodSpec extends Specification {


    @VertexDefinition
    static enum VX {
        SOME_REAPER_PROCESS,
        SOME_REAPER_OUTPUT
    }

    static class TestGraphMethod extends GraphMethod {
        public Map execute(Graph graph, GraphTraversalSource g) {
            [:]
        }
    }

    static class TestGraphMethodThrowsException extends GraphMethod {
        public Map execute(Graph graph, GraphTraversalSource g) {
            throw new Exception('boom')
        }
    }



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////

    @Shared coreGraph
    @Shared graph
    @Shared g


    def setupSpec() { } 

    def cleanupSpec() { }

    def setup() {
        coreGraph = CoreGraphTinker.create()
        graph = coreGraph.graph
        g = graph.traversal()
    }

    def cleanup() {
        if (coreGraph) coreGraph.close()
        if (g) g.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    void "processes() finds processes"() {
        when:
        def gm = new TestGraphMethod()
        gm.arguments(a:'1', b:'2')
        def procVs = gm.processes(g)

        then:
        procVs != null
        procVs.size() == 0

        when:
        def gmc = gm.call(graph, g)
        procVs = gm.processes(g)

        then:
        procVs != null
        procVs.size() == 1
    }


    void "call() handles exceptions"() {
        when:
        def gm = new TestGraphMethodThrowsException()
        gm.arguments(a:'1', b:'2')
        def gmc = gm.call(graph, g)

        then:
        Exception e = thrown()
        gmc == null

        when:
        def procVs = g.V().isa(Core.VX.GRAPH_PROCESS).toSet()

        then:
        procVs.size() == 1

        when:
        def procV = procVs.first()

        then:
        Core.PX.START_TIME.of(procV).isPresent()
        Core.PX.STOP_TIME.of(procV).isPresent()
        Core.PX.EXCEPTION_MESSAGE.of(procV).isPresent()
        Core.PX.EXCEPTION_MESSAGE.valueOf(procV) == 'boom'
    }


    void "call() creates a good process vertex"() {
        when:
        def gm = new TestGraphMethod()
        gm.arguments(a:'1', b:'2')
        def gmc = gm.call(graph, g)

        then:
        gmc != null
        gmc.processVertex != null

        when:
        def procV = gmc.processVertex

        then:
        Core.PX.START_TIME.of(procV).isPresent()
        Core.PX.STOP_TIME.of(procV).isPresent()
        !Core.PX.EXCEPTION_MESSAGE.of(procV).isPresent()
    }


    void "call() returns a GraphMethodSpec with arguments"() {
        when:
        def gm = new TestGraphMethod()
        gm.arguments(a:'1', b:'2')
        def gmc = gm.call(graph, g)

        then:
        gmc != null
        gmc.arguments == [a:'1', b:'2']
    }


    void "arguments() set"() {
        when:
        def gm = new TestGraphMethod()
        gm.arguments(a:'1', b:'2')

        then:
        gm.arguments != null
        gm.arguments instanceof Map
        gm.arguments.equals([a:'1', b:'2'])
    }


}