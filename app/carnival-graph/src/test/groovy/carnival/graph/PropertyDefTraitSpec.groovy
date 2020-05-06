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
 * gradle test --tests "carnival.graph.PropertyDefTraitSpec"
 *
 */
class PropertyDefTraitSpec extends Specification {

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

    /*
    def "required index"() {
        when:
        def p1 = PX.PROP_A.require().index()

        then:
        p1 != null
        p1 instanceof ConstrainedPropertyDefTrait
        p1.required
        !p1.unique
        p1.index
    }

    def "required"() {
        when:
        def p1 = PX.PROP_A.require()

        then:
        p1 != null
        p1 instanceof ConstrainedPropertyDefTrait
        p1.required
        !p1.unique
        !p1.index
    }
    */


    def "withConstraints required"() {
        when:
        def p1 = PX.PROP_A.withConstraints(required:true)

        then:
        p1 != null
        p1 instanceof ConstrainedPropertyDefTrait
        p1.required
        !p1.unique
        !p1.index
    }
}

