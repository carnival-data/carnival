package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of



/**
 * gradle test --tests "carnival.core.graph.CoreGraphUtilsSpec"
 */
class CoreGraphUtilsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared coreGraph
    @Shared controlledInstances = [
    	new VertexInstanceDefinition(label:"Identifier", properties:[value:"1"]),
    	new VertexInstanceDefinition(label:"Identifier", properties:[value:"2"]),
    	new VertexInstanceDefinition(label:"CaseReportForm", properties:[futureEmrAllowed:true]),
        new VertexInstanceDefinition(label:"IdentifierClass", properties:[name:"testIdClass", hasScope:true, hasCreationFacility:false])
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
    def "test populateIdMap for existing ids"() {
        given:
        def graph = coreGraph.graph
        def g = coreGraph.graph.traversal()
        
        def idClass = g.V().hasLabel('IdentifierClass').has('name', 'testIdClass').next()
        def patient = graph.addVertex(T.label, 'Patient')
        def patient2 = graph.addVertex(T.label, 'Patient')
        def encounter = graph.addVertex(T.label, 'BiobankEncounter')

        def res

        expect:
        CoreGraphUtils.populateIdMap(graph, idClass).size() == 0

        when:
        def packetId = graph.addVertex(T.label, 'Identifier', 'value', 'texmex')
        packetId.addEdge('is_instance_of', idClass)
        res = CoreGraphUtils.populateIdMap(graph, idClass)

        then:
        res.size() == 1
        res["texmex"]
        res["texmex"].containsKey('patients')
        res["texmex"].containsKey('ids')
        res["texmex"].patients.size() == 0
        res["texmex"].biobankEncounters.size() == 0
        res["texmex"].ids.size() == 1

        when:
        encounter.addEdge('is_identified_by', packetId)
        res = CoreGraphUtils.populateIdMap(graph, idClass)

        then:
        res.size() == 1
        res["texmex"]
        res["texmex"].containsKey('patients')
        res["texmex"].containsKey('ids')
        res["texmex"].patients.size() == 0
        res["texmex"].biobankEncounters == [encounter]
        res["texmex"].ids.size() == 1

        when:
        patient.addEdge('is_identified_by', packetId)
        res = CoreGraphUtils.populateIdMap(graph, idClass)

        then:
        res.size() == 1
        res["texmex"]
        res["texmex"].containsKey('patients')
        res["texmex"].containsKey('ids')
        res["texmex"].patients == [patient]
        res["texmex"].biobankEncounters == [encounter]
        res["texmex"].ids.size() == 1

        when:
        patient2.addEdge('is_identified_by', packetId)
        res = CoreGraphUtils.populateIdMap(graph, idClass)

        then:
        res.size() == 1
        res["texmex"]
        res["texmex"].containsKey('patients')
        res["texmex"].containsKey('ids')
        res["texmex"].patients.contains(patient)
        res["texmex"].patients.contains(patient2) 
        res["texmex"].biobankEncounters == [encounter]
        res["texmex"].ids.size() == 1
        println res

        when:
        def packetId2 = graph.addVertex(T.label, 'Identifier', 'value', 'texmex2')
        packetId2.addEdge('is_instance_of', idClass)
        res = CoreGraphUtils.populateIdMap(graph, idClass)

        then:
        res.size() == 2
        res["texmex"]
        res["texmex2"]
        println res

        when: 
        def scope1 = graph.addVertex(T.label, 'IdentifierScope', 'name', 'scope1')
        def scope2 = graph.addVertex(T.label, 'IdentifierScope', 'name', 'scope2')
        packetId.addEdge('is_scoped_by', scope1)
        packetId2.addEdge('is_scoped_by', scope2)
        res = CoreGraphUtils.populateIdMap(graph, idClass)
        def res1 = CoreGraphUtils.populateIdMap(graph, idClass, scope1)
        def res2 = CoreGraphUtils.populateIdMap(graph, idClass, scope2)

        then:
        res.size() == 2
        res["texmex"]
        res["texmex2"]

        res1.size() == 1
        res1.containsKey("texmex")
        !res1.containsKey("texmex2")

        res2.size() == 1
        !res2.containsKey("texmex")
        res2.containsKey("texmex2")

        println res
        println res1
        println res2
    }

    def "test populatePatientIdMap for existing ids"() {
        given:
        def graph = coreGraph.graph
        def g = coreGraph.graph.traversal()

        //graph.addVertex(T.label, 'IdentifierClass', 'value', 'testIdClass', 'hasScope', 'false', 'hasCreationFacility', 'false')
        
        def idClass = g.V().hasLabel('IdentifierClass').has('name', 'testIdClass').next()
        def patient = graph.addVertex(T.label, 'Patient')
        def encounter = graph.addVertex(T.label, 'BiobankEncounter')

        def res

        expect:
        CoreGraphUtils.populatePatientIdMap(graph, idClass).size() == 0

        when:
        def packetId = graph.addVertex(T.label, 'Identifier', 'value', 'texmex')
        packetId.addEdge('is_instance_of', idClass)
        res = CoreGraphUtils.populatePatientIdMap(graph, idClass)

        then:
        res.size() == 1
        !res["texmex"].containsKey('patient')
        res["texmex"].containsKey('id')

        when:
        encounter.addEdge('is_identified_by', packetId)
        res = CoreGraphUtils.populatePatientIdMap(graph, idClass)

        then:
        res.size() == 1
        !res["texmex"].containsKey('patient')
        res["texmex"].containsKey('id')

        when:
        patient.addEdge('is_identified_by', packetId)
        res = CoreGraphUtils.populatePatientIdMap(graph, idClass)

        then:
        res.size() == 1
        res["texmex"].containsKey('patient')
        res["texmex"].containsKey('id')

    }

    def "test print graph"() {
        given:
        def graph = coreGraph.graph
        def g = coreGraph.graph.traversal()

        expect:
        CoreGraphUtils.printGraph(g)
    }
}

