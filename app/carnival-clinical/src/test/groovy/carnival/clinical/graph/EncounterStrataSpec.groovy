package carnival.clinical.graph



import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.IgnoreIf

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.io.FileUtils

import org.apache.tinkerpop.gremlin.*
import org.apache.tinkerpop.gremlin.util.*
import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.groovy.loaders.*
import org.apache.tinkerpop.gremlin.neo4j.structure.*

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import com.google.common.collect.Range

import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import static carnival.core.graph.CoreGraphUtils.printVerts
import static carnival.core.graph.CoreGraphUtils.str
import static carnival.core.graph.CoreGraphUtils.printGraph
import static carnival.util.AppUtil.sysPropFalse

import carnival.core.*
import carnival.core.matcher.*
import carnival.util.CodeRefGroup
import carnival.util.CodeRef
import carnival.core.graph.CoreGraphNeo4j



/**
 * gradle test --tests "carnival.core.graph.EncounterStrataSpec"
 *
 *
 */
class EncounterStrataSpec extends Specification {

    static Logger log = LoggerFactory.getLogger('carnival')


    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
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


    void assertGraphConstraints() {
        assert coreGraph.checkConstraints().size() == 0
        assert coreGraph.checkModel().size() == 0
    }


    def "BmiEncounterStrata"() {
        given:
        def g = graph.traversal()
        def bmiRanges
        def encGroup 
        def enc
        def encs
        def strata
        def strataGroups
        def sg
        def bmi

        when:
        bmiRanges = []
        bmiRanges.add(Range.closed(25,30))
        bmiRanges.add(Range.openClosed(30,35))

        encGroup = graph.addVertex(T.label, 'EncounterGroup')
        
        enc = graph.addVertex(T.label, 'BiobankEncounter')
        // (enc)-[:has_conclusionated_bmi]->(d:BodyMassIndex)
        bmi = graph.addVertex(T.label, 'BodyMassIndex', 'value', 25)
        enc.addEdge('has_conclusionated_bmi', bmi)

        enc.addEdge('is_member_of', encGroup)

        strata = new BmiEncounterStrata(coreGraph, 'caps1', bmiRanges)
        strata.classify(encGroup)
        strataGroups = g.V(strata.vertex).out('contains_group').hasLabel('EncounterGroup').toList()
        strataGroups.each { log.debug("strataGroup: ${it.value('name')}") }

        then:
        strataGroups.size() == 3 // one for each age range and one for unspecified

        when:
        def sg2030 = strataGroups.find { it.value('name') == '[25..30]' }

        then:
        sg2030

        when:
        encs = g.V(sg2030).in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', '(30..35]')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 0

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', 'unspecified')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 0

        when:
        enc = graph.addVertex(T.label, 'BiobankEncounter')
        bmi = graph.addVertex(T.label, 'BodyMassIndex', 'value', 19)
        enc.addEdge('has_conclusionated_bmi', bmi)
        enc.addEdge('is_member_of', encGroup)

        strata = new BmiEncounterStrata(coreGraph, 'caps2', bmiRanges)
        strata.classify(encGroup)
        strataGroups = g.V(strata.vertex).out('contains_group').hasLabel('EncounterGroup').toList()
        strataGroups.each { log.debug("strataGroup: ${it.value('name')}") }

        then:
        strata
        strata.vertex

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', '[25..30]')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', '(30..35]')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 0

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', 'unspecified')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1
    }



    def "AgeEncounterStrata"() {
        given:
        def g = graph.traversal()
        def ageRanges
        def encGroup 
        def enc
        def encs
        def strata
        def strataGroups
        def sg

        when:
        ageRanges = []
        ageRanges.add(Range.closed(20,30))
        ageRanges.add(Range.openClosed(30,40))

        encGroup = graph.addVertex(T.label, 'EncounterGroup')
        
        enc = graph.addVertex(T.label, 'BiobankEncounter', 'ageOfPatient', '20')
        enc.addEdge('is_member_of', encGroup)

        strata = new AgeEncounterStrata(coreGraph, 'caps1', ageRanges)
        strata.classify(encGroup)
        strataGroups = g.V(strata.vertex).out('contains_group').hasLabel('EncounterGroup').toList()
        strataGroups.each { log.debug("strataGroup: ${it.value('name')}") }

        then:
        strataGroups.size() == 3 // one for each age range and one for unspecified

        when:
        def sg2030 = strataGroups.find { it.value('name') == '[20..30]' }

        then:
        sg2030

        when:
        encs = g.V(sg2030).in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', '(30..40]')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 0

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', 'unspecified')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 0

        when:
        enc = graph.addVertex(T.label, 'BiobankEncounter', 'ageOfPatient', '19')
        enc.addEdge('is_member_of', encGroup)

        strata = new AgeEncounterStrata(coreGraph, 'caps2', ageRanges)
        strata.classify(encGroup)
        strataGroups = g.V(strata.vertex).out('contains_group').hasLabel('EncounterGroup').toList()
        strataGroups.each { log.debug("strataGroup: ${it.value('name')}") }

        then:
        strata
        strata.vertex

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', '[20..30]')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', '(30..40]')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 0

        when:
        encs = g.V(strata.vertex)
            .out('contains_group').has('name', 'unspecified')
            .in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1
    }

    

    def "EncounterStrataGroup"() {
        given:
        def g = graph.traversal()

        when:
        def patientGroupStrataV = graph.addVertex(T.label, 'EncounterStrata', 'name', 'pgs1')
        def patientStrataGroup = new EncounterStrataGroup(coreGraph, 'psg1', patientGroupStrataV)

        then:
        patientStrataGroup
        patientStrataGroup.name == 'psg1'
        g.V(patientStrataGroup.vertex).in('contains_group').hasLabel('EncounterStrata').next() == patientGroupStrataV
    }




}





