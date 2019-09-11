package carnival.clinical.graph.domain


import groovy.sql.*
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.step.*
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import org.apache.commons.io.FileUtils

import static carnival.core.graph.CoreGraphUtils.printVerts
import static carnival.core.graph.CoreGraphUtils.str
import static carnival.core.graph.CoreGraphUtils.printGraph
import static carnival.util.AppUtil.sysPropFalse

import carnival.core.graph.Core
import carnival.core.graph.CoreGraph
import carnival.core.graph.CoreGraphNeo4j
import carnival.util.KeyType


/**
 * gradle test --tests "carnival.clinical.graph.domain.EncounterGroupSpec"
 *
 *
 */
class EncounterGroupSpec extends Specification {


    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */


    ///////////////////////////////////////////////////////////////////////////
    // SHARED RESOURCES
    ///////////////////////////////////////////////////////////////////////////
    @Shared coreGraph
    @Shared graph



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////

    def setupSpec() {
        // Neo4j
        System.setProperty('log4j.configuration', 'log4j.properties')

        // create pmbb graph
        println "init neo4j graph..."
        CoreGraphNeo4j.clearGraph()
        coreGraph = CoreGraphNeo4j.create()
        graph = coreGraph.graph

        // validate the base graph
        assert coreGraph.checkConstraints().size() == 0
        assert coreGraph.checkModel().size() == 0
    } 


    def cleanupSpec() {
        if (graph) graph.close()
    }


    def cleanup() {
        if (graph) {
            if (sysPropFalse('test.graph.rollback')) graph.tx().commit()
            else graph.tx().rollback() 
        }
    }


    Vertex packetIdClassVertex(GraphTraversalSource g) {
        g.V().hasLabel('IdentifierClass').has('name', 'encounter_pack_id').tryNext().orElseGet {
            graph.addVertex(T.label, 'IdentifierClass', 'name', 'encounter_pack_id')
        }
    } 


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "createEncounterGroupFromIdValues with name"() {
        given:
        def g = graph.traversal()
        def packetIdClass = packetIdClassVertex(g)
        def enc
        def id
        def grp
        def encs

        when:
        id = graph.addVertex(T.label, 'Identifier', 'value', 'i1')
        id.addEdge('is_instance_of', packetIdClass)
        enc = graph.addVertex(T.label, 'BiobankEncounter')
        enc.addEdge('is_identified_by', id)

        grp = EncounterGroup.createEncounterGroupFromIdValues(graph, 'n1', KeyType.ENCOUNTER_PACK_ID, ['i1'])

        then:
        grp
        grp instanceof Vertex
        grp.value('name') == 'n1'
    }


    def "createEncounterGroupFromIdValues basic"() {
        given:
        def g = graph.traversal()
        def packetIdClass = packetIdClassVertex(g)
        def enc
        def id
        def grp
        def encs

        when:
        id = graph.addVertex(T.label, 'Identifier', 'value', 'i1')
        id.addEdge('is_instance_of', packetIdClass)
        enc = graph.addVertex(T.label, 'BiobankEncounter')
        enc.addEdge('is_identified_by', id)

        grp = EncounterGroup.createEncounterGroupFromIdValues(graph, KeyType.ENCOUNTER_PACK_ID, ['i1'])

        then:
        grp
        grp instanceof Vertex

        when:
        encs = g.V(grp).in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs
        encs.size() == 1
        encs[0] == enc

        when:
        id = graph.addVertex(T.label, 'Identifier', 'value', 'i2')
        id.addEdge('is_instance_of', packetIdClass)
        enc = graph.addVertex(T.label, 'BiobankEncounter')
        enc.addEdge('is_identified_by', id)

        grp = EncounterGroup.createEncounterGroupFromIdValues(graph, KeyType.ENCOUNTER_PACK_ID, ['i1'])
        encs = g.V(grp).in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1
        g.V(encs[0]).out('is_identified_by').next().value('value') == 'i1'

        when:
        grp = EncounterGroup.createEncounterGroupFromIdValues(graph, KeyType.ENCOUNTER_PACK_ID, ['i1', 'i2'])
        encs = g.V(grp).in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 2
        g.V(encs).out('is_identified_by').has('value', 'i1').next()
        g.V(encs).out('is_identified_by').has('value', 'i2').next()
    }

}


