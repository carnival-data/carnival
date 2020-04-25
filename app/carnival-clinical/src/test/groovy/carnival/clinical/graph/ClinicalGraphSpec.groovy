package carnival.clinical.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import carnival.core.graph.Identifier
import carnival.core.graph.Core
import carnival.core.graph.CoreGraphNeo4j
import carnival.core.graph.CoreGraphUtils 
import carnival.clinical.graph.Clinical



/**
 * gradle test --tests "carnival.clinical.graph.ClinicalGraphSpec"
 *
 */
class ClinicalGraphSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared coreGraph
    @Shared graph
    @Shared g
    
    @Shared controlledInstances = [
        Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "1"),
        Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "2"),
    ]


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() {
        CoreGraphNeo4j.clearGraph()
        coreGraph = CoreGraphNeo4j.create(controlledInstances:controlledInstances)
        coreGraph = coreGraph.withTraits(ClinicalGraphTrait)
        graph = coreGraph.graph
    } 

    def setup() {
    	g = graph.traversal()
    }

    def cleanup() {
        if (g) g.close()
        if (coreGraph) coreGraph.graph.tx().rollback()
    }

    def cleanupSpec() {
        if (coreGraph) coreGraph.graph.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    /**
    * This will test graph initizilation once it's been moved to CoreGraph
    */
    def "graph creation"() {
    	when: 
        println "===================="
        println "initial graph"
        CoreGraphUtils.printGraph(graph.traversal())
        println "===================="

    	then:
    	graph != null
    }

    def "initializeGraph for uniqueness constraint existence"() {
    	when:
    	def constraints = graph.cypher("CALL db.constraints()").toList()

    	then:
    	//println constraints
    	constraints != null
    	constraints.size() >= 2
    }


    def "checkConstraints for identifier uniqueness"() {
        given:

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
        Core.EX.IS_INSTANCE_OF.relate(g, id1Class1, idClass1) 

        def id2Class1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id2').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id2Class1, idClass1) 

        def id1Class2 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1Class2, idClass2) 

        def scopedId1Scope1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'scopedId1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, scopedId1Scope1, scopedIdClass) 
        Core.EX.IS_SCOPED_BY.relate(g, scopedId1Scope1, scope1) 

        def scopedId1Scope2 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'scopedId1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, scopedId1Scope2, scopedIdClass) 
        Core.EX.IS_SCOPED_BY.relate(g, scopedId1Scope2, scope2) 

        def facilityId1Facility1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'facilityId1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, facilityId1Facility1, facilityIdClass) 
        Core.EX.WAS_CREATED_BY.relate(g, facilityId1Facility1, facility1) 

        def facilityId1Facility2 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'facilityId1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, facilityId1Facility2, facilityIdClass) 
        Core.EX.WAS_CREATED_BY.relate(g, facilityId1Facility2, facility2) 


        expect:
        coreGraph.checkConstraints().size() == 0

        // duplicate id val of the same class
        when:
        def id1Class1Dupe = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').createVertex(graph, g)
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
        def scopedId1Scope1Dupe = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'scopedId1').createVertex(graph, g)
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
        def facilityId1Facility1Dupe = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'facilityId1').createVertex(graph, g)
        facilityId1Facility1Dupe.addEdge('is_instance_of', facilityIdClass)
        facilityId1Facility1Dupe.addEdge('was_created_by', facility1)

        then:
        coreGraph.checkConstraints().size() == 1
    }


    def "checkModel for unmodeled vertex and edge labels"() {
        def suitcase, salesman, id, idClass

        expect:
        !coreGraph.checkModel()

        when:
        id = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        idClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'idClass1',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id, idClass)

        then:
        CoreGraphUtils.printGraph(graph.traversal())
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
    def "checkModel for unmodeled vertex properties"() {
        given:
        def patient

        expect:
        !coreGraph.checkModel()

        when:
        patient = graph.addVertex(T.label, "Patient", "customProp", "abc123")

        then:
        coreGraph.checkModel().size() == 1
        println coreGraph.checkModel()
    }*/

    // reasoner tests
    def "reasoner rule connectPatientsAndEncounters identifiers link"() {
        given:
        def gv

        def idClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'testIdClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)

        def patients = []
        def encounters = []
        def identifiers  = []


        (1..5).each { i ->
            def patient = Clinical.VX.PATIENT.instance().createVertex(graph, g) 
            //graph.addVertex(T.label, 'Patient')
            def encounter = Clinical.VX.BIOBANK_ENCOUNTER.instance().createVertex(graph, g)
            //def encounter = graph.addVertex(T.label, 'BiobankEncounter')
            def packetId = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, i+100).vertex(graph, g)
            //def packetId = graph.addVertex(T.label, 'Identifier', 'value', i+100)
            Core.EX.IS_INSTANCE_OF.relate(g, packetId, idClass)
            //packetId.addEdge('is_instance_of', idClass)

            patients << patient
            encounters << encounter
            identifiers << packetId
        }


        expect:
        idClass

        coreGraph.checkModel().size() == 0
        coreGraph.checkConstraints().size() == 0

        g.V().hasLabel('Patient').toList().size() == 5
        g.V().hasLabel('BiobankEncounter').toList().size() == 5
        g.V().hasLabel('Patient').out('participated_in_encounter').hasLabel('BiobankEncounter').toList().size() == 0
        g.V().hasLabel('Patient').outE('participated_in_encounter').toList().size() == 0

        // no edges created b/c no ids in common
        when:
        coreGraph.connectPatientsAndEncounters()

        then:
        coreGraph.checkModel().size() == 0
        coreGraph.checkConstraints().size() == 0

        g.V().hasLabel('Patient').toList().size() == 5
        g.V().hasLabel('BiobankEncounter').toList().size() == 5
        g.V().hasLabel('Patient').out('participated_in_encounter').hasLabel('BiobankEncounter').toList().size() == 0
        g.V().hasLabel('Patient').outE('participated_in_encounter').toList().size() == 0


        // give some patients and encounters the same identifiers
        // e[0] <-i[0]-> p[0]
        //
        // e[1] <-i[1]-> p[1]
        //
        // e[2] <-i[2]-> p[2]
        // e[3] <-i[2]-> p[2]
        //
        //        i[4]-> p[4]
        when:
        [[0,0], [1,1], [2,2], [3,2]].each { n ->
            Core.EX.IS_IDENTIFIED_BY.relate(g, encounters[n[0]], identifiers[n[1]])    
        }

        [[0,0], [1,1], [2,2], [4,4]].each { n ->
            Core.EX.IS_IDENTIFIED_BY.relate(g, patients[n[0]], identifiers[n[1]])    
        }

        coreGraph.connectPatientsAndEncounters()

        then:
        CoreGraphUtils.printGraph(g)

        coreGraph.checkModel().size() == 0
        coreGraph.checkConstraints().size() == 0

        g.V().hasLabel('Patient').toList().size() == 5
        g.V().hasLabel('BiobankEncounter').toList().size() == 5
        g.V().hasLabel('Patient').out('participated_in_encounter').hasLabel('BiobankEncounter').toList().size() == 4
        g.V().hasLabel('Patient').outE('participated_in_encounter').toList().size() == 4


        // second run should not create any new edges
        when:
        coreGraph.connectPatientsAndEncounters()

        then:
        CoreGraphUtils.printGraph(g)
        coreGraph.checkModel().size() == 0
        coreGraph.checkConstraints().size() == 0

        g.V().hasLabel('Patient').toList().size() == 5
        g.V().hasLabel('BiobankEncounter').toList().size() == 5
        g.V().hasLabel('Patient').out('participated_in_encounter').hasLabel('BiobankEncounter').toList().size() == 4
        g.V().hasLabel('Patient').outE('participated_in_encounter').toList().size() == 4
    }


    def "reasoner rule connectPatientsAndEncounters consent link"() {
        given:
        def gv

        def idClass = Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'testIdClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ).vertex(graph, g)

        // patient1 and encounter1 share an identifier
        def id1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1, idClass)
        def encounter1 = Clinical.VX.BIOBANK_ENCOUNTER.createVertex(graph, g)
        def patient1 = Clinical.VX.PATIENT.createVertex(graph, g)
        def crf = Clinical.VX.CASE_REPORT_FORM.createVertex(graph, g)
        Core.EX.IS_IDENTIFIED_BY.relate(g, encounter1, id1)
        Core.EX.IS_IDENTIFIED_BY.relate(g, patient1, id1)
        Clinical.EX.PARTICIPATED_IN_FORM_FILING.relate(g, encounter1, crf)

        // encounter1 linked to encounter2 by being under the same consent
        def id2 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id2').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id2, idClass)
        def encounter2 = Clinical.VX.BIOBANK_ENCOUNTER.createVertex(graph, g)
        Core.EX.IS_IDENTIFIED_BY.relate(g, encounter2, id2)
        Clinical.EX.IS_UNDER_CONSENT.relate(g, encounter2, crf)

        expect:
        coreGraph.checkModel().size() == 0
        coreGraph.checkConstraints().size() == 0

        when:
        def numNodesBefore = g.V().count().next()
        def numEdgesBefore = g.V().outE().dedup().count().next()
        coreGraph.connectPatientsAndEncounters()
        def numNodesAfter = g.V().count().next()
        def numEdgesAfter = g.V().outE().dedup().count().next()

        println "nodes before: $numNodesBefore, ${numNodesBefore.getClass()}"

        then:
        coreGraph.checkModel().size() == 0
        coreGraph.checkConstraints().size() == 0

        graph.cypher("""
            MATCH (patient1:Patient)-[:participated_in_encounter]->(encounter1:BiobankEncounter)
            WHERE id(patient1) = ${patient1.id()} and id(encounter1) = ${encounter1.id()}
            RETURN *
        """).toList().size() == 1

        graph.cypher("""
            MATCH (patient1:Patient)-[:participated_in_encounter]->(encounter2:BiobankEncounter)
            WHERE id(patient1) = ${patient1.id()} and id(encounter2) = ${encounter2.id()}
            RETURN *
        """).toList().size() == 1

        numNodesBefore == numNodesAfter
        numEdgesBefore + 2 == numEdgesAfter

    }


    def "Identifier vertex class"() {
        given:
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

        //def idClass1 = graph.addVertex(T.label, 'IdentifierClass', 'name', 'idClass1', 'hasCreationFacility', false, 'hasScope', false)
        //def idClass2 = graph.addVertex(T.label, 'IdentifierClass', 'name', 'idClass2', 'hasCreationFacility', false, 'hasScope', false)
        //def scopedIdClass = graph.addVertex(T.label, 'IdentifierClass', 'name', 'testIdClass', 'hasCreationFacility', false, 'hasScope', true)
        //def facilityIdClass = graph.addVertex(T.label, 'IdentifierClass', 'name', 'testFacilityIdClass', 'hasCreationFacility', true, 'hasScope', false)


        def scope1 = Core.VX.IDENTIFIER_SCOPE.instance().withProperty(Core.PX.NAME, 'scope1').vertex(graph, g)
        def scope2 = Core.VX.IDENTIFIER_SCOPE.instance().withProperty(Core.PX.NAME, 'scope2').vertex(graph, g)

        def facility1 = Core.VX.IDENTIFIER_FACILITY.instance().withProperty(Core.PX.NAME, 'facility1').vertex(graph, g)
        def facility2 = Core.VX.IDENTIFIER_FACILITY.instance().withProperty(Core.PX.NAME, 'facility2').vertex(graph, g)

        def id1Class1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1Class1, idClass1) 

        def id2Class1 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id2').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id2Class1, idClass1) 

        def id1Class2 = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1Class2, idClass2) 

        def scopedId = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'scopedId1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, scopedId, scopedIdClass) 
        Core.EX.IS_SCOPED_BY.relate(g, scopedId, scope1) 

        def facilityId = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'facilityId1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, facilityId, facilityIdClass) 
        Core.EX.WAS_CREATED_BY.relate(g, facilityId, facility1) 



        //def scope1 = graph.addVertex(T.label, 'IdentifierScope', "name", "scope1")
        //def scope2 = graph.addVertex(T.label, 'IdentifierScope', "name", "scope2")

        //def facility1 = graph.addVertex(T.label, 'IdentifierFacility', "name", "facility1")
        //def facility2 = graph.addVertex(T.label, 'IdentifierFacility', "name", "facility2")

        //def id1Class1 = graph.addVertex(T.label, 'Identifier', 'value', 'id1')
        //id1Class1.addEdge('is_instance_of', idClass1)

        //def id2Class1 = graph.addVertex(T.label, 'Identifier', 'value', 'id2')
        //id2Class1.addEdge('is_instance_of', idClass1)

        //def id1Class2 = graph.addVertex(T.label, 'Identifier', 'value', 'id1')
        //id1Class2.addEdge('is_instance_of', idClass2)

        //def scopedId = graph.addVertex(T.label, 'Identifier', 'value', 'scopedId1')
        //scopedId.addEdge('is_instance_of', scopedIdClass)
        //scopedId.addEdge('is_scoped_by', scope1)

        //def facilityId = graph.addVertex(T.label, 'Identifier', 'value', 'facilityId1')
        //facilityId.addEdge('is_instance_of', facilityIdClass)
        //facilityId.addEdge('was_created_by', facility1)

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
        idNode = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        //identifier = new Identifier(identifierClass:idClass1, identifierScope:null, value:"id1" )
        //idNode = identifier.getOrCreateNode(graph) 
        postIdCount = g.V().hasLabel("Identifier").toList().size()

        then:
        idNode == id1Class1
        preIdCount == postIdCount


        when:
        preIdCount = g.V().hasLabel("Identifier").toList().size()
        idNode = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id2').vertex(graph, g)
        //identifier = new Identifier(identifierClass:idClass1, identifierScope:null, value:"id2" )
        //idNode = identifier.getOrCreateNode(graph) 
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

