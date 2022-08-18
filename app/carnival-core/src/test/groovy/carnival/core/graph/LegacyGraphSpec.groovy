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
class LegacyGraphSpec extends Specification {

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
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() { } 

    def setup() {
        carnival = CarnivalTinker.create()
        carnival.graphValidator = new LegacyValidator()
    }

    def cleanup() {
        if (carnival) carnival.close()
    }

    def cleanupSpec() { }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////
    

    def "test checkConstraints for identifier uniqueness"() {
        given:
        def graph = carnival.graph
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
        carnival.checkConstraints().size() == 0

        // duplicate id val of the same class
        when:
        def id1Class1Dupe = graph.addVertex(T.label, 'Identifier', 'value', 'id1')
        id1Class1Dupe.addEdge('is_instance_of', idClass1)

        then:
        carnival.checkConstraints().size() == 1

        // reset
        when:
        id1Class1Dupe.remove()

        then:
        carnival.checkConstraints().size() == 0


        //duplicate id val of the same class, same scope
        when:
        def scopedId1Scope1Dupe = graph.addVertex(T.label, 'Identifier', 'value', 'scopedId1')
        scopedId1Scope1Dupe.addEdge('is_instance_of', scopedIdClass)
        scopedId1Scope1Dupe.addEdge('is_scoped_by', scope1)


        then:
        carnival.checkConstraints().size() == 1

        // reset
        when:
        scopedId1Scope1Dupe.remove()

        then:
        carnival.checkConstraints().size() == 0


        //duplicate id val of the same class, same facility
        when:
        def facilityId1Facility1Dupe = graph.addVertex(T.label, 'Identifier', 'value', 'facilityId1')
        facilityId1Facility1Dupe.addEdge('is_instance_of', facilityIdClass)
        facilityId1Facility1Dupe.addEdge('was_created_by', facility1)

        then:
        carnival.checkConstraints().size() == 1

    }

}

