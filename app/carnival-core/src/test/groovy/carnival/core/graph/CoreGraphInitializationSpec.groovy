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
    @Shared graph
    @Shared g
    
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
        coreGraph = CoreGraphTinker.create()
        graph = coreGraph.graph
        g = graph.traversal()
    }

    def setupSpec() {
    } 


    def cleanupSpec() {
    }


    def cleanup() {
        if (coreGraph) coreGraph.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "add definitions convenience method"() {
        def modelErrs 

        expect:
        coreGraph.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 1

        when:
        coreGraph.addDefinitions(GraphModel.VX)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "add definitions"() {
        def modelErrs 

        expect:
        coreGraph.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 1

        when:
        coreGraph.addDefinitions(graph, g, GraphModel.VX)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "set superclass"() {
        when:
        coreGraph.initializeGremlinGraph(graph, g, 'test.coregraphspec')

        then:
        g.V(GraphModel.VX.COLLIE_CLASS.vertex)
            .out(Base.EX.IS_SUBCLASS_OF.label)
            .is(GraphModel.VX.DOG_CLASS.vertex)
        .tryNext().isPresent()
    }
    
}

