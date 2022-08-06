package carnival.core.graph


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of



/**
 * gradle test --tests "carnival.core.graph.CoreGraphInitSpec"
 *
 */
class CoreGraphInitSpec extends Specification {

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
        CoreGraphNeo4j.clearGraph()
        def classDef = Core.VX.DATA_TRANSFORMATION_PROCESS_CLASS

        // we need to set to null because initialization machinery from
        // previous tests may have (did) initi all vertex defs
        classDef.vertex = null

        when:
        def graph = CoreGraphNeo4j.openGremlinGraph()

        then:
        graph != null

        when:
        def graphSchema = new CoreGraphSchema()
        def graphValidator = new GremlinGraphValidator()
        def coreGraph = new CoreGraphNeo4j(graph, graphSchema, graphValidator)

        then:
        classDef.vertex == null

        when:
        def g = graph.traversal()
        try {
            coreGraph.initializeGremlinGraph(graph, g)
            coreGraph.initNeo4j(graph, g)
        } finally {
            if (g) g.close()
        }

        then:
        classDef.vertex != null

        cleanup:
        if (coreGraph) coreGraph.graph.close()
    }

}

