package carnival.core.graph



import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import org.apache.commons.io.FileUtils

import carnival.graph.*




/**
 * gradle test --tests "carnival.core.graph.ReaperSpec"
 *
 *
 */
class ReaperSpec extends Specification {


    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
    } 


    def cleanupSpec() {
    }


    def cleanup() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    void initGraph(Graph graph, GraphTraversalSource g) {
        [
            Core.VX.PROCESS_CLASS,
            Core.VX.DATA_TRANSFORMATION_PROCESS_CLASS,
            Reaper.VX.REAPER_PROCESS_CLASS,
        ].each { vdef ->
            vdef.vertex = vdef.controlledInstance().vertex(graph, g)
        }
        Reaper.VX.REAPER_PROCESS_CLASS.setSubclassOf(g, Core.VX.DATA_TRANSFORMATION_PROCESS_CLASS)     
        ReaperWithDefs.ReaperMethodWithDefs.VX.values().each { 
            it.vertex = it.controlledInstance().vertex(graph, g)
            it.setSubclassOf(g, Reaper.VX.REAPER_PROCESS_CLASS)
        }   
    }

    def "default tracked process"() {
        given:
        def g1 = TinkerGraph.open()
        def g = g1.traversal()

        expect:
        g.V().hasLabel('ReaperMethod1Process').toList().size() == 0

        when:
        initGraph(g1, g)

        then:
        g.V().hasLabel('ReaperMethod1Process').toList().size() == 0

        def reaper = new SimpleReaper(g1)
        def res
        def methodArgs


        when:
        methodArgs = [reap:{return [graphModified:false]}]
        res = reaper.call('ReaperMethod1', methodArgs)
        g.V().toList().each { println "${it} ${it.label()}" }

        then:
        g.V().hasLabel('ReaperMethod1Process').tryNext().isPresent()
        res.reaperMethodInstance
        res.reaperMethodInstance.getAllTrackedProcesses(g).size() == 1

        when:
        def tps1 = res.reaperMethodInstance.getAllTrackedProcesses(g)
        def tps2 = reaper.getAllTrackedProcesses(SimpleReaper.ReaperMethod1)

        then:
        tps1 
        tps2
        tps1.size() == 1
        tps2.size() == 1
        tps1.first() == tps2.first()

        when:
        def procV = tps1.first()

        then:
        procV
        g.V(procV).out(Core.EX.IS_INSTANCE_OF.label).tryNext().isPresent()
        g.V(procV).out(Core.EX.IS_INSTANCE_OF.label).count().next() == 1

        when:
        def reaperProcClassV = g.V(procV).out(Core.EX.IS_INSTANCE_OF.label).next()

        then:
        reaperProcClassV.label() == 'ReaperMethod1ProcessClass'

        when:
        def dataTransProcClassV = Core.VX.DATA_TRANSFORMATION_PROCESS_CLASS.vertex

        then:
        dataTransProcClassV
        g.V(reaperProcClassV).out(Base.EX.IS_SUBCLASS_OF.label).is(Reaper.VX.REAPER_PROCESS_CLASS.vertex).tryNext().isPresent()

        g.V(Reaper.VX.REAPER_PROCESS_CLASS.vertex)
            .out(Base.EX.IS_SUBCLASS_OF.label)
            .is(dataTransProcClassV)
        .tryNext().isPresent()

        when:
        def procClassV = Core.VX.PROCESS_CLASS.vertex

        then:
        procClassV
        g.V(dataTransProcClassV).out(Base.EX.IS_SUBCLASS_OF.label).is(procClassV).tryNext().isPresent()
    }


    def "graph initialization"() {
        when:
        def g1 = TinkerGraph.open()
        initGraph(g1, g1.traversal())
        def reaper = new ReaperWithDefs(g1)
        def res = reaper.reaperMethodWithDefs()
        println "res: $res"

        then:
        res != null
    }


    def "resource propagation"() {
        given:
        def reaper = new SimpleReaper()
        def rm

        when:
        def g1 = TinkerGraph.open()
        reaper.graph = g1
        rm = reaper.createReaperMethodInstance('ReaperMethod1')

        then:
        rm.graph.hashCode() == g1.hashCode()

        when:
        def g2 = TinkerGraph.open()
        reaper.graph = g2
        rm = reaper.createReaperMethodInstance('ReaperMethod1')

        then:
        g1.hashCode() != g2.hashCode()
        rm.graph.hashCode() == g2.hashCode()
    }


    def "call via methodMissing fail"() {
        given:
        def reaper = new SimpleReaper()
        Exception e

        when:
        def methodArgs = [
            reap:{ Map args -> 
                return [graphModified:false] 
            }
        ]
        def res = reaper.reaperMethod1_(methodArgs)

        then:
        e = thrown()
    }


    def "call via methodMissing"() {
        given:
        def reaper = new SimpleReaper()

        when:
        def methodArgs = [
            reap:{ Map args -> 
                return [graphModified:false] 
            }
        ]
        def res = reaper.reaperMethod1(methodArgs)

        then:
        res.success == true
        res.reap != null
        res.reap['graphModified'] == false
    }


    /* relaxed this requirement
    def "call reap must have graphModified"() {
        given:
        def reaper = new SimpleReaper()
        Throwable e

        when:
        def methodArgs = [
            reap:{ Map args -> 
                return [graphModified_:false] 
            }
        ]
        def res = reaper.call('ReaperMethod1', methodArgs)

        then:
        e = thrown()
    }*/


    def "call reap"() {
        given:
        def reaper = new SimpleReaper()

        when:
        def methodArgs = [
            reap:{ Map args -> 
                return [graphModified:false] 
            }
        ]
        def res = reaper.call('ReaperMethod1', methodArgs)

        then:
        res.success == true
        res.reap != null
        res.reap['graphModified'] == false
    }


    def "call postCondition fail"() {
        given:
        def reaper = new SimpleReaper()

        when:
        def methodArgs = [
            reap:{ Map args -> 
                return [graphModified:false] 
            },
            post:{ Map args -> 
                return [new GraphValidationError(message:'fail1')] 
            }
        ]
        def res = reaper.call('ReaperMethod1', methodArgs)

        then:
        res.success == false
        res.checkPostConditions.size() == 1

        when:
        def err = res.checkPostConditions.first()
        
        then:
        err.message == 'fail1'
    }


    def "call preCondition fail"() {
        given:
        def reaper = new SimpleReaper()

        when:
        def methodArgs = [
            pre:{ Map args -> 
                return [new GraphValidationError(message:'fail1')] 
            }
        ]
        def res = reaper.call('ReaperMethod1', methodArgs)

        then:
        res.success == false
        res.checkPreConditions.size() == 1

        when:
        def err = res.checkPreConditions.first()
        
        then:
        err.message == 'fail1'
    }


    def "findReaperMethodClass invalid"() {
        given:
        def reaper = new SimpleReaper()

        when:
        def rm = reaper.findReaperMethodClass(name)

        then:
        rm == null

        where:
        name << [
            'reaperMethod2', 
            'dfsdfsdf'
        ]
    }



    def "findReaperMethodClass valid"() {
        given:
        def reaper = new SimpleReaper()

        when:
        def rm = reaper.findReaperMethodClass(name)

        then:
        rm != null
        rm.name.endsWith("ReaperMethod1")

        where:
        name            | shouldFindIt
        'reaperMethod1' | true
        'reapermethod1' | true
        'ReaperMethod1' | true
        'REAPERMETHOD1' | true
    }


    def "getAllReaperMethodClasses"() {
        given:
        def reaper = new SimpleReaper()

        when:
        def reaperMethodClasses = reaper.getAllReaperMethodClasses()

        then:
        reaperMethodClasses != null
        reaperMethodClasses.size() == 1

        reaperMethodClasses.find { it.name.endsWith("ReaperMethod1") }
    }

}



