package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import carnival.core.graph.Core
import carnival.graph.VertexModel
import carnival.core.CarnivalTinker



public class GraphMethodCallSpec extends Specification {


    @VertexModel
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


    def setupSpec() { } 

    def cleanupSpec() { }

    def setup() {
        coreGraph = CarnivalTinker.create()
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

    void "arguments() returns arguments"() {
        when:
        def procV = VX.SOME_REAPER_PROCESS.instance().create(graph)
        def gmc = new GraphMethodCall()
        gmc.arguments = [a:'1', b:'2']
        def args = gmc.arguments()

        then:
        args != null
        args instanceof Map
        args.equals([a:'1', b:'2'])
    }


    void "process() works"() {
        when:
        def procV = VX.SOME_REAPER_PROCESS.instance().create(graph)
        def output1V = VX.SOME_REAPER_OUTPUT.instance().create(graph)
        Core.EX.IS_OUTPUT_OF.instance()
            .from(output1V)
            .to(procV)
        .create()

        def gmc = new GraphMethodCall()
        gmc.processVertex = procV

        def gmp = gmc.process(g)

        then:
        gmp != null
        gmp.vertex() == procV

        when:
        def gmpOutputs = gmp.outputs(g)

        then:
        gmpOutputs != null
        gmpOutputs.size() == 1
        gmpOutputs[0] == output1V
    }

}