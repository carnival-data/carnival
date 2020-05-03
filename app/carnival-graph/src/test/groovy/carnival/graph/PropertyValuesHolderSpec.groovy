package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource



/**
 * gradle test --tests "carnival.graph.PropertyValuesHolderSpec"
 *
 */
class PropertyValuesHolderSpec extends Specification {

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
    }

    def cleanup() {
    }

    def cleanupSpec() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "with non-null properties"() {
        when:
        def ph1 = VX.CIS_THING.instance().withNonNullProperties(
            PX.CIS_PROP_A, 'a',
            PX.CIS_PROP_B, null
        )

        then:
        ph1.propertyValues.size() == 1
    }


    def "with properties"() {
        when:
        def ph1 = VX.CIS_THING.instance().withProperties(
            PX.CIS_PROP_A, 'a',
            PX.CIS_PROP_B, 'b'
        )

        then:
        ph1.propertyValues.size() == 2
    }




}

