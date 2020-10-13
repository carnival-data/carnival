package carnival.core.graph.reasoner



import groovy.sql.*

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T

import carnival.core.graph.CoreGraphSpecification



/**
 * gradle test --tests "carnival.core.graph.reasoner.PersonEncounterLinkerSpec"
 *
 *
 */
class PersonEncounterLinkerSpec extends CoreGraphSpecification {

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////


    def "reason"() {
        given:
        def res
        def reasoner = new PersonEncounterLinker(graph)
        def g = graph.traversal()

        expect:
        reasoner.validate().isValid

        when:
        def pV = graph.addVertex(T.label, 'Patient')
        def encV = graph.addVertex(T.label, 'BiobankEncounter')
        res = reasoner.reason()

        then:
        !g.V(pV).out('participated_in_encounter').is(encV).tryNext().isPresent()

        when:
        def idV = graph.addVertex(T.label, 'Identifier')
        pV.addEdge('is_identified_by', idV)
        encV.addEdge('is_identified_by', idV)
        res = reasoner.reason()

        then:
        g.V(pV).out('participated_in_encounter').is(encV).tryNext().isPresent()

        when:
        res = reasoner.validate()

        then:
        res
        res.success
        res.isValid
    }


    def "validate"() {
        given:
        def res
        def reasoner = new PersonEncounterLinker(graph)

        expect:
        graph != null
        def g = graph.traversal()

        when:
        res = reasoner.validate()

        then:
        res
        res.success
        res.isValid

        when:
        def pV = graph.addVertex(T.label, 'Patient')
        def encV = graph.addVertex(T.label, 'BiobankEncounter')
        def idV = graph.addVertex(T.label, 'Identifier')
        pV.addEdge('is_identified_by', idV)
        encV.addEdge('is_identified_by', idV)

        res = reasoner.validate()

        then:
        res
        res.success
        !res.isValid
        res.shouldBeLinked
        res.shouldBeLinked.size() > 0
        res.shouldBeLinked.find { it.patient == pV && it.encounter == encV }

        when:
        pV.addEdge('participated_in_encounter', encV)
        res = reasoner.validate()

        then:
        res
        res.success
        res.isValid
    }

}



