package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.graph.Core
import carnival.graph.Base
import carnival.graph.VertexModel
import carnival.graph.VertexDefinition



public class GraphMethodSpec extends Specification {


    @VertexModel
    static enum VX {
        SOME_REAPER_PROCESS_CLASS,
        SOME_REAPER_PROCESS,
        SOME_REAPER_OUTPUT
    }

    static class TestGraphMethodNameOveride extends GraphMethod {
        String name = 'my-funky-name'
        public void execute(Graph graph, GraphTraversalSource g) {}
    }

    static class TestGraphMethodProcessClassOveride extends GraphMethod {
        VertexDefinition processVertexDef = GraphMethodSpec.VX.SOME_REAPER_PROCESS
        VertexDefinition processClassVertexDef = GraphMethodSpec.VX.SOME_REAPER_PROCESS_CLASS
        public void execute(Graph graph, GraphTraversalSource g) {}
    }

    static class TestGraphMethodProcessOveride extends GraphMethod {
        VertexDefinition processVertexDef = GraphMethodSpec.VX.SOME_REAPER_PROCESS
        public void execute(Graph graph, GraphTraversalSource g) {}
    }

    static class TestGraphMethod extends GraphMethod {
        public void execute(Graph graph, GraphTraversalSource g) {}
    }

    static class TestGraphMethodThrowsException extends GraphMethod {
        public void execute(Graph graph, GraphTraversalSource g) {
            throw new Exception('boom')
        }
    }

