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
 * gradle test --tests "carnival.core.graph.CarnivalSpec"
 *
 */
class CarnivalSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    static enum VX implements VertexDefinition {
        CGS_SUITCASE
    }

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared carnival
    
    @Shared vertexBuilders = [
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "1"),
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "2"),
    ]


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
        carnival = CarnivalTinker.create(vertexBuilders:vertexBuilders)
        /*carnival.withTraversal { graph, g ->
            ["1", "2"].each {
                Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, it).create(graph)
            }
        }*/
    }

    def setupSpec() { } 


    def cleanupSpec() { }


    def cleanup() {
        if (carnival) carnival.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////
    
    /**
    * This will test graph initizilation once it's been moved to Carnival
    */
    def "test graph creation"() {
    	when: 
    	def graph = carnival.graph

        println "===================="
        println "initial graph"
        CarnivalUtils.printGraph(graph.traversal())
        println "===================="

    	then:
    	graph != null
    }


    def "test initializeGraph for VertexBuilder creation"() {
    	given:
    	def graph = carnival.graph
    	def g = graph.traversal()
    	def vs = []
    	//println "graph: $graph"
    	//println "--------"
    	//CarnivalUtils.printGraph(g)


    	//expect:
    	//carnival.graphSchema.vertexBuilders?.size() == vertexBuilders?.size()

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
    	def graph = carnival.graph
    	def g = graph.traversal()
    	def vs

    	expect:
    	carnival.checkConstraints().size() == 0

    	when:
    	g.V().hasLabel('Identifier').has("value", "1").next().remove()
    	vs = g.V().hasLabel('Identifier').has("value", "1").toList()

    	then:
    	vs.size() == 0
    	carnival.checkConstraints().size() == 1
    	//println carnival.checkConstraints()

    	when:
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "1").vertex(graph, g)
    	//graph.addVertex(T.label, "Identifier", "value", "1")
    	vs = g.V().hasLabel('Identifier').has("value", "1").toList()

    	then:
    	vs.size() == 1
    	carnival.checkConstraints().size() == 0

    	when:
    	graph.addVertex(
            T.label, "Identifier",
            Base.PX.NAME_SPACE.label, Core.VX.IDENTIFIER.nameSpace, 
            "value", "1"
        )
    	vs = g.V().hasLabel('Identifier').has("value", "1").toList()

    	then:
    	vs.size() == 2
    	carnival.checkConstraints().size() == 1

    }

    def "test checkConstraints for property existence constraints"() {
    	given:
    	def graph = carnival.graph
    	def g = graph.traversal()
    	def vert

    	expect:
    	carnival.checkConstraints().size() == 0

    	when:
    	vert = graph.addVertex(
            T.label, "IdentifierClass",
            Base.PX.NAME_SPACE.label, Core.VX.IDENTIFIER_CLASS.nameSpace
        )

    	then:
    	carnival.checkConstraints().size() == 3
    }


    def "test checkConstraints for relationship domain constraints"() {
        given:
        def graph = carnival.graph
        def g = graph.traversal()

        def identifierFacility
        def identifier
        def identifierClass
        def suitcase

        expect:
        carnival.checkConstraints().size() == 0

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
        carnival.checkConstraints().size() == 0

        when:
        Base.EX.IS_INSTANCE_OF.relate(g, identifier, identifierClass)
        //identifier.addEdge("is_instance_of", identifierClass)
        Core.EX.WAS_CREATED_BY.relate(g, identifier, identifierFacility)
        //identifier.addEdge("was_created_by", identifierFacility)

        then:
        carnival.checkConstraints().size() == 0

        when:
        suitcase.addEdge(Core.EX.WAS_CREATED_BY.label, identifierFacility, Base.PX.NAME_SPACE.label, Core.EX.WAS_CREATED_BY.nameSpace)
        //Core.EX.WAS_CREATED_BY.relate(g, suitcase, identifierFacility)
        //suitcase.addEdge("was_created_by", identifierFacility)

        then:
        carnival.checkConstraints().size() == 1
    }


    def "test checkConstraints for relationship range constraints"() {
        given:
        def graph = carnival.graph
        def g = graph.traversal()

        def identifierFacility
        def identifier
        def identifierClass
        def suitcase

        expect:
        carnival.checkConstraints().size() == 0

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
        carnival.checkConstraints().size() == 0

        when:
        Base.EX.IS_INSTANCE_OF.relate(g, identifier, identifierClass)
        Core.EX.WAS_CREATED_BY.relate(g, identifier, identifierFacility)
        //identifier.addEdge("is_instance_of", identifierClass)
        //identifier.addEdge("was_created_by", identifierFacility)

        then:
        carnival.checkConstraints().size() == 0

        when:
        //Core.EX.WAS_CREATED_BY.relate(g, identifier, suitcase)
        identifier.addEdge(Core.EX.WAS_CREATED_BY.label, suitcase, Base.PX.NAME_SPACE.label, Core.EX.WAS_CREATED_BY.nameSpace)
        //identifier.addEdge("was_created_by", suitcase)

        then:
        carnival.checkConstraints().size() == 1
    }


    def "test checkModel for unmodeled vertex and edge labels"() {
        given:
        def graph = carnival.graph
        def suitcase, salesman, id, idClass
        def g = graph.traversal()

        expect:
        !carnival.checkModel()

        when:
        id = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, '123').vertex(graph, g)
        idClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'idClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)
        Base.EX.IS_INSTANCE_OF.relate(g, id, idClass)

        then:
        CarnivalUtils.printGraph(g)
        carnival.checkModel().size() == 0

        when:
        suitcase = graph.addVertex(T.label, "Suitcase")
        salesman = graph.addVertex(T.label, "Salesman")

        then:
        carnival.checkModel().size() == 1
        println carnival.checkModel()

        when:
        suitcase.addEdge("belongs_to", salesman)

        then:
        carnival.checkModel().size() == 2
        println carnival.checkModel()
    }

    /*
    // Apparently the keys returned by "CALL db.propertyKeys()" consist of any key that was ever created...
    // if a vertex with a new key is created but then that vertex is deleted or the transaction is rolled back,
    // that key is still returned.  This causes tests to fail, so commenting out for now.
    */
    /*
    def "test checkModel for unmodeled vertex properties"() {
        given:
        def graph = carnival.graph
        def patient

        expect:
        !carnival.checkModel()

        when:
        patient = graph.addVertex(T.label, "Patient", "customProp", "abc123")

        then:
        carnival.checkModel().size() == 1
        println carnival.checkModel()
    }*/



    def "test Identifier vertex class"() {
        given:
        def graph = carnival.graph
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
        carnival.checkModel().size() == 0
        carnival.checkConstraints().size() == 0

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

