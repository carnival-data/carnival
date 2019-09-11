package carnival.clinical.graph.domain


import groovy.sql.*
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

import org.apache.commons.io.FileUtils

import static carnival.core.graph.CoreGraphUtils.printVerts
import static carnival.core.graph.CoreGraphUtils.str
import static carnival.core.graph.CoreGraphUtils.printGraph
import static carnival.util.AppUtil.sysPropFalse

import carnival.core.graph.Core
import carnival.core.graph.CoreGraph
import carnival.core.graph.CoreGraphNeo4j
import carnival.core.graph.CarnivalTraversalSource
import carnival.clinical.util.TestUtils


/**
 * gradle test --tests "carnival.clinical.graph.domain.PatientGroupSpec"
 *
 *
 */
class PatientGroupSpec extends Specification {


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



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "patientLookupGenericId basic"() {
        given:
        def g = graph.traversal(CarnivalTraversalSource.class)
        def args
        def res
        def cypher

        def ids = (1..3).toList()
        def idFacility = "patientLookupGenericId-basic"
        def patVs = TestUtils.createPatients(graph:graph, g:g, idClass:"generic_patient_id", idFacilityName:idFacility, ids:ids)
        printVerts(patVs, 'createPatients')

        when:
        res = PatientGroup.patientLookupGenericId(graph, idFacility, patVs)

        then:
        res.size() == 3
        res.get('1') == g.patientsIdentifiedByGenericId(idFacility, '1').toList().first()
        res.get('2') == g.patientsIdentifiedByGenericId(idFacility, '2').toList().first()
        res.get('3') == g.patientsIdentifiedByGenericId(idFacility, '3').toList().first()
    }


    def "patientLookupMrn basic"() {
        given:
        def g = graph.traversal(CarnivalTraversalSource.class)
        def args
        def res
        def cypher

        def ids = (1..3).toList()
        def idFacility = "patientLookupMrn-basic"
        def patVs = TestUtils.createPatients(graph:graph, g:g, idClass:"mrn", idFacilityName:idFacility, ids:ids)
        printVerts(patVs, 'createPatients')

        when:
        res = PatientGroup.patientLookupMrn(graph, idFacility, patVs)

        then:
        res.size() == 3
        res.get(1) == g.patientsIdentifiedByMrn(idFacility, 1).toList().first()
        res.get(2) == g.patientsIdentifiedByMrn(idFacility, 2).toList().first()
        res.get(3) == g.patientsIdentifiedByMrn(idFacility, 3).toList().first()
    }


    def "patientLookupPacketId basic"() {
        given:
        def g = graph.traversal(CarnivalTraversalSource.class)
        def args
        def res
        def cypher

        def ids = (1..3).toList()
        def patVs = TestUtils.createPatients(graph:graph, g:g, idClass:"encounter_pack_id", ids:ids)
        printVerts(patVs, 'createPatients')

        when:
        res = PatientGroup.patientLookupPacketId(graph, patVs)

        then:
        res.size() == 3
        res.get('1') == g.patientsIdentifiedByPacketId('1').toList().first()
        res.get('2') == g.patientsIdentifiedByPacketId('2').toList().first()
        res.get('3') == g.patientsIdentifiedByPacketId('3').toList().first()
    }


    def "patientLookupEmpi basic"() {
        given:
        def g = graph.traversal(CarnivalTraversalSource.class)
        def args
        def res
        def cypher

        def ids = (1..3).toList()
        def patVs = TestUtils.createPatients(graph:graph, g:g, idClass:"empi", ids:ids)
        printVerts(patVs, 'createPatients')

        when:
        res = PatientGroup.patientLookupEmpi(graph, patVs)

        then:
        res.size() == 3
        res.get('1') == g.patientsIdentifiedByEmpi('1').toList().first()
        res.get('2') == g.patientsIdentifiedByEmpi('2').toList().first()
        res.get('3') == g.patientsIdentifiedByEmpi('3').toList().first()
    }

}


