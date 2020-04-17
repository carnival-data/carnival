package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import carnival.graph.*



/**
 * gradle test --tests "carnival.core.graph.CoreGraphNeo4jSpec"
 *
 */
class CoreGraphNeo4jSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    static enum VX implements VertexDefTrait {
        CGS_SUITCASE
    }

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared coreGraph
    
    @Shared controlledInstances = [
        Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "1"),
        Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "2"),
    ]


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
    }

    def setupSpec() {
        CoreGraphNeo4j.clearGraph()
        coreGraph = CoreGraphNeo4j.create(controlledInstances:controlledInstances)
    } 


    def cleanupSpec() {
        if (coreGraph) coreGraph.graph.close()
    }


    def cleanup() {
        if (coreGraph) coreGraph.graph.tx().rollback()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////
    
    def "test apoc"() {
        when: 
        def graph = coreGraph.graph
        def apocVersion = graph.cypher('RETURN apoc.version()').toList().first()
        println "apocVersion: $apocVersion"

        then:
        apocVersion != null
    }


    def "test initializeGraph for uniqueness constraint existence"() {
    	when:
    	def graph = coreGraph.graph
    	def constraints = graph.cypher("CALL db.constraints()").toList()

    	then:
    	//println constraints
    	constraints != null
    	constraints.size() >= 2
    }

}

