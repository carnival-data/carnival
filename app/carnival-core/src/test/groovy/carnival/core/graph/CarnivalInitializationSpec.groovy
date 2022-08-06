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
 * gradle test --tests "carnival.core.graph.CarnivalInitializationSpec"
 *
 */
class CarnivalInitializationSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    static enum VX implements VertexDefinition {
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
        coreGraph = CarnivalTinker.create()
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


    def "add edge definitions convenience method"() {
        def modelErrs 

        expect:
        coreGraph.checkModel().size() == 0

        when:
        coreGraph.addConstraints(GraphModel.VX)
        def d1 = GraphModel.VX.DOG.instance().create(graph)
        def d2 = GraphModel.VX.DOG.instance().create(graph)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 0

        when:
        GraphModel.EX.BARKS_AT.instance().from(d1).to(d2).create()
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 1

        when:
        coreGraph.addConstraints(GraphModel.EX)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "add edge definitions"() {
        def modelErrs 

        expect:
        coreGraph.checkModel().size() == 0

        when:
        coreGraph.addConstraints(graph, g, GraphModel.VX)
        def d1 = GraphModel.VX.DOG.instance().create(graph)
        def d2 = GraphModel.VX.DOG.instance().create(graph)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 0

        when:
        GraphModel.EX.BARKS_AT.instance().from(d1).to(d2).create()
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 1

        when:
        coreGraph.addConstraints(graph, g, GraphModel.EX)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 0
    }




    def "add vertex definitions convenience method"() {
        def modelErrs 

        expect:
        coreGraph.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 1

        when:
        coreGraph.addConstraints(GraphModel.VX)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "add vertex definitions"() {
        def modelErrs 

        expect:
        coreGraph.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = coreGraph.checkModel()

        then:
        modelErrs.size() == 1

        when:
        coreGraph.addConstraints(graph, g, GraphModel.VX)
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

