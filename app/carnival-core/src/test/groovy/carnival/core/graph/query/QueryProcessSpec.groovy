package carnival.core.graph.query


import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.commons.io.FileUtils



/**
 * gradle test --tests "carnival.core.graph.query.QueryProcessSpec"
 *
 *
 */
class QueryProcessSpec extends Specification {


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


    def "monitor thread basic"() {
        given:
        def qp
        def su

        when:
        qp = QueryProcess.create('a')
        qp.startMonitorThread(sleepIntervalMillis:100)

        then:
        1 == 1

        when:
        qp.statusUpdate(0, 10)
        sleep 200

        then:
        1 == 1

        when:
        qp.statusUpdate(1, 10)
        sleep 200

        then:
        1 == 1

        when:
        qp.stop()

        then:
        1 == 1
    }



    def "completed propagates up fail()"() {
        given:
        def qp
        def b1
        def b2

        when:
        qp = QueryProcess.create(name:'a', isTrackingProc:true)
        b1 = qp.createSubProcess('b1')
        b2 = qp.createSubProcess('b2')

        then:
        !qp.completed
        !b1.completed
        !b2.completed

        when:
        b1.stop()

        then:
        !qp.completed
        b1.completed
        !b2.completed

        when:
        b2.fail(new Exception('hi'))

        then:
        b1.completed
        b2.completed
        qp.completed
        !qp.success
        qp.timeEstimator.isCanceled
    }


    def "completed propagates up stop()"() {
        given:
        def qp
        def b1
        def b2

        when:
        qp = QueryProcess.create(name:'a', isTrackingProc:true)
        b1 = qp.createSubProcess('b1')
        b2 = qp.createSubProcess('b2')

        then:
        !qp.completed
        !b1.completed
        !b2.completed

        when:
        b1.stop()

        then:
        !qp.completed
        b1.completed
        !b2.completed

        when:
        b2.stop()

        then:
        b1.completed
        b2.completed
        qp.completed
        qp.success
        qp.timeEstimator.isCanceled
    }



    def "subProcStatusUpdate"() {
        given:
        def qp
        def su
        def b1
        def b2
        def ch

        when:
        qp = new QueryProcess('a')
        b1 = qp.createSubProcess('b1')
        b2 = qp.createSubProcess('b2')
        ch = qp.createStatusUpdateReadChannel()

        b2.statusUpdate(0, 10)
        su = ch.val

        then:
        su
        su.queryProcess == b2
        su.current == 0
        su.total == 10
    }


    def "statusUpdate"() {
        given:
        def qp
        def su

        qp = new QueryProcess('a')
        def channel = qp.createStatusUpdateReadChannel()

        when:
        qp.statusUpdate(0, 10)
        su = channel.val

        then:
        su instanceof StatusUpdate
        su.queryProcess == qp
        su.current == 0
        su.total == 10
    }


    def "getAllProcesses"() {
        given:
        def qp
        def all

        when:
        qp = new QueryProcess('a')
        all = qp.allProcesses
        
        then:
        all.size() == 1
        all[0] == qp

        when:
        qp = new QueryProcess('a')
        def b1 = new QueryProcess('b1')
        def b2 = new QueryProcess('b2')
        qp.subProcesses << b1
        qp.subProcesses << b2
        all = qp.allProcesses
        
        then:
        all.size() == 3
        all[0] == qp
        all[1] == b1
        all[2] == b2

        when:
        qp = new QueryProcess('a')
        
        b1 = new QueryProcess('b1')
        def b11 = new QueryProcess('b11')
        def b12 = new QueryProcess('b12')
        b1.subProcesses << b11
        b1.subProcesses << b12
        
        b2 = new QueryProcess('b2')
        qp.subProcesses << b1
        qp.subProcesses << b2
        all = qp.allProcesses
        
        then:
        all.size() == 5
        all[0] == qp
        all[1] == b1
        all[2] == b11
        all[3] == b12
        all[4] == b2
    }


}





