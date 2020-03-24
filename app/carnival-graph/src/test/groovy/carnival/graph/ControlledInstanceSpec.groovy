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
 * gradle test --tests "carnival.graph.ControlledInstanceSpec"
 *
 */
class ControlledInstanceSpec extends Specification {

    static enum VX implements VertexDefTrait {
        CIS_THING(
            vertexProperties:[
                PX.CIS_PROP_A.withConstraints(required:true), 
                PX.CIS_PROP_B.defaultValue(1).withConstraints(required:true)
            ]
        )

        public VX() {}
        public VX(Map m) {m.each { k,v -> this."$k" = v }}
    }

    static enum PX implements PropertyDefTrait {
        CIS_PROP_A,
        CIS_PROP_B

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

    def "try next"() {
        expect:
        !VX.CIS_THING.instance().withProperties(
            PX.CIS_PROP_A, 'a',
            PX.CIS_PROP_B, 'b'
        ).traversal(graph, g).tryNext().isPresent()
    }


    def "default property values"() {
        given:
        def v
        def ci
        Exception t


        when:
        def vpDefs = VX.CIS_THING.vertexProperties
        
        then:
        vpDefs.size() == 2
        vpDefs[0].hasProperty('required')
        vpDefs[1].hasProperty('required')
        vpDefs[0] instanceof ConstrainedPropertyDefTrait
        vpDefs[1] instanceof ConstrainedPropertyDefTrait


        when:
        ci = VX.CIS_THING.instance()
        v = ci.vertex(graph, g)

        then:
        t = thrown()


        when:
        ci = VX.CIS_THING.instance().withProperty(PX.CIS_PROP_A, 'a')
        v = ci.vertex(graph, g)
        println "CIS_PROP_A: ${PX.CIS_PROP_A.of(v)}"
        println "CIS_PROP_B: ${PX.CIS_PROP_B.of(v)}"

        then:
        v != null
        PX.CIS_PROP_A.valueOf(v) == 'a'
        PX.CIS_PROP_B.valueOf(v) == 1


        when:
        ci = VX.CIS_THING.instance().withProperties(
            PX.CIS_PROP_A, 'a',
            PX.CIS_PROP_B, 2
        )
        v = ci.vertex(graph, g)
        println "CIS_PROP_A: ${PX.CIS_PROP_A.of(v)}"
        println "CIS_PROP_B: ${PX.CIS_PROP_B.of(v)}"

        then:
        v != null
        PX.CIS_PROP_A.valueOf(v) == 'a'
        PX.CIS_PROP_B.valueOf(v) == 2
    }



}

