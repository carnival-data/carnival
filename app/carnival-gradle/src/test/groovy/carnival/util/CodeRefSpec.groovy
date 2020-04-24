package carnival.util


import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.commons.io.FileUtils



/**
 * gradle -Dtest.single=CodeRefSpec test
 *
 *
 */
class CodeRefSpec extends Specification {


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

    def "getBaseCode"() {
        when:
        def cr = new CodeRef(system:CodeSystem.ICD, value:refStr)
        def res = cr.getBaseCode()

        then:
        res == base

        where:
        refStr  | base
        '1'     | '1'
        '1.'    | '1.'
        '1.1'   | '1.1'
        '1*'    | '1'
        '1.*'   | '1.'
        '1.1*'  | '1.1'
    }


    def "isWildcard"() {
        when:
        def cr = new CodeRef(system:CodeSystem.ICD, value:refStr)
        def res = cr.isWildcard()

        then:
        res == wild

        where:
        refStr  | wild
        '1'     | false
        '1.'    | false
        '1.1'   | false
        '1*'    | true
        '1.*'   | true
        '1.1*'  | true
    }


    def "static isWildcard"() {
        when:
        def res = CodeRef.isWildcard(refStr)

        then:
        res == wild

        where:
        refStr  | wild
        '1'     | false
        '1.'    | false
        '1.1'   | false
        '1*'    | true
        '1.*'   | true
        '1.1*'  | true
    }


    def "validateSyntax invalid"() {
        when:
        def cr = new CodeRef(system:CodeSystem.ICD, value:refStr)
        def res = cr.validateSyntax()

        then:
        res != null

        where:
        refStr << ['1%', '1**', '*1']
    }


    def "validateSyntax valid"() {
        when:
        def cr = new CodeRef(system:CodeSystem.ICD, value:refStr)
        def res = cr.validateSyntax()

        then:
        res == null

        where:
        refStr << ['1', '1.', '1.1', '1*', '1.*', '1.1*']
    }


    def "static validateSyntax invalid"() {
        when:
        def res = CodeRef.validateSyntax(refStr)

        then:
        res != null

        where:
        refStr << ['1%', '1**', '*1']
    }


    def "static validateSyntax valid"() {
        when:
        def res = CodeRef.validateSyntax(refStr)

        then:
        res == null

        where:
        refStr << ['1', '1.', '1.1', '1*', '1.*', '1.1*']
    }


    def "create"() {
        when:
        def cr = CodeRef.create(sys, val)

        then:
        cr.system == sys
        cr.value == val

        where:
        sys              | val
        CodeSystem.ICD   | '1.1'
        CodeSystem.ICD9  | '1.1'
        CodeSystem.ICD10 | '1.1'
    }

}





