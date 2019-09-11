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
 * gradle test --tests "carnival.core.graph.CoreGraphGraphModelExtensionSpec"
 *
 */
class CoreGraphGraphModelExtensionSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared coreGraph
    
    @Shared controlledInstances = [
        Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "1"),
        Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "2"),
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
    
    static enum VX implements VertexDefTrait {
        PORTAL_GUN,
        RICKS_LAB

        private VX() {}
        private VX(Map m) { if (m.vertexProperties) this.vertexProperties = m.vertexProperties }
    }


    static enum EX implements EdgeDefTrait {
        WAS_CREATED_BY (domain:[VX.PORTAL_GUN], range:[VX.RICKS_LAB])

        private EX() {}
        private EX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    def "relationship domain extension"() {
        
        // add a relatiohship for was_created_by that allows suitcase in the comain
        // final check should pass

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
        identifierFacility = graph.addVertex(T.label, "IdentifierFacility", "name", "f1")
        identifier = graph.addVertex(T.label, "Identifier", "value", "abc123")
        identifierClass = graph.addVertex(T.label, "IdentifierClass", "name", "testIdClass", "hasScope", false, "hasCreationFacility", true)
        suitcase = graph.addVertex(T.label, "PortalGun")

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        identifier.addEdge("is_instance_of", identifierClass)
        identifier.addEdge("was_created_by", identifierFacility)

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        suitcase.addEdge("was_created_by", identifierFacility)

        then:
        coreGraph.checkConstraints().size() == 0
    }


    def "relationship range extension"() {
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
        identifierFacility = graph.addVertex(T.label, "IdentifierFacility", "name", "f1")
        identifier = graph.addVertex(T.label, "Identifier", "value", "abc123")
        identifierClass = graph.addVertex(T.label, "IdentifierClass", "name", "testIdClass", "hasScope", false, "hasCreationFacility", true)
        suitcase = graph.addVertex(T.label, "RicksLab")

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        identifier.addEdge("is_instance_of", identifierClass)
        identifier.addEdge("was_created_by", identifierFacility)

        then:
        coreGraph.checkConstraints().size() == 0

        when:
        identifier.addEdge("was_created_by", suitcase)

        then:
        coreGraph.checkConstraints().size() == 0
    }
    
}

