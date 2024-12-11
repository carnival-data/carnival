package carnival.core



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.*
import test.coregraphspec.GraphModel
import carnival.core.graph.*
import carnival.core.util.DuplicateModelException
import carnival.core.Carnival.AddModelResult



/**
 * gradle test --tests "carnival.core.CarnivalModelSpec"
 *
 */
class CarnivalModelSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    @PropertyModel
    static enum PX1 {
        PA
    }

    @VertexModel
    static enum VX1 {
        THING,
        ANOTHER_THING(propertyDefs:[PX1.PA])
    }

    @VertexModel
    static enum VX2 {
        THING,
        THING_CLASS
    }

    @EdgeModel
    static enum EX1 {
        VERB
    }

    /*@VertexModel
    static enum VX3 {
        GLOBAL_THING(global:true)
    }*/

    /*@EdgeModel
    static enum EX1 {
    	VERB,
        GLOBAL_VERB(global:true)
    }*/

    /*@EdgeModel
    static enum EX3 {
        GLOBAL_VERB(global:true)
    }*/


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
        if (g) g.close()
        if (carnival) carnival.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "add models from package results"() {
        when:
        List<AddModelResult> results = carnival
            .addModelsFromPackage('test.coregraphspec')

        then:
        results
        results.size() == 2

        when:
        AddModelResult vertexModelRes = results.find{
            it.vertexConstraints
        }
        AddModelResult edgeModelRes = results.find{
            it.edgeConstraints
        }

        then:
        vertexModelRes
        edgeModelRes
    }


    def "add vertex model from package"() {
        def modelErrs

        expect:
        carnival.checkModel().size() == 0
        GraphModel.VX.DOG_CLASS.vertex == null

        when:
        int numVerts1
        carnival.withGremlin { graph, g ->
            numVerts1 = g.V().count().next()
        }
        GraphModel.VX.DOG.instance().create(graph)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 1

        when:
        carnival.addModelsFromPackage('test.coregraphspec')
        int numVerts2
        carnival.withGremlin { graph, g ->
            numVerts2 = g.V().count().next()
        }
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
        GraphModel.VX.DOG_CLASS.vertex
        GraphModel.VX.COLLIE_CLASS.vertex
        numVerts2 == numVerts1 + 3
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
        carnival.addModel(GraphModel.VX)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "edge model result"() {
        when:
        carnival.addModel(PX1)
        carnival.addModel(VX1)
        def res = carnival.addModel(EX1)

        then:
        res
        res.edgeConstraints
        res.edgeConstraints.size() == 1
    }


    def "property model result"() {
        when:
        def res = carnival.addModel(PX1)

        then:
        res
        res.propertyConstraints
        res.propertyConstraints.size() == 1
    }

        
    def "core model class vertices"() {
        when:
        def processClassVs
        carnival.withGremlin { graph, g ->
            processClassVs = g.V().isa(Core.VX.PROCESS_CLASS).toList()
        }

        then:
        processClassVs
        processClassVs.size() == 1
    }


    def "addModel vertex creates class vertices"() {

        when:
        def res = carnival.addModel(VX2)

        def thingClassVs
        carnival.withGremlin { graph, g ->
            thingClassVs = g.V().isa(VX2.THING_CLASS).toList()
        }

        then:
        thingClassVs
        thingClassVs.size() == 1
    }


    def "vertex model result"() {
        when:
        carnival.addModel(PX1)
        def res = carnival.addModel(VX1)

        then:
        res
        res.vertexConstraints
        res.vertexConstraints.size() == 2
    }


    /*def "duplicate global edge model ignoreDuplicateModels no exception"() {
        when:
        carnival.ignoreDuplicateModels = true
        carnival.addModel(EX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(EX3)

        then:
        noExceptionThrown()
    }*/


    /*def "duplicate edge model ignoreDuplicateModels no exception"() {
        when:
        carnival.ignoreDuplicateModels = true
        carnival.addModel(EX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(EX1)

        then:
        noExceptionThrown()
    }*/


    /*def "add duplicate global edge model throws exception"() {
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
    }*/


    /*def "add duplicate edge model throws exception"() {
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
    }*/


    /*def "duplicate global vertex model ignoreDuplicateModels no exception"() {
        when:
        carnival.ignoreDuplicateModels = true
        carnival.addModel(VX1)

        then:
        noExceptionThrown()

        when:
        carnival.addModel(VX3)

        then:
        noExceptionThrown()
    }*/


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


    /*def "add duplicate global vertex model throws exception"() {
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
    }*/


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
        carnival.addVertexModel(GraphModel.VX)

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
        carnival.addModel(GraphModel.VX)
        modelErrs = carnival.checkModel()

        then:
        modelErrs.size() == 0
    }


    def "set superclass"() {
        when:
        carnival.addModelsFromPackage('test.coregraphspec')

        def classSubGraph
        carnival.withGremlin { graph, g ->
            classSubGraph = g.V(GraphModel.VX.COLLIE_CLASS.vertex)
                .out(Base.EX.IS_SUBCLASS_OF.label)
                .is(GraphModel.VX.DOG_CLASS.vertex)
            .tryNext()
        }

        then:
        classSubGraph.isPresent()
    }
    
}

