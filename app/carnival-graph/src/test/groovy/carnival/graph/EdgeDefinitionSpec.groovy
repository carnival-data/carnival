package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Property



/**
 * gradle test --tests "carnival.graph.EdgeDefinitionSpec"
 *
 */
class EdgeDefinitionSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // GRAPH DEFS
    ///////////////////////////////////////////////////////////////////////////

    static final VertexDefinition DYNAMIC_THING = new DynamicVertexDef('DYNAMIC_THING')

    static enum VX implements VertexDefinition {
        THING,
        ANOTHER_THING
    }

    static enum VX2 implements VertexDefinition {
        THING (global:true),
        ANOTHER_THING (global:true)

        private VX2() {}
        private VX2(Map m) { m.each { k,v -> this."$k" = v } }
    }

    static enum EX1 implements EdgeDefinition {
        RELATION(            
            propertyDefs:[
                PX.PROP_A.withConstraints(required:true)
            ]
        ),
        RELATION_2(            
            propertyDefs:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        )
        private EX1() {}
        private EX1(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum EX2 implements EdgeDefinition {
        RELATION(
            propertyDefs:[
                PX.PROP_A
            ],   
            domain: [VX.THING], 
            range: [VX.ANOTHER_THING]
        )

        private EX2() {}
        private EX2(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum EX3 implements EdgeDefinition {
        RELATION(
            propertyDefs:[
                PX.PROP_A.defaultValue(1).withConstraints(required:true)
            ],
            domain: [DYNAMIC_THING], 
            range: [VX.ANOTHER_THING]
        )

        private EX3() {}
        private EX3(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum PX implements PropertyDefinition {
        PROP_A,
        PROP_B,
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

    def "two defined properties"() {
        when:
        def v1 = VX.THING.instance().create(graph)
        def v2 = VX.THING.instance().create(graph)
        def e1 = EX1.RELATION_2.instance().from(v1).to(v2).withProperties(
            PX.PROP_A, 'a',
            PX.PROP_B, 'b'
        ).create()
        e1.property('someOtherProp', 'qq')
        def dps1 = EX1.RELATION_2.definedPropertiesOf(e1)
        println "dps1: $dps1"

        then:
        dps1 != null
        dps1.size() == 2
        dps1.find { it.key() == PX.PROP_A.label }
        dps1.find { it.key() == PX.PROP_B.label }
        
        when:
        def dp1 = dps1.find { it.key() == PX.PROP_A.label }

        then:
        dp1 instanceof Property
        dp1.key() == PX.PROP_A.label
        dp1.value() == 'a'

        when:
        def dp2 = dps1.find { it.key() == PX.PROP_B.label }

        then:
        dp2 instanceof Property
        dp2.key() == PX.PROP_B.label
        dp2.value() == 'b'
    }


    def "one defined property"() {
        when:
        def v1 = VX.THING.instance().create(graph)
        def v2 = VX.THING.instance().create(graph)
        def e1 = EX1.RELATION_2.instance().from(v1).to(v2).withProperty(PX.PROP_A, 'a').create()
        e1.property('someOtherProp', 'qq')
        def dps1 = EX1.RELATION_2.definedPropertiesOf(e1)
        println "dps1: $dps1"

        then:
        dps1 != null
        dps1.size() == 1
        
        when:
        def dp1 = dps1.first()

        then:
        dp1 instanceof Property
        dp1.key() == PX.PROP_A.label
        dp1.value() == 'a'
    }


    def "vertex vertex enforce range with global"() {
        given:
        def v1
        def v2
        def e
        Throwable t

        when:
        v1 = VX.THING.instance().vertex(graph, g)
        v2 = VX2.ANOTHER_THING.instance().vertex(graph, g)
        e = EX2.RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX2.RELATION.setRelationship(g, v1, v1)

        then:
        t = thrown()
    }


    def "vertex vertex enforce domain with global"() {
        given:
        def v1
        def v2
        def e
        Throwable t

        when:
        v1 = VX2.THING.instance().vertex(graph, g)
        v2 = VX.ANOTHER_THING.instance().vertex(graph, g)
        e = EX2.RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX2.RELATION.setRelationship(g, v2, v2)

        then:
        t = thrown()
    }


    def "vertex vertex enforce domain dynamic"() {
        given:
        def v1
        def v2
        def e
        Throwable t

        when:
        v1 = DYNAMIC_THING.instance().vertex(graph, g)
        v2 = VX.ANOTHER_THING.instance().vertex(graph, g)
        e = EX3.RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX3.RELATION.setRelationship(g, v2, v2)

        then:
        t = thrown()
    }


    def "vertex vertex enforce range"() {
        given:
        def v1
        def v2
        def e
        Throwable t

        when:
        v1 = VX.THING.instance().vertex(graph, g)
        v2 = VX.ANOTHER_THING.instance().vertex(graph, g)
        e = EX2.RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX2.RELATION.setRelationship(g, v1, v1)

        then:
        t = thrown()
    }


    def "vertex vertex enforce domain"() {
        given:
        def v1
        def v2
        def e
        Throwable t

        when:
        v1 = VX.THING.instance().vertex(graph, g)
        v2 = VX.ANOTHER_THING.instance().vertex(graph, g)
        e = EX2.RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX2.RELATION.setRelationship(g, v2, v2)

        then:
        t = thrown()
    }



    def "name spaces"() {
        given:
        def v1
        def v2
        def e

        when:
        v1 = VX.THING.instance().vertex(graph, g)
        v2 = VX.THING.instance().vertex(graph, g)
        e = EX1.RELATION.setRelationship(g, v1, v2)

        then:
        e
        e.property(Base.PX.NAME_SPACE.label).isPresent()
        e.value(Base.PX.NAME_SPACE.label) == 'carnival.graph.EdgeDefinitionSpec$EX1'
    }

}

