package carnival.core.graph.query


import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.commons.io.FileUtils



/**
 * gradle test --tests "carnival.core.graph.query.StatusUpdateSpec"
 *
 *
 */
class StatusUpdateSpec extends Specification {


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

    def "isValid"() {
        given:
        def su

        when:
        su = new StatusUpdate(args)
        
        then:
        su.isValid() == expected

        where:
        expected | args
        false    | [:]
        false    | [queryProcess:new QueryProcess('a')]
        true     | [queryProcess:new QueryProcess('a'), total:1]
    }


}





