package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import carnival.core.graph.Core
import carnival.graph.VertexDefinition



public class GraphMethodProcessSpec extends Specification {


    @VertexDefinition
    static enum VX {
        SOME_REAPER_PROCESS,
        SOME_REAPER_OUTPUT
    }


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////

    @Shared coreGraph
    @Shared graph
    @Shared g


    def setupSpec() {
    } 


    def cleanupSpec() {
    }

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

    void "vertex() returns vertex"() {
        when:
        def v = graph.addVertex('TEST_VERTEX_LABEL')
        def rmp = new GraphMethodProcess()
        rmp.vertex = v
        def v2 = rmp.vertex()

        then:
        v2 != null
        v2 == v
    }


    void "outputs() works"() {
        when:
        def procV = VX.SOME_REAPER_PROCESS.instance().create(graph)
        def input1V = VX.SOME_REAPER_OUTPUT.instance().create(graph)
        Core.EX.IS_OUTPUT_OF.instance()
            .from(input1V)
            .to(procV)
        .create()

        def rmp = new GraphMethodProcess()
        rmp.vertex = procV

        def outputs = rmp.outputs(g)

        then:
        outputs != null
        outputs.size() == 1
        outputs[0] == input1V
    }

}