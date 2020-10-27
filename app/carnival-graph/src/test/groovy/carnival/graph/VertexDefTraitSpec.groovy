package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.VertexProperty
//import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of



/**
 * gradle test --tests "carnival.graph.VertexDefTraitSpec"
 *
 */
class VertexDefTraitSpec extends Specification {

    static enum VX implements VertexDefTrait {
        THING,

        THING_1(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true)
            ]
        ),

        THING_2(
            vertexProperties:[
                PX.PROP_A
            ]
        ),
        
        THING_3(
            vertexProperties:[
                PX.PROP_A.defaultValue(1).withConstraints(required:true)
            ]
        ),

        THING_4(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        ),

        THING_5(propertiesMustBeDefined:false),

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v }}
    }


    static enum PX implements PropertyDefTrait {
        PROP_A,
        PROP_B
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

    def "can add undefined props on switch"() {
        when:
        def v1 = VX.THING_5.instance().withProperty(PX.PROP_A, 'a').create(graph)

        then:
        noExceptionThrown()
    }


    def "properties must be defined by default"() {
        when:
        def v1 = VX.THING.instance().withProperty(PX.PROP_A, 'a').create(graph)

        then:
        Exception e = thrown()
        e instanceof IllegalArgumentException
        //e.printStackTrace()
    }


    def "two defined properties"() {
        when:
        def v1 = VX.THING_4.instance().withProperties(
            PX.PROP_A, 'a',
            PX.PROP_B, 'b'
        ).create(graph)
        v1.property('someOtherProp', 'qq')
        def dps1 = VX.THING_4.definedPropertiesOf(v1)
        println "dps1: $dps1"

        then:
        dps1 != null
        dps1.size() == 2
        dps1.find { it.label == PX.PROP_A.label }
        dps1.find { it.label == PX.PROP_B.label }
        
        when:
        def dp1 = dps1.find { it.label == PX.PROP_A.label }

        then:
        dp1.label() == PX.PROP_A.label
        dp1.value() == 'a'

        when:
        def dp2 = dps1.find { it.label == PX.PROP_B.label }

        then:
        dp2.label() == PX.PROP_B.label
        dp2.value() == 'b'
    }


    def "one defined property"() {
        when:
        def v1 = VX.THING_4.instance().withProperty(PX.PROP_A, 'a').create(graph)
        v1.property('someOtherProp', 'qq')
        def dps1 = VX.THING_4.definedPropertiesOf(v1)
        println "dps1: $dps1"

        then:
        dps1 != null
        dps1.size() == 1
        
        when:
        def dp1 = dps1.first()

        then:
        dp1 instanceof VertexProperty
        dp1.label() == PX.PROP_A.label
        dp1.value() == 'a'
    }


    def "property def constraints"() {

        expect:
        !PX.PROP_A.hasProperty('required')
        VX.THING_1.vertexProperties[0].hasProperty('required')
        !VX.THING_2.vertexProperties[0].hasProperty('required')
        VX.THING_3.vertexProperties[0].hasProperty('required')

    }


    def "controlled instance vertex enum"() {
        given:
        def v

        when:
        v = VX.THING.controlledInstance().vertex(graph, g)

        then:
        v
        !v.property(Base.PX.IS_CLASS.label).isPresent()
        v.property(Base.PX.NAME_SPACE.label).isPresent()
        v.value(Base.PX.NAME_SPACE.label) == 'carnival.graph.VertexDefTraitSpec$VX'
    }


    def "controlled instance vertex object"() {
        given:
        def v
        def vDef

        when:
        vDef = new DynamicVertexDef('THING_2')

        then:
        vDef instanceof DynamicVertexDef
        vDef.getNameSpace() == 'carnival.graph.DynamicVertexDef'

        when:
        vDef.nameSpace = 'some.custom.NameSpace'

        then:
        vDef.nameSpace == 'some.custom.NameSpace'

        when:
        v = vDef.controlledInstance().vertex(graph, g)

        then:
        v
        !v.property(Base.PX.IS_CLASS.label).isPresent()
        v.property(Base.PX.NAME_SPACE.label).isPresent()
        v.value(Base.PX.NAME_SPACE.label) == 'some.custom.NameSpace'
    }


    def "createVertex enum"() {
        given:
        def v

        expect:
        VX.THING.getNameSpace() == 'carnival.graph.VertexDefTraitSpec$VX'

        when:
        v = VX.THING.instance().vertex(graph, g)

        then:
        v
        !v.property(Base.PX.IS_CLASS.label).isPresent()
        v.property(Base.PX.NAME_SPACE.label).isPresent()
        v.value(Base.PX.NAME_SPACE.label) == 'carnival.graph.VertexDefTraitSpec$VX'
    }



    def "createVertex object"() {
        given:
        def v
        def vDef

        when:
        vDef = new DynamicVertexDef('THING_2')

        then:
        vDef instanceof DynamicVertexDef
        vDef.getNameSpace() == 'carnival.graph.DynamicVertexDef'

        when:
        vDef.nameSpace = 'some.custom.NameSpace'

        then:
        vDef.nameSpace == 'some.custom.NameSpace'

        when:
        v = vDef.instance().vertex(graph, g)

        then:
        v
        !v.property(Base.PX.IS_CLASS.label).isPresent()
        v.property(Base.PX.NAME_SPACE.label).isPresent()
        v.value(Base.PX.NAME_SPACE.label) == 'some.custom.NameSpace'
    }



}

