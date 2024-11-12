package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.VertexProperty



/**
 * gradle test --tests "carnival.graph.PropertyDefinitionSpec"
 *
 */
class PropertyDefinitionSpec extends Specification {

    @PropertyModel
    static enum PX {
        PROP_A,
        PROP_B,
        PROP_C(
            cardinality: PropertyDefinition.Cardinality.LIST
        )
    }

    @VertexModel
    static enum VX {
        THING_ONE,

        THING_TWO(
            vertexProperties:[
                PX.PROP_A
            ]
        ),

        THING_THREE(
            vertexProperties:[
                PX.PROP_A,
                PX.PROP_B
            ]
        )
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

    def "cardinality"() {
        expect:
        PX.PROP_A.cardinality == PropertyDefinition.Cardinality.SINGLE
        PX.PROP_B.cardinality == PropertyDefinition.Cardinality.SINGLE
        PX.PROP_C.cardinality == PropertyDefinition.Cardinality.LIST
    }


    def "label format"() {
        expect:
        PX.PROP_A.label == 'PropA0CarnivalGraphPropertydefinitionspecPx'

        when:
        Vertex v1 = VX.THING_TWO.instance().withProperties(
            PX.PROP_A, 'prop-a-val-1',
        ).create(graph)

        List<VertexProperty> props = v1.properties().toList()
        //println "props: ${props}"
        VertexProperty theProp = props.find({ 
            it.label() ==  'PropA0CarnivalGraphPropertydefinitionspecPx'
        })

        then:
        theProp
    }


    def "base properties are defined"() {
        when:
        def v1 = VX.THING_TWO.instance().create(graph)
        pm.valueOf(v1)

        then:
        noExceptionThrown()

        where:
        pm << EnumSet.allOf(Base.PX)
    }
    

    def "valueOf throws an exception for undefined property"() {
        when:
        def v1 = VX.THING_TWO.instance().create(graph)
        PX.PROP_B.valueOf(v1)

        then:
        Exception e = thrown()
    }


    def "valueOf returns null if property not present"() {
        when:
        def v1 = VX.THING_TWO.instance().create(graph)

        then:
        !PX.PROP_A.of(v1).isPresent()
        PX.PROP_A.valueOf(v1) == null
    }


    def "valueOf returns property value if present"() {
        when:
        def v1 = VX.THING_TWO.instance().withProperty(PX.PROP_A, 'a').create(graph)

        then:
        PX.PROP_A.valueOf(v1) == 'a'
    }


    def "set closure result"() {
        when:
        def v1 = VX.THING_TWO.instance().create(graph)

        then:
        !PX.PROP_A.of(v1).isPresent()

        when:
        PX.PROP_A.set(v1) {1+1}

        then:
        PX.PROP_A.of(v1).isPresent()
        PX.PROP_A.valueOf(v1) == 2

        when:
        PX.PROP_A.set(v1) { (1==1) }

        then:
        PX.PROP_A.of(v1).isPresent()
        PX.PROP_A.valueOf(v1) == true

        when:
        PX.PROP_A.set(v1) { (1!=1) }

        then:
        PX.PROP_A.of(v1).isPresent()
        PX.PROP_A.valueOf(v1) == false
    }


    def "setIf closure result"() {
        when:
        def v1 = VX.THING_TWO.instance().create(graph)

        then:
        !PX.PROP_A.of(v1).isPresent()

        when:
        PX.PROP_A.setIf(v1) {
            null
        }

        then:
        !PX.PROP_A.of(v1).isPresent()

        when:
        PX.PROP_A.setIf(v1) {
            'a'
        }

        then:
        PX.PROP_A.of(v1).isPresent()
        PX.PROP_A.valueOf(v1) == 'a'
    }


    def "set() respects defined properties"() {
        Exception e

        when:
        def v1 = VX.THING_ONE.instance().create(graph)
        PX.PROP_A.set(v1, 'a')

        then:
        e = thrown()
        e instanceof IllegalArgumentException

        when:
        def v2 = VX.THING_TWO.instance().create(graph)
        PX.PROP_A.set(v2, 'a')

        then:
        noExceptionThrown()

        when:
        PX.PROP_B.set(v2, 'b')

        then:
        e = thrown()
        e instanceof IllegalArgumentException
    }


    def "defaultValue is not global"() {
        when:
        PX px1 = PX.PROP_A

        then:
        PX.PROP_A.defaultValue == null
        px1.defaultValue == null

        when:
        def px2 = PX.PROP_A.defaultValue('a')

        then:
        PX.PROP_A.defaultValue == null
        px1.defaultValue == null
        px2.defaultValue == 'a'
    }


    def "constraints is not global"() {
        when:
        PX px1 = PX.PROP_A

        then:
        !PX.PROP_A.required
        !px1.required

        when:
        def px2 = PX.PROP_A.withConstraints(required:true)

        then:
        !PX.PROP_A.required
        !px1.required
        px2.required
    }


    def "withConstraints required"() {
        when:
        def p1 = PX.PROP_A.withConstraints(required:true)

        then:
        p1 != null
        p1.required
        !p1.unique
        !p1.index
    }

}

