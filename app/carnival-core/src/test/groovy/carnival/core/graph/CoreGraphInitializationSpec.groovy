package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import carnival.graph.*
import test.coregraphspec.GraphModel



/**
 * gradle test --tests "carnival.core.graph.CoreGraphInitializationSpec"
 *
 */
class CoreGraphInitializationSpec extends Specification {

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
    
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
    	
    }

    def setupSpec() {
        coreGraph = CoreGraphTinker.create()
    } 


    def cleanupSpec() {
        if (coreGraph) coreGraph.close()
    }


    def cleanup() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "set superclass"() {
        given:
    	def graph = coreGraph.graph
    	def g = graph.traversal()

        when:
        coreGraph.initializeGremlinGraph(graph, g, 'test.coregraphspec')

        then:
        g.V(GraphModel.VX.COLLIE_CLASS.vertex)
            .out(Base.EX.IS_SUBCLASS_OF.label)
            .is(GraphModel.VX.DOG_CLASS.vertex)
        .tryNext().isPresent()
    }
    
}

