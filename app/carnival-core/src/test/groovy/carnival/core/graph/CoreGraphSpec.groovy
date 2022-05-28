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
 * gradle test --tests "carnival.core.graph.CoreGraphSpec"
 *
 */
class CoreGraphSpec extends Specification {

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
    
    @Shared vertexBuilders = [
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "1"),
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "2"),
    ]


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
        coreGraph = CoreGraphTinker.create(vertexBuilders:vertexBuilders)
    }

    def setupSpec() {
        //CoreGraphNeo4j.clearGraph()
        //coreGraph = CoreGraphNeo4j.create(vertexBuilders:vertexBuilders)
    } 


    def cleanupSpec() {
        //if (coreGraph) coreGraph.graph.close()
    }


    def cleanup() {
        //if (coreGraph) coreGraph.graph.tx().rollback()
        if (coreGraph) coreGraph.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////
    
    /**
    * This will test graph initizilation once it's been moved to CoreGraph
    */
    def "test graph creation"() {
    	when: 
    	def graph = coreGraph.graph

        println "===================="
        println "initial graph"
        CoreGraphUtils.printGraph(graph.traversal())
        println "===================="

    	then:
    	graph != null
    }


    def "test initializeGraph for VertexBuilder creation"() {
    	given:
    	def graph = coreGraph.graph
    	def g = graph.traversal()
    	def vs = []
    	//println "graph: $graph"
    	//println "--------"
    	//CoreGraphUtils.printGraph(g)


    	//expect:
    	//coreGraph.graphSchema.vertexBuilders?.size() == vertexBuilders?.size()

    	when:
    	vs = g.V().hasLabel('Identifier').toList()

    	then:
    	vs
    	vs.size() == 2

    	when:
    	vs = g.V().hasLabel('Identifier').has("value", "1").toList()

    	then:
    	vs
    	vs.size() == 1

    	when:
    	vs = g.V().hasLabel('Identifier').has("value", "2").toList()

    	then:
    	vs
    	vs.size() == 1

    	when:
    	vs = g.V().hasLabel('Identifier').has("value", "3").toList()

    	then:
    	vs.size() == 0
    }


    def "test checkConstraints for exactly one singleton vertex"() {
		given:
    	def graph = coreGraph.graph
    	def g = graph.traversal()
    	def vs

    	expect:
    	coreGraph.checkConstraints().size() == 0

    	when:
    	g.V().hasLabel('Identifier').has("value", "1").next().remove()
    	vs = g.V().hasLabel('Identifier').has("value", "1").toList()

    	then:
    	vs.size() == 0
    	coreGraph.checkConstraints().size() == 1
    	//println coreGraph.checkConstraints()

    	when:
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "1").vertex(graph, g)
    	//graph.addVertex(T.label, "Identifier", "value", "1")
    	vs = g.V().hasLabel('Identifier').has("value", "1").toList()

    	then:
    	vs.size() == 1
    	coreGraph.checkConstraints().size() == 0

    	when:
    	graph.addVertex(
            T.label, "Identifier",
            Base.PX.NAME_SPACE.label, Core.VX.IDENTIFIER.nameSpace, 
            "value", "1"
        )
    	vs = g.V().hasLabel('Identifier').has("value", "1").toList()

    	then:
    	vs.size() == 2
    	coreGraph.checkConstraints().size() == 1

    }

    def "test checkConstraints for property existence constraints"() {
    	given:
    	def graph = coreGraph.graph
    	def g = graph.traversal()
    	def vert

    	expect:
    	coreGraph.checkConstraints().size() == 0

    	when:
    	vert = graph.addVertex(
            T.label, "IdentifierClass",
            Base.PX.NAME_SPACE.label, Core.VX.IDENTIFIER_CLASS.nameSpace
        )

    	then:
    	coreGraph.checkConstraints().size() == 3
    }


    def "test checkConstraints for relationship domain constraints"() {
        given:
        def graph = coreGraph.graph
        def g = graph.traversal()

        def identifierFacility
        def identifier
        def identifierClass
        def suitcase

        expect:
        coreGraph.checkConstraints().size() == 0

        when:
        identifierFacility = Core.VX.IDENTIFIER_FACILITY.instance().withProperty(Core.PX.NAME, "f1").vertex(graph, g)
        //identifierFacility = graph.addVertex(T.label, "IdentifierFacility", "name", "f1")
        identifier = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "abc123").vertex(graph, g)
        //identifier = graph.addVertex(T.label, "Identifier", "value", "abc123")
        identifierClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, "testIdClass",
            Core.PX.HAS_SCOPE, false,
            Core.PX.HAS_CREATION_FACILITY, true
        ).vertex(graph, g)
        //identifierClass = graph.addVertex(
        //   T.label, "IdentifierClass", 
        //    Base.PX.NAME_SPACE.label, Core.VX.IDENTIFIER_CLASS.nameSpace,
        //    "name", "testIdClass", 
        //    "hasScope", false, 
        //    "hasCreationFacility", true
        //)
        suitcase = VX.CGS_SUITCASE.instance().vertex(graph, g)

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        Base.EX.IS_INSTANCE_OF.relate(g, identifier, identifierClass)
        //identifier.addEdge("is_instance_of", identifierClass)
        Core.EX.WAS_CREATED_BY.relate(g, identifier, identifierFacility)
        //identifier.addEdge("was_created_by", identifierFacility)

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        suitcase.addEdge(Core.EX.WAS_CREATED_BY.label, identifierFacility, Base.PX.NAME_SPACE.label, Core.EX.WAS_CREATED_BY.nameSpace)
        //Core.EX.WAS_CREATED_BY.relate(g, suitcase, identifierFacility)
        //suitcase.addEdge("was_created_by", identifierFacility)

        then:
        coreGraph.checkConstraints().size() == 1
    }


    def "test checkConstraints for relationship range constraints"() {
        given:
        def graph = coreGraph.graph
        def g = graph.traversal()

        def identifierFacility
        def identifier
        def identifierClass
        def suitcase

        expect:
        coreGraph.checkConstraints().size() == 0

        when:
        identifierFacility = Core.VX.IDENTIFIER_FACILITY.instance().withProperty(Core.PX.NAME, "f1").vertex(graph, g)
        identifier = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "abc123").vertex(graph, g)
        identifierClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, "testIdClass",
            Core.PX.HAS_SCOPE, false,
            Core.PX.HAS_CREATION_FACILITY, true
        ).vertex(graph, g)
        suitcase = VX.CGS_SUITCASE.instance().vertex(graph, g)

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        Base.EX.IS_INSTANCE_OF.relate(g, identifier, identifierClass)
        Core.EX.WAS_CREATED_BY.relate(g, identifier, identifierFacility)
        //identifier.addEdge("is_instance_of", identifierClass)
        //identifier.addEdge("was_created_by", identifierFacility)

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        //Core.EX.WAS_CREATED_BY.relate(g, identifier, suitcase)
        identifier.addEdge(Core.EX.WAS_CREATED_BY.label, suitcase, Base.PX.NAME_SPACE.label, Core.EX.WAS_CREATED_BY.nameSpace)
        //identifier.addEdge("was_created_by", suitcase)

        then:
        coreGraph.checkConstraints().size() == 1
    }


    def "test checkConstraints for identifier uniqueness"() {
        given:
        def graph = coreGraph.graph
        def g = graph.traversal()

        def idClass1 = graph.addVertex(T.label, 'IdentifierClass', 'name', 'idClass1', 'hasCreationFacility', false, 'hasScope', false)
        def idClass2 = graph.addVertex(T.label, 'IdentifierClass', 'name', 'idClass2', 'hasCreationFacility', false, 'hasScope', false)
        def scopedIdClass = graph.addVertex(T.label, 'IdentifierClass', 'name', 'testIdClass', 'hasCreationFacility', false, 'hasScope', true)
        def facilityIdClass = graph.addVertex(T.label, 'IdentifierClass', 'name', 'testFacilityClass', 'hasCreationFacility', true, 'hasScope', false)

        def scope1 = graph.addVertex(T.label, 'IdentifierScope', "name", "scope1")
        def scope2 = graph.addVertex(T.label, 'IdentifierScope', "name", "scope2")

        def facility1 = graph.addVertex(T.label, 'IdentifierFacility', "name", "facility1")
        def facility2 = graph.addVertex(T.label, 'IdentifierFacility', "name", "facility2")

        def id1Class1 = graph.addVertex(T.label, 'Identifier', 'value', 'id1')
        id1Class1.addEdge('is_instance_of', idClass1)

        def id2Class1 = graph.addVertex(T.label, 'Identifier', 'value', 'id2')
        id2Class1.addEdge('is_instance_of', idClass1)

        def id1Class2 = graph.addVertex(T.label, 'Identifier', 'value', 'id1')
        id1Class2.addEdge('is_instance_of', idClass2)

        // scope
        def scopedId1Scope1 = graph.addVertex(T.label, 'Identifier', 'value', 'scopedId1')
        scopedId1Scope1.addEdge('is_instance_of', scopedIdClass)
        scopedId1Scope1.addEdge('is_scoped_by', scope1)

        def scopedId1Scope2 = graph.addVertex(T.label, 'Identifier', 'value', 'scopedId1')
        scopedId1Scope2.addEdge('is_instance_of', scopedIdClass)
        scopedId1Scope2.addEdge('is_scoped_by', scope2)


        // facility
        def facilityId1Facility1 = graph.addVertex(T.label, 'Identifier', 'value', 'facilityId1')
        facilityId1Facility1.addEdge('is_instance_of', facilityIdClass)
        facilityId1Facility1.addEdge('was_created_by', facility1)

        def facilityId1Facility2 = graph.addVertex(T.label, 'Identifier', 'value', 'facilityId1')
        facilityId1Facility2.addEdge('is_instance_of', facilityIdClass)
        facilityId1Facility2.addEdge('was_created_by', facility2)



        expect:
        coreGraph.checkConstraints().size() == 0

        // duplicate id val of the same class
        when:
        def id1Class1Dupe = graph.addVertex(T.label, 'Identifier', 'value', 'id1')
        id1Class1Dupe.addEdge('is_instance_of', idClass1)

        then:
        coreGraph.checkConstraints().size() == 1

        // reset
        when:
        id1Class1Dupe.remove()

        then:
        coreGraph.checkConstraints().size() == 0


        //duplicate id val of the same class, same scope
        when:
        def scopedId1Scope1Dupe = graph.addVertex(T.label, 'Identifier', 'value', 'scopedId1')
        scopedId1Scope1Dupe.addEdge('is_instance_of', scopedIdClass)
        scopedId1Scope1Dupe.addEdge('is_scoped_by', scope1)


        then:
        coreGraph.checkConstraints().size() == 1

        // reset
        when:
        scopedId1Scope1Dupe.remove()

        then:
        coreGraph.checkConstraints().size() == 0


        //duplicate id val of the same class, same facility
        when:
        def facilityId1Facility1Dupe = graph.addVertex(T.label, 'Identifier', 'value', 'facilityId1')
        facilityId1Facility1Dupe.addEdge('is_instance_of', facilityIdClass)
        facilityId1Facility1Dupe.addEdge('was_created_by', facility1)

        then:
        coreGraph.checkConstraints().size() == 1

    }


    def "test checkModel for unmodeled vertex and edge labels"() {
        given:
        def graph = coreGraph.graph
        def suitcase, salesman, id, idClass
        def g = graph.traversal()

        expect:
        !coreGraph.checkModel()

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

    /*
    // Apparently the keys returned by "CALL db.propertyKeys()" consist of any key that was ever created...
    // if a vertex with a new key is created but then that vertex is deleted or the transaction is rolled back,
    // that key is still returned.  This causes tests to fail, so commenting out for now.
    */
    /*
    def "test checkModel for unmodeled vertex properties"() {
        given:
        def graph = coreGraph.graph
        def patient

        expect:
        !coreGraph.checkModel()

        when:
        patient = graph.addVertex(T.label, "Patient", "customProp", "abc123")

        then:
        coreGraph.checkModel().size() == 1
        println coreGraph.checkModel()
    }*/



    def "test Identifier vertex class"() {
        given:
        def graph = coreGraph.graph
        def g = graph.traversal()
        Exception t

        def idClass1 = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'idClass1',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)

        def idClass2 = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'idClass2',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)

        def scopedIdClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'testIdClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, true
        ).vertex(graph, g)

        def facilityIdClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'testFacilityIdClass',
            Core.PX.HAS_CREATION_FACILITY, true,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)


        def scope1 = Core.VX.IDENTIFIER_SCOPE.instance().withProperty(Core.PX.NAME, 'scope1').vertex(graph, g)
        def scope2 = Core.VX.IDENTIFIER_SCOPE.instance().withProperty(Core.PX.NAME, 'scope2').vertex(graph, g)

        def facility1 = Core.VX.IDENTIFIER_FACILITY.instance().withProperty(Core.PX.NAME, 'facility1').vertex(graph, g)
        def facility2 = Core.VX.IDENTIFIER_FACILITY.instance().withProperty(Core.PX.NAME, 'facility2').vertex(graph, g)

        def id1Class1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Base.EX.IS_INSTANCE_OF.relate(g, id1Class1, idClass1) 

        def id2Class1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id2').vertex(graph, g)
        Base.EX.IS_INSTANCE_OF.relate(g, id2Class1, idClass1) 

        def id1Class2 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Base.EX.IS_INSTANCE_OF.relate(g, id1Class2, idClass2) 

        def scopedId = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'scopedId1').vertex(graph, g)
        Base.EX.IS_INSTANCE_OF.relate(g, scopedId, scopedIdClass) 
        Core.EX.IS_SCOPED_BY.relate(g, scopedId, scope1) 

        def facilityId = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'facilityId1').vertex(graph, g)
        Base.EX.IS_INSTANCE_OF.relate(g, facilityId, facilityIdClass) 
        Core.EX.WAS_CREATED_BY.relate(g, facilityId, facility1) 

        def allIdVerts = [id1Class1, id2Class1, id1Class2, scopedId, facilityId]

        def identifier
        def preIdCount
        def postIdCount

        def idClass
        def idScope
        def idValue
        def expectedNode
        def idNode

        expect:
        coreGraph.checkModel().size() == 0
        coreGraph.checkConstraints().size() == 0

        /*
        where:
        idClass | idScope   | idValue   || existingNode 
        idClass1| null      | "id1"     || id1Class1
        idClass1| null      | "id2"     || id2Class1
        idClass2| null      | "id1"     || id1Class2
        scopedId| scope1    | "scopedId1"|| scopedId
        facilityId| scope1  | "facilityId1"|| facilityId
        */

        // existing nodes
        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:idClass1, identifierScope:null, value:"id1" )
        idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        idNode == id1Class1
        preIdCount == postIdCount


        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:idClass1, identifierScope:null, value:"id2" )
        idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        idNode == id2Class1
        preIdCount == postIdCount


        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:idClass2, identifierScope:null, value:"id1" )
        idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        idNode == id1Class2
        preIdCount == postIdCount


        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:scopedIdClass, identifierScope:scope1, value:"scopedId1" )
        idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        preIdCount == postIdCount
        idNode == scopedId

        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:facilityIdClass, identifierFacility:facility1, value:"facilityId1" )
        idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        preIdCount == postIdCount
        idNode == facilityId


        // new nodes
        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:idClass2, identifierScope:null, value:"id2" )
        idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        !(idNode in allIdVerts)
        preIdCount == postIdCount - 1


        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:scopedId, identifierScope:scope2, value:"scopedId1" )
        idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        !(idNode in allIdVerts)
        preIdCount == postIdCount - 1

        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        identifier = new Identifier(identifierClass:facilityId, identifierFacility:scope2, value:"facilityId1" )
        idNode = identifier.getOrCreateNode(graph) 
        //postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        //!(idNode in allIdVerts)
        //preIdCount == postIdCount - 1
        t = thrown()

    }
}

