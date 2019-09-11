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
import carnival.core.vine.CachingVine
import carnival.core.vine.CachingVine.CacheMode
import carnival.util.CodeRefGroup
import carnival.util.CodeRef
import carnival.core.graph.CoreGraphNeo4j



/**
 * gradle test --tests "carnival.core.graph.EncounterMatcherSpec"
 *
 *
 */
class EncounterMatcherSpec extends Specification {


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


    void assertGraphConstraints() {
        assert coreGraph.checkConstraints().size() == 0
        assert coreGraph.checkModel().size() == 0
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////


    def "age and bmi"() {
        given:
        def g = graph.traversal()
        def bmiRanges
        def ageRanges
        def encGroup 
        def enc
        def encs
        def bmiStrata
        def ageStrata
        def strataGroups
        def sg
        def bmi
        def cohorts
        def criteria
        def pat

        when:
        bmiRanges = []
        bmiRanges.add(Range.closed(25,30))
        bmiRanges.add(Range.openClosed(30,35))

        ageRanges = []
        ageRanges.add(Range.closed(20,30))
        ageRanges.add(Range.openClosed(30,40))

        encGroup = graph.addVertex(T.label, 'EncounterGroup', 'name', 'age and bmi test encounter group')
        
        enc = graph.addVertex(T.label, 'BiobankEncounter', 'ageOfPatient', '20')
        bmi = graph.addVertex(T.label, 'BodyMassIndex', 'value', 25)
        enc.addEdge('has_conclusionated_bmi', bmi)
        enc.addEdge('is_member_of', encGroup)
        pat = graph.addVertex(T.label, 'Patient')
        pat.addEdge('participated_in_encounter', enc)

        bmiStrata = new BmiEncounterStrata(coreGraph, 'bmi1', bmiRanges)
        bmiStrata.classify(encGroup)
        strataGroups = g.V(bmiStrata.vertex).out('contains_group').hasLabel('EncounterGroup').toList()
        strataGroups.each { println "${bmiStrata.name} strataGroup: ${it.value('name')} ${g.V(it).in('is_member_of').toList()}" }

        ageStrata = new AgeEncounterStrata(coreGraph, 'age1', ageRanges)
        ageStrata.classify(encGroup)
        strataGroups = g.V(ageStrata.vertex).out('contains_group').hasLabel('EncounterGroup').toList()
        strataGroups.each { println "${ageStrata.name} strataGroup: ${it.value('name')} ${g.V(it).in('is_member_of').toList()}" }

        criteria = [
            [
                criteriaGroups:[
                    ageStrata.getGroupVertex(Range.closed(20,30)),
                    bmiStrata.getGroupVertex(Range.closed(25,30))
                ],
                count:1
            ]
        ]
        cohorts = EncounterMatcher.generateCohortGroup(
            coreGraph:coreGraph,
            cohortName:'cohort1',
            candidatesGroup:encGroup,
            criteria:criteria
        )
        
        println "cohorts: $cohorts"

        then:
        cohorts
        cohorts.potCohortGroup
        cohorts.selCohortGroup

        when:
        g.V(cohorts.potCohortGroup).in().each { println "p ${cohorts.potCohortGroup.value('name')} ${it.label()}" }
        g.V(cohorts.selCohortGroup).in().each { println "s ${cohorts.selCohortGroup.value('name')} ${it.label()}" }

        encs = g.V(cohorts.potCohortGroup).in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1

        when:
        encs = g.V(cohorts.selCohortGroup).in('is_member_of').hasLabel('BiobankEncounter').toList()

        then:
        encs.size() == 1

    }




}





