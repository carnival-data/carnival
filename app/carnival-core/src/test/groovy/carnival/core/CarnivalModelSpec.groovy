package carnival.core



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import carnival.graph.*
import test.coregraphspec.GraphModel
import carnival.core.graph.*
import carnival.core.util.DuplicateModelException



/**
 * gradle test --tests "carnival.core.CarnivalModelSpec"
 *
 */
class CarnivalModelSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    @VertexModel
    static enum VX1 {
        THING,
        GLOBAL_THING(global:true)
    }

    @VertexModel
    static enum VX2 {
        THING
    }

    @VertexModel
    static enum VX3 {
        GLOBAL_THING(global:true)
    }

    @EdgeModel
    static enum EX1 {
    	VERB,
        GLOBAL_VERB(global:true)
    }

    @EdgeModel
    static enum EX2 {
    	VERB
    }

    @EdgeModel
    static enum EX3 {
        GLOBAL_VERB(global:true)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared carnival
    @Shared graph
    @Shared g
    
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
        carnival = CarnivalTinker.create()
        graph = carnival.graph
        g = graph.traversal()
    }

    def setupSpec() {
    } 


    def cleanupSpec() {
    }


    def cleanup() {
        if (carnival) carnival.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "duplicate global edge model ignoreDuplicateModels no exception"() {
        when:
        carnival.ignoreDuplicateModels = true
        carnival.addModel(EX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(EX3)

        then:
        noExceptionThrown()
    }


    def "duplicate edge model ignoreDuplicateModels no exception"() {
        when:
        carnival.ignoreDuplicateModels = true
        carnival.addModel(EX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(EX1)

        then:
        noExceptionThrown()
    }


    def "add duplicate global edge model throws exception"() {
        expect:
        !carnival.ignoreDuplicateModels

        when:
        carnival.addModel(EX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(EX3)

        then:
        DuplicateModelException e = thrown()
    }


    def "add duplicate edge model throws exception"() {
        expect:
        !carnival.ignoreDuplicateModels

        when:
        carnival.addModel(EX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(EX1)

        then:
        DuplicateModelException e = thrown()
    }


    def "duplicate global vertex model ignoreDuplicateModels no exception"() {
        when:
        carnival.ignoreDuplicateModels = true
        carnival.addModel(VX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(VX3)

        then:
        noExceptionThrown()
    }


    def "duplicate vertex model ignoreDuplicateModels no exception"() {
        when:
        carnival.ignoreDuplicateModels = true
        carnival.addModel(VX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(VX1)

        then:
        noExceptionThrown()
    }


    def "add duplicate global vertex model throws exception"() {
        expect:
        !carnival.ignoreDuplicateModels

        when:
        carnival.addModel(VX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(VX3)

        then:
        DuplicateModelException e = thrown()
    }


    def "add duplicate vertex model throws exception"() {
        expect:
        !carnival.ignoreDuplicateModels

        when:
        carnival.addModel(VX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(VX1)

        then:
        DuplicateModelException e = thrown()
    }


    def "add vertex model convenience method"() {
        def modelErrs 

        expect:
        carnival.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 1

        when:
        carnival.addModel(GraphModel.VX)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "add vertex model"() {
        def modelErrs 

        expect:
        carnival.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 1

        when:
        carnival.addVertexModel(graph, g, GraphModel.VX)

        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "add edge model convenience method"() {
        def modelErrs 

        expect:
        carnival.checkModel().size() == 0

        when:
        carnival.addModel(GraphModel.VX)
        def d1 = GraphModel.VX.DOG.instance().create(graph)
        def d2 = GraphModel.VX.DOG.instance().create(graph)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0

        when:
        GraphModel.EX.BARKS_AT.instance().from(d1).to(d2).create()
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 1

        when:
        carnival.addModel(GraphModel.EX)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "add vertex and edge models"() {
        def modelErrs 

        expect:
        carnival.checkModel().size() == 0

        when:
        carnival.addModel(graph, g, GraphModel.VX)
        def d1 = GraphModel.VX.DOG.instance().create(graph)
        def d2 = GraphModel.VX.DOG.instance().create(graph)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0

        when:
        GraphModel.EX.BARKS_AT.instance().from(d1).to(d2).create()
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 1

        when:
        carnival.addModel(graph, g, GraphModel.EX)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }




    def "add vertex model convenience method"() {
        def modelErrs 

        expect:
        carnival.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 1

        when:
        carnival.addModel(GraphModel.VX)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "add vertex model"() {
        def modelErrs 

        expect:
        carnival.checkModel().size() == 0

        when:
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 1

        when:
        carnival.addModel(graph, g, GraphModel.VX)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "set superclass"() {
        when:
        carnival.addModelsFromPackage(graph, g, 'test.coregraphspec')

        then:
        g.V(GraphModel.VX.COLLIE_CLASS.vertex)
            .out(Base.EX.IS_SUBCLASS_OF.label)
            .is(GraphModel.VX.DOG_CLASS.vertex)
        .tryNext().isPresent()
    }
    
}