/** */
@InheritConstructors
class ReaperWithDefs extends SimpleReaper {

    static class ReaperMethodWithDefs extends ReaperMethod {

        /** */
        static enum VX implements VertexDefTrait {
            SOME_REAPER_PROCESS_CLASS,
            SOME_REAPER_PROCESS,
        }

        /** */
        @Override
        VertexDefTrait getTrackedProcessClassDef() { return VX.SOME_REAPER_PROCESS_CLASS }

        /** */
        @Override
        VertexDefTrait getTrackedProcessDef() { return VX.SOME_REAPER_PROCESS }


        Collection<GraphValidationError> checkPreConditions(Map args) {
            log.debug "ReaperMethodWithDefs checkPreConditions"
            return []
        }

        Map reap(Map args = [:]) {
            log.debug "ReaperMethodWithDefs reap"
            return [graphModified:false]
        }

        Collection<GraphValidationError> checkPostConditions(Map args, Map results = [:]) {
            log.debug "ReaperMethodWithDefs checkPostConditions"
            return []
        }

    }


}



/** */
class SimpleReaper extends Reaper {

    @ReaperMethodResource
    Graph graph

    public SimpleReaper() {
        this.graph = TinkerGraph.open()
    }

    public SimpleReaper(Graph graph) {
        assert graph
        this.graph = graph
    }

    protected Graph getGraph() { graph }

    protected GraphTraversalSource traversal() {
        graph.traversal()
    }


    static class ReaperMethod1 extends ReaperMethod {

        Collection<GraphValidationError> checkPreConditions(Map args) {
            log.debug "ReaperMethod1 checkPreConditions"
            if (args.pre) return args.pre(args)
            return []
        }

        Map reap(Map args = [:]) {
            log.debug "ReaperMethod1 reap"
            if (args.reap) return args.reap(args)
            return [:]
        }

        Collection<GraphValidationError> checkPostConditions(Map args, Map results = [:]) {
            log.debug "ReaperMethod1 checkPostConditions"
            if (args.post) return args.post(args)
            return []
        }

    }
}




