package carnival.graph.ext



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.process.traversal.Order

import carnival.graph.VertexModel
import carnival.graph.PropertyModel
import carnival.graph.EdgeModel
import carnival.graph.Base



/**
 * gradle -Dtest.single=TinkerpopExtensionsSpec test
 *
 *
 */
class TinkerpopExtensionsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    @VertexModel
    static enum VX {
        THING(
            vertexProperties:[PX.ID, PX.NAME, PX.CREATED]
        )
    }

    @VertexModel
    static enum VX2 {
        THING(
            vertexProperties:[PX.ID]
        )
    }

    @EdgeModel
    static enum EX {
        IS_NOT
    }

    @EdgeModel
    static enum EX2{
        IS_NOT
    }

    @PropertyModel
    static enum PX {
        ID,
        NAME,
        CREATED
    }

    @VertexModel
    static enum VX3 {
        CLASS_OF_ALL_DOGS (
            isClass:true
        ),
        
        COLLIE_CLASS (
            superClass: CLASS_OF_ALL_DOGS
        ),

        SHIBA_INU_CLASS (
            superClass: CLASS_OF_ALL_DOGS
        ),

        SHIBA_INU (
            instanceOf: SHIBA_INU_CLASS
        ),

        COLLIE (
            instanceOf: COLLIE_CLASS
        )
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
    
    def setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()

        /*[VX3.CLASS_OF_ALL_DOGS, VX3.COLLIE_CLASS, VX3.SHIBA_INU_CLASS].each {
            it.applyTo(graph, g)
        }*/
    } 


    def cleanup() {
        if (g) g.close()
        if (graph) graph.close()
    }




    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    /*def "get class of an instance vertex"() {
        when:
        def collieV = VX3.COLLIE.instance().create(graph)

        def classVs = g.V(collieV).instanceClass().toList()

        then:
        classVs != null
        classVs.size() == 1
        classVs.contains(VX3.COLLIE_CLASS.vertex)
    }*/


    /*def "get classes of an instance vertex"() {
        when:
        def collieV = VX3.COLLIE.instance().create(graph)

        def classVs = g.V(collieV).classes().toList()

        then:
        classVs != null
        classVs.size() == 2
        classVs.contains(VX3.COLLIE_CLASS.vertex)
        classVs.contains(VX3.CLASS_OF_ALL_DOGS.vertex)
    }*/


    /*def "vertices that are instancef of a class"() {
        when:
        def collieV = VX3.COLLIE.instance().create(graph)
        def shibaV = VX3.SHIBA_INU.instance().create(graph)

        def dogVs = g.V().isInstanceOf(VX3.CLASS_OF_ALL_DOGS).toList()

        then:
        dogVs != null
        dogVs.size() == 2
        dogVs.contains(collieV)
        dogVs.contains(shibaV)
    }*/


    /*def "get all instances from class"() {
        when:
        def collieV = VX3.COLLIE.instance().create(graph)
        def shibaV = VX3.SHIBA_INU.instance().create(graph)

        def dogVs = g.V(VX3.CLASS_OF_ALL_DOGS.vertex).instances().toList()

        then:
        dogVs != null
        dogVs.size() == 2
        dogVs.contains(collieV)
        dogVs.contains(shibaV)
    }*/


    // things must have changed with new version of tinkerpop.  this does
    // not cause an exception.  not an issue with carival.
    /*
    def "all must be groupable"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').ensure(graph, g)
        def v3 = VX.THING.instance().withProperty(PX.ID, '60').ensure(graph, g)
        EX.IS_NOT.instance().from(v1).to(v2).create()
        EX.IS_NOT.instance().from(v3).to(v2).create()
        println "$v1 $v2 $v3"        
        def op = g.V()
            .isa(VX.THING)
            .group().by(
                __.out(EX.IS_NOT)
            )
        .tryNext()
        if (op.isPresent()) println "op: ${op.get()}"

        then:
        Exception e = thrown()
        e instanceof java.lang.IllegalArgumentException
    }
    */


    def "match on differing properties"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').create(graph)
        def v3 = VX.THING.instance().withProperty(PX.NAME, '58').create(graph)
        def v4 = VX.THING.instance().create(graph)

        def matches = g.V().matchesOn(PX.NAME, v1, PX.ID).toList()

        then:
        matches.size() == 1
        matches.contains(v3)
    }


    def "match on same property"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').create(graph)
        def v3 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
        def v4 = VX.THING.instance().create(graph)

        def matches = g.V().matchesOn(PX.ID, v1).toList()

        then:
        matches.size() == 2
        matches.contains(v1)
        matches.contains(v3)
    }


    def "order by default order"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
        def v2 = VX.THING.instance().withProperty(PX.ID, '57').create(graph)
        def v3 = VX.THING.instance().withProperty(PX.ID, '59').create(graph)

        def orderedVs = g.V().isa(VX.THING).order().by(PX.ID).toList()

        then:
        orderedVs.size() == 3
        orderedVs == [v2, v1, v3]
    }


    def "order by asc"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
        def v2 = VX.THING.instance().withProperty(PX.ID, '57').create(graph)
        def v3 = VX.THING.instance().withProperty(PX.ID, '59').create(graph)

        def orderedVs = g.V().isa(VX.THING).order().by(PX.ID, Order.asc).toList()

        then:
        orderedVs.size() == 3
        orderedVs == [v2, v1, v3]
    }


    def "order by desc"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
        def v2 = VX.THING.instance().withProperty(PX.ID, '57').create(graph)
        def v3 = VX.THING.instance().withProperty(PX.ID, '59').create(graph)

        def orderedVs = g.V().isa(VX.THING).order().by(PX.ID, Order.desc).toList()

        then:
        orderedVs.size() == 3
        orderedVs == [v3, v1, v2]
    }


    def "has pdef date value"() {
        expect:
        g.V().isa(VX.THING).count().next() == 0

        when:
        Date d = new Date()
        def v1 = VX.THING.instance().withProperty(PX.CREATED, d).create(graph)

        then:
        g.V().isa(VX.THING).count().next() == 1
        g.V(v1).has(PX.CREATED, d).tryNext().isPresent()

        when:
        Date e = new Date()
        e.setYear(d.year-1)

        then:
        e.compareTo(d) != 0
        !g.V(v1).has(PX.CREATED, e).tryNext().isPresent()
    }


    def "has pdef value"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, 'someval').create(graph)

        then:
        g.V(v1).has(PX.ID, 'someval').tryNext().isPresent()
    }


    def "has pdef enum"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, LOCAL_ID.ID1).create(graph)

        then:
        g.V(v1).has(PX.ID, LOCAL_ID.ID1).tryNext().isPresent()
    }


    def "hasNot pdef"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, LOCAL_ID.ID1).create(graph)
        def v2 = VX.THING.instance().create(graph)

        then:
        !g.V(v1).hasNot(PX.ID).tryNext().isPresent()
        g.V(v2).hasNot(PX.ID).tryNext().isPresent()
    }


    def "has pdef"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, LOCAL_ID.ID1).create(graph)

        then:
        g.V(v1).has(PX.ID).tryNext().isPresent()
    }


    def "both basic"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').ensure(graph, g)
        EX.IS_NOT.instance().from(v1).to(v2).create()

        then:
        g.V(v2).both(EX.IS_NOT).tryNext().isPresent()
        g.V(v1).both(EX.IS_NOT).tryNext().isPresent()
        g.V(v2).both(EX.IS_NOT).next() == v1
        g.V(v1).both(EX.IS_NOT).next() == v2
    }


    def "in basic"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').ensure(graph, g)
        EX.IS_NOT.instance().from(v1).to(v2).create()

        then:
        g.V(v2).in(EX.IS_NOT).tryNext().isPresent()
        g.V(v2).in(EX.IS_NOT).next() == v1
        !g.V(v1).in(EX.IS_NOT).tryNext().isPresent()
    }


    def "out basic"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').ensure(graph, g)
        EX.IS_NOT.instance().from(v1).to(v2).create()

        then:
        g.V(v1).out(EX.IS_NOT).tryNext().isPresent()
        g.V(v1).out(EX.IS_NOT).next() == v2
        !g.V(v2).out(EX.IS_NOT).tryNext().isPresent()
    }


    def "isa has basic"() {
        when:
        def vc = VX.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
        def vf = g.V().isa(VX.THING).has(PX.ID, '58').tryNext()

        then:
        vf.isPresent()
        vf.get() == vc

        when:
        def vc2 = VX2.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
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



    /*def "nextOne"() {
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
    }*/

}





