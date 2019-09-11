package carnival.core.graph.query


import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.commons.io.FileUtils



/**
 * gradle test --tests "carnival.core.graph.query.TimeEstimatorSpec"
 *
 *
 */
class TimeEstimatorSpec extends Specification {


    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
    } 


    def cleanupSpec() {
    }


    def cleanup() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////


    def "time to completion as string single proc"() {
        given:
        def qpa = new QueryProcess('a')
        def est = qpa.timeEstimator
        def str

        when:
        est.handleUpdate(qpa, 0, 100)
        str = est.timeToCompletionAsString
        println "str: $str"

        then:
        str == null

        when:
        sleep 500
        est.handleUpdate(qpa, 10, 100)
        str = est.timeToCompletionAsString
        println "str: $str"

        then:
        str != null
    }

    /*def "getTimeToCompletionAsString"() {
        given:
        def est = new TimeEstimator()
        def ttc

        when:
        est.handleUpdate(name:'a', current:0, total:100)
        ttc = est.timeToCompletionAsString

        then:
        ttc        

        when:
        sleep 500
        est.handleUpdate(name:'a', current:10, total:100)
        ttc = est.timeToCompletionAsString

        then:
        ttc
    }*/


    def "time to completion tracking proc with sub procs using statusUpdate"() {
        given:
        def qpa = QueryProcess.create('a')
        def b1 = qpa.createSubProcess('b1')
        def b2 = qpa.createSubProcess('b2')

        def est = qpa.timeEstimator

        expect:
        est != null

        when:
        b1.statusUpdate(0, 100)

        then:
        est.timeToCompletion == null

        when:
        b2.statusUpdate(0, 100)

        then:
        est.timeToCompletion == null

        when:
        sleep 50
        b1.statusUpdate(10, 100)
        sleep 50

        then:
        // time to completeion is null because b2 is incomplete and without
        // a time estimate        
        est.timeToCompletion == null

        when:
        sleep 50
        b2.statusUpdate(20, 100)
        sleep 50

        then:
        est.timeToCompletion != null
        est.timeToCompletion > 0
    }


    def "time to completion tracking proc with sub procs"() {
        given:
        def qpa = new QueryProcess('a')
        def b1 = qpa.createSubProcess('b1')
        def b2 = qpa.createSubProcess('b2')

        def est = qpa.timeEstimator

        expect:
        est != null

        when:
        b1.timeEstimator.handleUpdate(b1, 0, 100)

        then:
        est.timeToCompletion == null

        when:
        b2.timeEstimator.handleUpdate(b2, 0, 100)

        then:
        est.timeToCompletion == null

        when:
        sleep 50
        b1.timeEstimator.handleUpdate(b1, 10, 100)

        then:
        // time to completeion is null because b2 is incomplete and without
        // a time estimate        
        est.timeToCompletion == null

        when:
        b2.timeEstimator.handleUpdate(b2, 20, 100)

        then:
        est.timeToCompletion != null
        est.timeToCompletion > 0
    }


    def "time to completion single proc"() {
        given:
        def qpa = new QueryProcess('a')
        def est = qpa.timeEstimator

        when:
        est.handleUpdate(qpa, 0, 100)

        then:
        est.timeToCompletion == null

        when:
        sleep 500
        est.handleUpdate(qpa, 10, 100)

        then:
        est.timeToCompletion != null
        est.timeToCompletion > 0
    }


    def "handleUpdate listens only to relevant proc"() {
        given:
        def qpa = new QueryProcess('a')
        def est = new TimeEstimator(qpa)

        expect:
        est.statusUpdates.size() == 0

        when:
        est.handleUpdate(qpa, 0, 100)

        then:
        est.statusUpdates.size() == 1
        
        when:
        def qpb = new QueryProcess('b')
        est.handleUpdate(qpb, 2, 58)

        then:
        est.statusUpdates.size() == 1

        when:
        qpa.subProcesses << qpb
        est.handleUpdate(qpb, 2, 58)

        then: 
        est.statusUpdates.size() == 1
    }



}





