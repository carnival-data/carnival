package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
//import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of



/**
 * gradle test --tests "carnival.graph.VertexDefTraitSpec"
 *
 */
class VertexDefTraitSpec extends Specification {

    static enum VX implements VertexDefTrait {
        VDTS_THING,

        VDTS_THING_1(
            vertexProperties:[
                PX.VDTS_PROP_A.withConstraints(required:true)
            ]
        ),

        VDTS_THING_2(
            vertexProperties:[
                PX.VDTS_PROP_A
            ]
        ),
        
        VDTS_THING_3(
            vertexProperties:[
                PX.VDTS_PROP_A.defaultValue(1).withConstraints(required:true)
            ]
        ),

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v }}
    }


    static enum PX implements PropertyDefTrait {
        VDTS_PROP_A,
        VDTS_PROP_B,

        public PX() {}
        public PX(Map m) {m.each { k,v -> this."$k" = v }}
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


    def "property def constraints"() {

        expect:
        !PX.VDTS_PROP_A.hasProperty('required')
        VX.VDTS_THING_1.vertexProperties[0].hasProperty('required')
        !VX.VDTS_THING_2.vertexProperties[0].hasProperty('required')
        VX.VDTS_THING_3.vertexProperties[0].hasProperty('required')

    }


    def "controlled instance vertex enum"() {
        given:
        def v

        when:
        v = VX.VDTS_THING.controlledInstance().vertex(graph, g)

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
        vDef = new DynamicVertexDef('VDTS_THING_2')

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
        VX.VDTS_THING.getNameSpace() == 'carnival.graph.VertexDefTraitSpec$VX'

        when:
        v = VX.VDTS_THING.instance().vertex(graph, g)

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
        vDef = new DynamicVertexDef('VDTS_THING_2')

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

