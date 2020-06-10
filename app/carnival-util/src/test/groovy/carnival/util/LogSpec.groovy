package carnival.util


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * gradle -Dtest.single=LogSpec test
 *
 *
 */
class LogSpec extends Specification {


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

    @Shared Logger testLog

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
        testLog = LoggerFactory.getLogger('carnivalLogTester')
    } 


    def cleanupSpec() {
    }




    def "progress method for valid params"() {
        //when:


        expect:
        Log.progress(testLog, msg, total, current)

        where:
        msg     | total | current
        "test"  | 10    | 5
        "test"  | 10    | 10
        "test"  | 10    | 0
        "test"  | 0     | 10
        "test"  | 0     | 0
        null    | 10    | 5
    }

}
