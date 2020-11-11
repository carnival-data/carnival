package carnival.core.graph



import groovy.transform.InheritConstructors
import groovy.sql.*
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.*




/**
 * gradle test --tests "carnival.core.graph.ReasonerSpec"
 *
 *
 */
class ReasonerSpec extends Specification {

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

    @InheritConstructors
    static class MyReasoner extends Reasoner {
        public Map validate(Map args) {args}
        public Map reason(Map args) {args}
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    void "ensure differentiates by args"() {
        when:
        def r1 = new MyReasoner(graph)
        def o1 = r1.ensure(success:true, a:'a')

        then:
        o1.result.get('success') == true
        o1.processVertex != null

        when:
        def o2 = r1.ensure(success:true, a:'b')

        then:
        o2.result.get('success') == true
        o2.processVertex != null
        o2.processVertex != o1.processVertex

        when:
        def o3 = r1.ensure(success:true, a:'b')

        then:
        o3 == [:]
    }


    void "rerun after failure"() {
        when:
        def r1 = new MyReasoner(graph)
        def o1 = r1.ensure(success:false)

        then:
        o1.result.get('success') == false
        o1.processVertex != null

        when:
        def o2 = r1.ensure(success:true)

        then:
        o2.result.get('success') == true
        o2.processVertex != null
        o2.processVertex != o1.processVertex
    }


    void "do not rerun after success"() {
        when:
        def r1 = new MyReasoner(graph)
        def o1 = r1.ensure(success:true)

        then:
        o1.result.get('success') == true
        o1.processVertex != null

        when:
        def o2 = r1.ensure(success:true)

        then:
        o2 == [:]
    }

}




