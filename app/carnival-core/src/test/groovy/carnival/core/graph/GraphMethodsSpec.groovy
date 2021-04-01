package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.graph.Core
import carnival.graph.Base
import carnival.graph.VertexDefinition




class GmsTestMethods implements GraphMethods {

    class TestGraphMethod extends GraphMethod {
        public void execute(Graph graph, GraphTraversalSource g) {}
    }

    class TestGraphMethodThrowsException extends GraphMethod {
        public void execute(Graph graph, GraphTraversalSource g) {
            throw new Exception('boom')
        }
    }

}



public class GraphMethodsSpec extends Specification {


    @VertexDefinition
    static enum VX {
        SOME_REAPER_PROCESS_CLASS,
        SOME_REAPER_PROCESS,
        SOME_REAPER_OUTPUT
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

    void "processDefinition() sets process vertex def"() {
        when:
        def gms = new GmsTestMethods()
        def gmc = gms
            .method('TestGraphMethod')
            .arguments(a:'1', b:'2')
            .processDefinition(VX.SOME_REAPER_PROCESS)
        .call(graph, g)

        then:
        gmc != null
        gmc.processVertex != null

        when:
        def procV = gmc.processVertex

        then:
        VX.SOME_REAPER_PROCESS.isa(procV)
        Core.PX.START_TIME.of(procV).isPresent()
        Core.PX.STOP_TIME.of(procV).isPresent()
        !Core.PX.EXCEPTION_MESSAGE.of(procV).isPresent()

        g.V(procV)
            .out(Core.EX.IS_INSTANCE_OF)
            .is(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .tryNext()
        .isPresent()
    }


    void "call() creates a good process vertex"() {
        when:
        def gms = new GmsTestMethods()
        def gmc = gms
            .method('TestGraphMethod')
            .arguments(a:'1', b:'2')
        .call(graph, g)

        then:
        gmc != null
        gmc.processVertex != null

        when:
        def procV = gmc.processVertex

        then:
        Core.VX.GRAPH_PROCESS.isa(procV)
        Core.PX.START_TIME.of(procV).isPresent()
        Core.PX.STOP_TIME.of(procV).isPresent()
        !Core.PX.EXCEPTION_MESSAGE.of(procV).isPresent()
    }



    void "method()"() {
        when:
        def gms = new GmsTestMethods()
        def gm = gms.method('TestGraphMethod')

        then:
        gm != null
        gm instanceof GmsTestMethods.TestGraphMethod
    }


    

}