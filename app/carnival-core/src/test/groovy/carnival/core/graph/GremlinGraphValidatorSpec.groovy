package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import carnival.graph.*
import test.TestModel



/**
 * gradle test --tests "carnival.core.graph.GremlinGraphValidatorSpec"
 *
 */
class GremlinGraphValidatorSpec extends Specification {

    static enum VX1 implements VertexDefTrait {
        GGV_THING,
        GGV_ANOTHER_THING,
        GGV_SUITCASE,
        GGV_SALESMAN
    }

    static enum VX2 implements VertexDefTrait {
        GGV_THING(vertexProperties:[Core.PX.NAME.withConstraints(required:true, unique:true)])

        private VX2() {}
        private VX2(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum VX3 implements VertexDefTrait {
        GGV_ANOTHER_THING(
            global:true,
            vertexProperties:[Core.PX.NAME.withConstraints(required:true, unique:true)]
        )

        private VX3() {}
        private VX3(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum EX1 implements EdgeDefTrait {
        GGV_RELATION
    }

    static enum EX2 implements EdgeDefTrait {
        GGV_RELATION(
            domain: [VX1.GGV_THING], 
            range: [VX1.GGV_ANOTHER_THING]
        )

        private EX2() {}
        private EX2(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum EX3 implements EdgeDefTrait {
        GGV_GLOBAL_RELATION(
            global:true,
            domain: [VX1.GGV_THING], 
            range: [VX3.GGV_ANOTHER_THING]
        )

        private EX3() {}
        private EX3(Map m) {m.each { k,v -> this."$k" = v } }
    }

    static enum EX4 implements EdgeDefTrait {
        GGV_GLOBAL_RELATION
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared coreGraph
    @Shared graph
    @Shared graphValidator
    @Shared g
    @Shared graphSchema
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() { 
        coreGraph = CoreGraphTinker.create()
        graph = coreGraph.graph
        graphSchema = coreGraph.graphSchema
        graphValidator = new GremlinGraphValidator()
        g = graph.traversal()
    }

    def cleanup() {
        if (g) g.close()
        if (coreGraph) coreGraph.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "test package is modelled on demand overload"() {
        def thing, anotherThing, suitcase

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        coreGraph.initializeGremlinGraph(graph, g, 'test')
        TestModel.VX.APPLICATION.instance().withProperty(Core.PX.NAME, 'TestApp').vertex(graph, g)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }


    def "test package is not modelled by default overload"() {
        def thing, anotherThing, suitcase

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        TestModel.VX.APPLICATION.instance().withProperty(Core.PX.NAME, 'TestApp').vertex(graph, g)

        then:
        graphValidator.checkModel(g, graphSchema).size() > 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }


    def "test package is modelled on demand"() {
        def thing, anotherThing, suitcase

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        coreGraph.initializeGremlinGraph(graph, g, 'test')
        TestModel.VX.TEST_THING.instance().withProperty(Core.PX.NAME, 'TestThingName').vertex(graph, g)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }


    def "test package is not modelled by default"() {
        def thing, anotherThing, suitcase

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        TestModel.VX.TEST_THING.instance().withProperty(Core.PX.NAME, 'TestThingName').vertex(graph, g)

        then:
        graphValidator.checkModel(g, graphSchema).size() > 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }


    def "carnival package is modelled"() {
        def thing, anotherThing, suitcase

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        Core.VX.APPLICATION.instance().withProperty(Core.PX.NAME, 'GremlinGraphValidatorSpecApp').vertex(graph, g)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }


    def "global edge def constraints"() {
        def thing, anotherThing, suitcase

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        thing = VX1.GGV_THING.instance().vertex(graph, g)
        anotherThing = VX3.GGV_ANOTHER_THING.instance().withProperty(Core.PX.NAME, 'name').vertex(graph, g)
        thing.addEdge(EX4.GGV_GLOBAL_RELATION.label, anotherThing)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        suitcase = VX1.GGV_SUITCASE.instance().vertex(graph, g)
        EX4.GGV_GLOBAL_RELATION.relate(g, thing, suitcase)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 1
    }


    def "unmodelled edge labels fail unless there is global def"() {
        def thing, anotherThing

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0

        when:
        thing = VX1.GGV_THING.instance().vertex(graph, g)
        anotherThing = VX1.GGV_ANOTHER_THING.instance().vertex(graph, g)
        thing.addEdge(EX3.GGV_GLOBAL_RELATION.label, anotherThing)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0

        when:
        thing.addEdge('someRandoEdgeLabel', anotherThing)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 1
    }


    def "unmodelled vertex labels fail unless there is global def"() {
        def thing

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0

        when:
        thing = graph.addVertex(T.label, VX1.GGV_ANOTHER_THING.label)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0

        when:
        thing = graph.addVertex(T.label, VX1.GGV_THING.label)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 1
    }


    def "checkModel global vertex def addVertex"() {
        def thing

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        thing = graph.addVertex(T.label, VX1.GGV_ANOTHER_THING.label)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 1

        when:
        thing.property(Core.PX.NAME.label, 'name')

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }



    def "checkModel global vertex def enum"() {
        def thing

        expect:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        thing = VX1.GGV_ANOTHER_THING.instance().vertex(graph, g)

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 1

        when:
        thing.property(Core.PX.NAME.label, 'name')

        then:
        graphValidator.checkModel(g, graphSchema).size() == 0
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }


    def "test checkModel for unmodeled vertex and edge labels"() {
        def suitcase, salesman, id, idClass

        expect:
        coreGraph.checkModel().size() == 0

        when:
        id = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, '123').vertex(graph, g)
        idClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'idClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)
        Base.EX.IS_INSTANCE_OF.relate(g, id, idClass)

        then:
        CoreGraphUtils.printGraph(g)
        coreGraph.checkModel().size() == 0

        when:
        suitcase = graph.addVertex(T.label, "Suitcase")
        salesman = graph.addVertex(T.label, "Salesman")

        then:
        coreGraph.checkModel().size() == 1
        println coreGraph.checkModel()

        when:
        suitcase.addEdge("belongs_to", salesman)

        then:
        coreGraph.checkModel().size() == 2
        println coreGraph.checkModel()
    }


    def "edge name space"() {
        def v1, v2, e1, e2

        expect:
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        v1 = VX1.GGV_THING.controlledInstance().vertex(graph, g)
        v2 = VX1.GGV_THING.controlledInstance().vertex(graph, g)
        e1 = EX1.GGV_RELATION.setRelationship(g, v1, v2)

        then:
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        //e2 = EX2.GGV_RELATION.setRelationship(g, v1, v2)
        e2 = v1.addEdge(EX2.GGV_RELATION.label, v2, Base.PX.NAME_SPACE.label, EX2.GGV_RELATION.nameSpace)

        then:
        graphValidator.checkConstraints(g, graphSchema).size() == 1
    }


    def "base graph"() {
        expect:
        graphValidator.checkConstraints(g, graphSchema).size() == 0
    }


    def "vertex enum required prop"() {
        expect:
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        VX1.GGV_THING.controlledInstance().vertex(graph, g)

        then:
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        def lv = VX2.GGV_THING.controlledInstance()
            .withProperty(Core.PX.NAME, 'some name')
            .vertex(graph, g)

        then:
        graphValidator.checkConstraints(g, graphSchema).size() == 0

        when:
        lv.property(Core.PX.NAME.label).remove()

        then:
        graphValidator.checkConstraints(g, graphSchema).size() == 1
    }

}