    static class TestGraphMethodArguments extends GraphMethod {
        public void execute(Graph graph, GraphTraversalSource g) {
            def args = arguments
            if (args == null) throw new Exception('args are null')
            assert args instanceof Map
            assert args.get('a') == 1
            assert args.get('b') == 2
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

    void "arguments"() {
        when:
        def gm = new TestGraphMethodArguments()
        gm.arguments(a:1, b:2)
        def gmc = gm.call(graph, g)

        then:
        noExceptionThrown()
    }


    void "name override"() {
        when:
        def gm = new TestGraphMethodNameOveride()
        gm.arguments(a:'1', b:'2')
        def gmc = gm.call(graph, g)

        then:
        gmc != null
        gmc.processVertex != null

        when:
        def procV = gmc.processVertex

        then:
        Core.PX.NAME.of(procV).isPresent()
        Core.PX.NAME.valueOf(procV) == 'my-funky-name'
    }


    void "processClassDefinition override"() {
        when:
        def gm = new TestGraphMethodProcessClassOveride()
        gm.arguments(a:'1', b:'2')
        def gmc = gm.call(graph, g)

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
            .out(Base.EX.IS_INSTANCE_OF)
            .is(VX.SOME_REAPER_PROCESS_CLASS.vertex)
            .tryNext()
        .isPresent()

        g.V(VX.SOME_REAPER_PROCESS_CLASS.vertex)
            .out(Base.EX.IS_SUBCLASS_OF)
            .is(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .tryNext()
        .isPresent()
    }


    void "processDefinition override"() {
        when:
        def gm = new TestGraphMethodProcessOveride()
        gm.arguments(a:'1', b:'2')
        def gmc = gm.call(graph, g)

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
            .out(Base.EX.IS_INSTANCE_OF)
            .is(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .tryNext()
        .isPresent()
    }

    
    void "processClassDefinition() sets process class vertex def"() {
        when:
        def gm = new TestGraphMethod()
        gm.arguments(a:'1', b:'2')
            .processDefinition(VX.SOME_REAPER_PROCESS)
            .processClassDefinition(VX.SOME_REAPER_PROCESS_CLASS)            
        def gmc = gm.call(graph, g)

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
            .out(Base.EX.IS_INSTANCE_OF)
            .is(VX.SOME_REAPER_PROCESS_CLASS.vertex)
            .tryNext()
        .isPresent()

        g.V(VX.SOME_REAPER_PROCESS_CLASS.vertex)
            .out(Base.EX.IS_SUBCLASS_OF)
            .is(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .tryNext()
        .isPresent()
    }



   void "processDefinition() sets process vertex def"() {
        when:
        def gm = new TestGraphMethod()
        gm.arguments(a:'1', b:'2')
        gm.processDefinition(VX.SOME_REAPER_PROCESS)
        def gmc = gm.call(graph, g)

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
            .out(Base.EX.IS_INSTANCE_OF)
            .is(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .tryNext()
        .isPresent()
    }


    ///////////////////////////////////////////////////////////////////////////
    // ENSURE
    ///////////////////////////////////////////////////////////////////////////
    
    void "ensure() returns null when work is already done"() {
        when:
        def res1 = new TestGraphMethod().name('n1').arguments(a:'1').call(graph, g)
        def res2 = new TestGraphMethod().name('n1').arguments(a:'1').ensure(graph, g)

        then:
        res1 != null
        res2 == null
    }


    void "ensure() does not re-do work"() {
        expect:
        new TestGraphMethod().processes(g).size() == 0
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0

        when:
        new TestGraphMethod().name('n1').arguments(a:'1').call(graph, g)
        new TestGraphMethod().name('n2').arguments(a:'1').call(graph, g)
        new TestGraphMethod().name('n2').arguments(a:'1').call(graph, g)
        
        new TestGraphMethod().name('n2').arguments(a:'1').ensure(graph, g)

        then:
        new TestGraphMethod().processes(g).size() == 0
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0
        new TestGraphMethod().name('n1').processes(g).size() == 0
        new TestGraphMethod().name('n1').arguments(a:'1').processes(g).size() == 1
        new TestGraphMethod().name('n2').processes(g).size() == 0
        new TestGraphMethod().name('n2').arguments(a:'1').processes(g).size() == 2
    }


    void "ensure() works first time"() {
        expect:
        new TestGraphMethod().processes(g).size() == 0
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0

        when:
        new TestGraphMethod().name('n2').arguments(a:'1').ensure(graph, g)

        then:
        new TestGraphMethod().processes(g).size() == 0
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0
        new TestGraphMethod().name('n2').arguments(a:'1').processes(g).size() == 1
    }


    ///////////////////////////////////////////////////////////////////////////
    // PROCESSES
    ///////////////////////////////////////////////////////////////////////////

    void "processes() differentiates on name and arguments"() {
        expect:
        new TestGraphMethod().processes(g).size() == 0
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0

        when:
        new TestGraphMethod().name('n1').arguments(a:'1').call(graph, g)
        new TestGraphMethod().name('n2').arguments(a:'1').call(graph, g)
        new TestGraphMethod().name('n2').arguments(a:'1').call(graph, g)

        then:
        new TestGraphMethod().processes(g).size() == 0
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0
        new TestGraphMethod().name('n1').processes(g).size() == 0
        new TestGraphMethod().name('n1').arguments(a:'1').processes(g).size() == 1
        new TestGraphMethod().name('n2').processes(g).size() == 0
        new TestGraphMethod().name('n2').arguments(a:'1').processes(g).size() == 2
    }


    void "processes() differentiates on arguments"() {
        expect:
        new TestGraphMethod().processes(g).size() == 0
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0

        when:
        new TestGraphMethod().call(graph, g)

        then:
        new TestGraphMethod().processes(g).size() == 1
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 0

        when:
        new TestGraphMethod().arguments(a:'1').call(graph, g)

        then:
        new TestGraphMethod().processes(g).size() == 1
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 1

        when:
        new TestGraphMethod().arguments(a:'1').call(graph, g)
        new TestGraphMethod().arguments(a:'2').call(graph, g)

        then:
        new TestGraphMethod().processes(g).size() == 1
        new TestGraphMethod().arguments(a:'1').processes(g).size() == 2
        new TestGraphMethod().arguments(a:'2').processes(g).size() == 1
    }



    void "processes()"() {
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



    ///////////////////////////////////////////////////////////////////////////
    // call()
    ///////////////////////////////////////////////////////////////////////////


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
        Core.VX.GRAPH_PROCESS.isa(procV)
        Core.PX.NAME.of(procV).isPresent()
        Core.PX.START_TIME.of(procV).isPresent()
        Core.PX.STOP_TIME.of(procV).isPresent()
        !Core.PX.EXCEPTION_MESSAGE.of(procV).isPresent()

        Core.PX.NAME.valueOf(procV) == 'carnival.core.graph.GraphMethodSpec$TestGraphMethod'
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

        when:
        def argsAlias = gm.args

        then:
        argsAlias != null
        argsAlias instanceof Map
        argsAlias.equals([a:'1', b:'2'])
    }


}