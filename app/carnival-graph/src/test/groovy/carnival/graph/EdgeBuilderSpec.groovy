package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Edge



/**
 * gradle test --tests "carnival.graph.EdgeBuilderSpec"
 *
 */
class EdgeBuilderSpec extends Specification {

    @VertexDefinition
    static enum VX {
        EBS_THING_1,
        EBS_THING_2
    }

    @PropertyDefinition
    static enum PX {
        EBS_PROP_A,
        EBS_PROP_B
    }

    @EdgeDefinition
    static enum EX {
        EBS_REL_1(
            domain:[VX.EBS_THING_1],
            range:[VX.EBS_THING_2]
        ),
        EBS_REL_2(
            propertyDefs:[
                PX.EBS_PROP_A.withConstraints(required:true), 
                PX.EBS_PROP_B.defaultValue(1).withConstraints(required:true)
            ],
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

    def "properties simple"() {
        given:
        Exception e
        def v1 = VX.EBS_THING_1.createVertex(graph)
        def v2 = VX.EBS_THING_2.createVertex(graph)

        when:
        def e1 = EX.EBS_REL_2.instance()
            .from(v1)
            .to(v2)
        .create()

        then:
        e = thrown()
        e instanceof RequiredPropertyException

        when:
        def e2 = EX.EBS_REL_2.instance()
            .withProperty(PX.EBS_PROP_A, 'a')
            .from(v1)
            .to(v2)
        .create()

        then:
        noExceptionThrown()
        e2 instanceof Edge
        PX.EBS_PROP_A.valueOf(e2) == 'a'
    }


    def "domain check"() {
        given:
        Exception e
        def eb
        def v1 = VX.EBS_THING_1.createVertex(graph)
        def v2 = VX.EBS_THING_2.createVertex(graph)

        when:
        eb = EX.EBS_REL_1.instance().from(v2)

        then:
        e = thrown()
        e instanceof EdgeDomainException

        when:
        eb = EX.EBS_REL_1.instance().from(v1)

        then:
        noExceptionThrown()
    }


    def "range check"() {
        given:
        Exception e
        def eb
        def v1 = VX.EBS_THING_1.createVertex(graph)
        def v2 = VX.EBS_THING_2.createVertex(graph)

        when:
        eb = EX.EBS_REL_1.instance().to(v1)

        then:
        e = thrown()
        e instanceof EdgeRangeException

        when:
        eb = EX.EBS_REL_1.instance().to(v2)

        then:
        noExceptionThrown()
    }


    def "create simple"() {
        given:
        Exception e
        def v1 = VX.EBS_THING_1.createVertex(graph)
        def v2 = VX.EBS_THING_2.createVertex(graph)

        when:
        def eb1 = EX.EBS_REL_1.instance().from(v1).to(v2).create()
        def eb2 = EX.EBS_REL_1.instance().from(v1).to(v2).create()

        then:
        noExceptionThrown()
        eb1
        eb2
        eb1 != eb2
        !eb1.equals(eb2) 
    }


    def "edge simple"() {
        given:
        Exception e
        def v1 = VX.EBS_THING_1.createVertex(graph)
        def v2 = VX.EBS_THING_2.createVertex(graph)

        when:
        def eb1 = EX.EBS_REL_1.instance().from(v1).to(v2).ensure(g)
        def eb2 = EX.EBS_REL_1.instance().from(v1).to(v2).ensure(g)

        then:
        noExceptionThrown()
        eb1
        eb2
        eb1 == eb2
        eb1.equals(eb2) 
    }


}

