package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge



/**
 * gradle test --tests "carnival.graph.DefAnnotationsSpec"
 *
 */
class DefAnnotationsSpec extends Specification {

    @VertexModel
    static enum VX {
        THING,

        THING_1(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        )
    }


    @EdgeModel
    static enum EX {
    	IS_NOT(
            domain:[VX.THING], 
            range:[VX.THING_1]            
        )
    }


    @PropertyModel
    static enum PX {
        PROP_A,
        PROP_B,
        PROP_C
    }


    @PropertyModel
    static enum PX_REDUNDANT_DEF_TRAIT implements PropertyDefinition {
        PROP_A
    }



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared graph
    @Shared g
    


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() {
    } 

    def setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()
    }

    def cleanup() {
        if (g) g.close()
        if (graph) graph.close()
    }

    def cleanupSpec() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "edge domain"() {
        when:
        Vertex v1 = VX.THING_1.instance().withProperty(
            PX.PROP_A, 'a'
        ).create(graph)

        then:
        v1 != null
        PX.PROP_A.valueOf(v1) == 'a'
    }


    def "vertex props"() {
        when:
        Vertex v1 = VX.THING.instance().create(graph)
        Vertex v2 = VX.THING_1.instance().withProperty(
            PX.PROP_A, 'a'
        ).create(graph)
        Edge e1 = EX.IS_NOT.instance().from(v1).to(v2).create()

        then:
        e1 != null
    }

}

