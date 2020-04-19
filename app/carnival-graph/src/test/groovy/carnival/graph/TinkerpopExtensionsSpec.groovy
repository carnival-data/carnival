package carnival.graph



import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.commons.io.FileUtils

import org.apache.tinkerpop.gremlin.structure.T

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource



/**
 * gradle -Dtest.single=TinkerpopExtensionsSpec test
 *
 *
 */
class TinkerpopExtensionsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////
    static enum VX implements VertexDefTrait {
        THING(
            vertexProperties:[PX.ID]
        )

        public VX() {}
        public VX(Map m) {m.each { k,v -> this."$k" = v }}
    }

    static enum VX2 implements VertexDefTrait {
        THING(
            vertexProperties:[PX.ID]
        )

        public VX2() {}
        public VX2(Map m) {m.each { k,v -> this."$k" = v }}
    }

    static enum EX implements EdgeDefTrait {
        IS_NOT
    }

    static enum EX2 implements EdgeDefTrait {
        IS_NOT
    }

    static enum PX implements PropertyDefTrait {
        ID
    }

    static enum LOCAL_ID { ID1 }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    @Shared graph
    @Shared g


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
        graph = TinkerGraph.open()
        g = graph.traversal()
    } 


    def cleanupSpec() {
        if (g) g.close()
        if (graph) graph.close()
    }




    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "has enum"() {
        when:
        def vc = VX.THING.instance().withProperty(PX.ID, LOCAL_ID.ID1).create(graph, g)
        def vf = g.V().isa(VX.THING).has(PX.ID, LOCAL_ID.ID1).tryNext()

        then:
        vf.isPresent()
        vf.get() == vc
    }


    def "out basic"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph, g)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').create(graph, g)
        EX.IS_NOT.instance().from(v1).to(v2).create(g)

        then:
        g.V(v1).out(EX.IS_NOT).tryNext().isPresent()
        g.V(v1).out(EX.IS_NOT).next() == v2
        !g.V(v1).out(EX2.IS_NOT).tryNext().isPresent()
    }


    def "isa has basic"() {
        when:
        def vc = VX.THING.instance().withProperty(PX.ID, '58').create(graph, g)
        def vf = g.V().isa(VX.THING).has(PX.ID, '58').tryNext()

        then:
        vf.isPresent()
        vf.get() == vc

        when:
        def vc2 = VX2.THING.instance().withProperty(PX.ID, '58').create(graph, g)
        def vf2 = g.V().isa(VX2.THING).has(PX.ID, '58').tryNext()

        then:
        vf2.isPresent()

        when:
        vf2 = vf2.get()

        then:
        vf2 == vc2
        vf2 != vf
        vf2 != vc
    }


    def "nextOne"() {
        given:
        def res
        Throwable e

        when:
        def nothing = g.V().hasLabel('something-that-does-not-exist').nextOne()

        then:
        nothing == null

        when:
        graph.addVertex(T.label, 'a-unique-thing')
        def single = g.V().hasLabel('a-unique-thing').nextOne()

        then:
        single != null

        when:
        graph.addVertex(T.label, 'a-unique-thing')
        def multi = g.V().hasLabel('a-unique-thing').nextOne()

        then:
        e = thrown()
        //e.printStackTrace()
        e instanceof RuntimeException
        e.message.startsWith('nextOne')
    }

}





