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

        A_CLASS,
        B_CLASS (
            superClass: VX.A_CLASS
        ),
        B (
            instanceOf: VX.B_CLASS
        ),

        CLASS_OF_SOMETHING (
            isClass: true
        ),

        NOT_A_CLASS (
            isClass: false
        )

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

    def "instanceOf edge is automatically created"() {
        setup:
        // these have to be explicitly set as we are not using CoreGraph, just
        // creating a TinkerGraph and testing VertexDefTrait in isolation
        if (!VX.A_CLASS.vertex) VX.A_CLASS.vertex = VX.A_CLASS.instance().create(graph)
        if (!VX.B_CLASS.vertex) VX.B_CLASS.vertex = VX.B_CLASS.instance().create(graph)

        when:
        def b = VX.B.instance().create(graph)

        then:
        b != null
        g.V(b)
            .out(Base.EX.IS_INSTANCE_OF)
            .is(VX.B_CLASS.vertex)
        .tryNext().isPresent()
    }


    class Something implements VertexDefTrait {
        String name
        String name() {
            this.name
        }
    }

    def "cannot set superclass unless is a class"() {
        when:
        def s = new Something(name:"a")

        then:
        !s.isClass()

        when:
        s.superClass = VX.THING

        then:
        Exception e = thrown()
    }


    def "cannot set instanceof of class"() {
        when:
        def s = new Something(name:"a_class")

        then:
        s.isClass()

        when:
        s.instanceOf = VX.THING

        then:
        Exception e = thrown()

    }


    def "explicit isClass"() {
        expect:
        VX.CLASS_OF_SOMETHING.isClass()
        !VX.NOT_A_CLASS.isClass()
        VX.A_CLASS.isClass
        VX.A_CLASS.isClass()
    }


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

