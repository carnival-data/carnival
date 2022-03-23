package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.VertexDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.EdgeDefinition
import carnival.graph.Base



/**
 *
 *
 */
class TinkerpopAnonExtensionsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    @VertexDefinition
    static enum VX {
        THING(
            vertexProperties:[PX.ID]
        )
    }

    @VertexDefinition
    static enum VX2 {
        THING(
            vertexProperties:[PX.ID]
        )
    }

    @EdgeDefinition
    static enum EX {
        IS_NOT
    }

    @EdgeDefinition
    static enum EX2{
        IS_NOT
    }

    @PropertyDefinition
    static enum PX {
        ID
    }

    @VertexDefinition
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

        [VX3.CLASS_OF_ALL_DOGS, VX3.COLLIE_CLASS, VX3.SHIBA_INU_CLASS].each {
            it.applyTo(graph, g)
        }
    } 


    def cleanup() {
        if (g) g.close()
        if (graph) graph.close()
    }




    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////


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





