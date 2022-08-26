package carnival.core


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import carnival.core.graph.DefaultGraphSchema
import carnival.core.graph.DefaultGraphValidator
import carnival.core.graph.*



/**
 * gradle test --tests "carnival.core.CarnivalInitSpec"
 *
 */
class CarnivalInitSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
    	
    }

    def setupSpec() {
    } 


    def cleanupSpec() {
    }


    def cleanup() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "test vertex def trait initizilation"() {
        given:
        def classDef = Core.VX.DATA_TRANSFORMATION_PROCESS_CLASS

        // we need to set to null because initialization machinery from
        // previous tests may have (did) init all vertex defs
        classDef.vertex = null

        expect:
        classDef.vertex == null

        when:
        def graph = TinkerGraph.open()
        def graphSchema = new DefaultGraphSchema()
        def graphValidator = new DefaultGraphValidator()
        def carnival = new CarnivalTinker(graph, graphSchema, graphValidator)

        def g = graph.traversal()
        try {
            carnival.initializeGremlinGraph(graph, g)
        } finally {
            if (g) g.close()
        }

        then:
        classDef.vertex != null

        cleanup:
        if (graph) graph.close()
    }

}

