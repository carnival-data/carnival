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

import carnival.graph.VertexModel
import carnival.graph.PropertyModel
import carnival.graph.EdgeModel
import carnival.graph.Base



/**
 *
 *
 */
class TinkerpopAnonExtensionsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    @VertexModel
    static enum VX {
        THING(
            vertexProperties:[PX.ID]
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
        ID
    }

    /*@VertexModel
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
            instanceOf: COLLIE_CLASS,
            vertexProperties:[PX3.ID, PX3.DATE_OF_BIRTH]
        )
    }*/

    @PropertyModel
    static enum PX3 {
        ID,
        DATE_OF_BIRTH
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

    /*def "anonymous traversal date property"() {
        when:
        Date d1 = new Date()
        def v1 = VX3.COLLIE.instance().withProperties(
            PX3.ID, '1',
            PX3.DATE_OF_BIRTH, d1
        ).ensure(graph, g)

        Calendar cal = Calendar.getInstance();
        cal.setTime(d1);
        cal.add(Calendar.DAY_OF_MONTH, -1);

        Date d2 = cal.getTime();
        def v2 = VX3.COLLIE.instance().withProperties(
            PX3.ID, '2',
            PX3.DATE_OF_BIRTH, d2
        ).ensure(graph, g)

        then:
        d1 != d2
        d1.compareTo(d2) != 0

        when:
        def collie1 = g.V()
            .isa(VX3.COLLIE)
            .and(
                __.has(PX3.ID, '1'),
                __.has(PX3.DATE_OF_BIRTH, d1)
            )
        .toList()

        then:
        collie1
        collie1.size() == 1
        collie1[0] == v1
    }*/


    def "anonymous traversal isa"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').ensure(graph, g)
        def v3 = VX.THING.instance().withProperty(PX.ID, '60').ensure(graph, g)
        EX.IS_NOT.instance().from(v1).to(v2).create()
        EX.IS_NOT.instance().from(v3).to(v2).create()
        println "$v1 $v2 $v3"        
        def op = g.V(v1).repeat(__.both()).until(__.isa(VX.THING)).tryNext()

        then:
        op.isPresent()
    }



    def "anonymous traversal out"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.ID, '58').ensure(graph, g)
        def v2 = VX.THING.instance().withProperty(PX.ID, '59').ensure(graph, g)
        def v3 = VX.THING.instance().withProperty(PX.ID, '60').ensure(graph, g)
        EX.IS_NOT.instance().from(v1).to(v2).create()
        EX.IS_NOT.instance().from(v3).to(v2).create()
        println "$v1 $v2 $v3"        
        def op = g.V(v1,v3).group().by(__.out(EX.IS_NOT)).tryNext()

        then:
        op.isPresent()

        when:
        def groups = op.get()
        groups.each { m -> println "$m" }

        then:
        groups.size() == 1
        groups.get(v2).size() == 2
        groups.get(v2).contains(v1)
        groups.get(v2).contains(v3)
    }

}





