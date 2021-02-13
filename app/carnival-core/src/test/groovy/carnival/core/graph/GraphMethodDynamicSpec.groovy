package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.graph.Core
import carnival.graph.Base
import carnival.graph.VertexDefinition
import carnival.graph.VertexDefTrait
import static carnival.core.graph.GraphMethodDynamic.GM



/** */
public class GraphMethodDynamicSpec extends Specification {


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


    void "call() handles exceptions"() {
        when:
        def gmc = GM.arguments(a:'1', b:'2').call(graph, g) { 
            throw new Exception('boom')
        }

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
        def gmc = GM.arguments(a:'1', b:'2').call(graph, g) { [r1:'v1'] }

        then:
        gmc != null
        gmc.processVertex != null

        when:
        def procV = gmc.processVertex

        then:
        Core.VX.GRAPH_PROCESS.isa(procV)
        Core.PX.NAME.of(procV).isPresent()
        Core.PX.START_TIME.of(procV).isPresent()
        Core.PX.STOP_TIME.of(procV).isPresent()
        !Core.PX.EXCEPTION_MESSAGE.of(procV).isPresent()

        Core.PX.NAME.valueOf(procV) == 'carnival.core.graph.GraphMethodDynamic'
    }


    void "call() receives graph traversal source"() {
        when:
        def gmc = GM.arguments(a:'1', b:'2').call(graph, g) { args, graph, g ->
            assert args != null
            assert graph != null
            assert g != null
            assert g instanceof GraphTraversalSource
            return args 
        }

        then:
        gmc != null
    }


    void "call() receives graph"() {
        when:
        def gmc = GM.arguments(a:'1', b:'2').call(graph, g) { args, graph ->
            assert args != null
            assert graph != null
            assert graph instanceof Graph
            return args 
        }

        then:
        gmc != null
    }


    void "call() receives arguments"() {
        when:
        def gmc = GM.arguments(a:'1', b:'2').call(graph, g) { args ->
            return args 
        }

        then:
        gmc != null
        gmc.arguments == [a:'1', b:'2']
        gmc.result == [a:'1', b:'2']
    }



    void "call() returns a good GraphMethodCall"() {
        when:
        def gmc = GM.arguments(a:'1', b:'2').call(graph, g) { [r1:'v1'] }

        then:
        gmc != null
        gmc.arguments == [a:'1', b:'2']
        gmc.result == [r1:'v1']
    }


}