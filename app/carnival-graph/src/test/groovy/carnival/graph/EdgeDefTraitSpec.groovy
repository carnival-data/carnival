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
 * gradle test --tests "carnival.graph.EdgeDefTraitSpec"
 *
 */
class EdgeDefTraitSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // GRAPH DEFS
    ///////////////////////////////////////////////////////////////////////////

    static final VertexDefTrait DYNAMIC_THING = new DynamicVertexDef('DYNAMIC_THING')

    static enum VX implements VertexDefTrait {
        EDTS_THING,
        EDTS_ANOTHER_THING,

    }

    static enum EX1 implements EdgeDefTrait {
        EDTS_RELATION
    }

    static enum EX2 implements EdgeDefTrait {
        EDTS_RELATION(
            domain: [VX.EDTS_THING], 
            range: [VX.EDTS_ANOTHER_THING]
        )

        private EX2() {}
        private EX2(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum EX3 implements EdgeDefTrait {
        EDTS_RELATION(
            domain: [DYNAMIC_THING], 
            range: [VX.EDTS_ANOTHER_THING]
        )

        private EX3() {}
        private EX3(Map m) {m.each { k,v -> this."$k" = v } }
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

    def "vertex vertex enforce domain dynamic"() {
        given:
        def v1
        def v2
        def e
        Throwable t

        when:
        v1 = DYNAMIC_THING.controlledInstance().vertex(graph, g)
        v2 = VX.EDTS_ANOTHER_THING.controlledInstance().vertex(graph, g)
        e = EX3.EDTS_RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX3.EDTS_RELATION.setRelationship(g, v2, v2)

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
        v1 = VX.EDTS_THING.controlledInstance().vertex(graph, g)
        v2 = VX.EDTS_ANOTHER_THING.controlledInstance().vertex(graph, g)
        e = EX2.EDTS_RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX2.EDTS_RELATION.setRelationship(g, v1, v1)

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
        v1 = VX.EDTS_THING.controlledInstance().vertex(graph, g)
        v2 = VX.EDTS_ANOTHER_THING.controlledInstance().vertex(graph, g)
        e = EX2.EDTS_RELATION.setRelationship(g, v1, v2)

        println "v1: ${v1} ${v1.label()} ${v1.value('nameSpace')}"
        println "v2: ${v2} ${v2.label()} ${v2.value('nameSpace')}"

        then:
        noExceptionThrown()

        when:
        e = EX2.EDTS_RELATION.setRelationship(g, v2, v2)

        then:
        t = thrown()
    }



    def "name spaces"() {
        given:
        def v1
        def v2
        def e

        when:
        v1 = VX.EDTS_THING.controlledInstance().vertex(graph, g)
        v2 = VX.EDTS_THING.controlledInstance().vertex(graph, g)
        e = EX1.EDTS_RELATION.setRelationship(g, v1, v2)

        then:
        e
        e.property(Base.PX.NAME_SPACE.label).isPresent()
        e.value(Base.PX.NAME_SPACE.label) == 'carnival.graph.EdgeDefTraitSpec$EX1'
    }

}

