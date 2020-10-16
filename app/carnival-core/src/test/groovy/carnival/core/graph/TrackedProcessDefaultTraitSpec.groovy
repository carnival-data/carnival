package carnival.core.graph



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
 * gradle test --tests "carnival.core.graph.TrackedProcessDefaultTraitSpec"
 *
 *
 */
class TrackedProcessDefaultTraitSpec extends Specification {

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

    static class MyTrackable implements GremlinTrait, TrackedProcessDefaultTrait {
        public MyTrackable(Graph graph) { this.graph = graph }
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    void "basics"() {
        when:
        def t1 = new MyTrackable(graph)
        def v1 = t1.createAndSetTrackedProcessVertex(graph)

        then:
        v1 != null
        v1.label() == "MyTrackableProcess"
        t1.getAllTrackedProcesses(g).size() == 1
        t1.getAllSuccessfulTrackedProcesses(g).size() == 0

        when:
        def t2 = new MyTrackable(graph)
        def v2 = t2.createAndSetTrackedProcessVertex(graph)
        Core.PX.SUCCESS.set(v2, true)

        then:
        v2 != null
        v2.label() == "MyTrackableProcess"
        v2 != v1
        t1.getAllTrackedProcesses(g).size() == 2
        t1.getAllSuccessfulTrackedProcesses(g).size() == 1
    }

}




