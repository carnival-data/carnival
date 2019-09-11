package carnival.util


import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.commons.io.FileUtils



/**
 * gradle -Dtest.single=TimeToCompletionEstimatorSpec test
 *
 *
 */
class TimeToCompletionEstimatorSpec extends Specification {


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

    def "getTimeToCompletionAsString"() {
        given:
        def est = new TimeToCompletionEstimator()
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
    }



    def "handleUpdate"() {
        given:
        def est = new TimeToCompletionEstimator()

        when:
        est.handleUpdate(name:'a', current:0, total:100)

        then:
        est.timeToCompletion == 0

        when:
        sleep 500
        est.handleUpdate(name:'a', current:10, total:100)

        then:
        est.timeToCompletion > 0
    }



}





