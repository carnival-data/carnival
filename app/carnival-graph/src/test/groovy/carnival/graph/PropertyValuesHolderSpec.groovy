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

        VX() {}
        VX(Map m) {m.each { k,v -> this."$k" = v }}
    }

    static enum PX implements PropertyDefTrait {
        CIS_PROP_A,
        CIS_PROP_B
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


    def "auto map matching properties"() {
        when:
        def rec = ['CIS_PROP_A':'val-a', 'CIS_PROP_B':'val-b', 'CIS_PROP_C':'val-c']
        def ph1 = VX.CIS_THING.instance().withMatchingProperties(rec)

        then:
        ph1.propertyValues.size() == 2
        ph1.propertyValues.find({ k, v -> k.name() == PX.CIS_PROP_A.name()}).value == 'val-a'
        ph1.propertyValues.find({ k, v -> k.name() == PX.CIS_PROP_B.name()}).value == 'val-b'
    }



    def "auto map all properties"() {
        when:
        def rec = ['CIS_PROP_A':'val-a', 'CIS_PROP_B':'val-b']
        def ph1 = VX.CIS_THING.instance().withProperties(rec)

        then:
        ph1.propertyValues.size() == 2
        ph1.propertyValues.find({ k, v -> k.name() == PX.CIS_PROP_A.name()}).value == 'val-a'
        ph1.propertyValues.find({ k, v -> k.name() == PX.CIS_PROP_B.name()}).value == 'val-b'
    }


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

