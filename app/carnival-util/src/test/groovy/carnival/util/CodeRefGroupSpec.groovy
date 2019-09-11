package carnival.util


import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.commons.io.FileUtils



/**
 * gradle -Dtest.single=CodeRefGroupSpec test
 *
 *
 */
class CodeRefGroupSpec extends Specification {


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


    def "icd strings from int ranges"() {
        def result

        when:
        result = CodeRefGroup.icdCodeStringsFromRanges(
            8, 10,
            140, 141
        )

        then:
        result.size() == 5
        result[0] == '8.*'
        result[1] == '9.*'
        result[2] == '10.*'
        result[3] == '140.*'
        result[4] == '141.*'
    }




    def "icd strings from ranges"() {
        def result

        when:
        result = CodeRefGroup.icdCodeStringsFromRanges(
            '8', '10',
            '140', '141'
        )

        then:
        result.size() == 5
        result[0] == '8.*'
        result[1] == '9.*'
        result[2] == '10.*'
        result[3] == '140.*'
        result[4] == '141.*'
    }


    def "icd strings from range"() {
        when:
        def result = CodeRefGroup.icdCodeStringsFromRange(rangeStart, rangeStop)

        then:
        result.size() == expectedResult.size()
        expectedResult.eachWithIndex { er, erIdx -> assert result[erIdx] == er }

        where:
        rangeStart | rangeStop | expectedResult
        '8'        | '11'      | ['8.*', '9.*', '10.*', '11.*']
        '140'      | '143'     | ['140.*', '141.*', '142.*', '143.*']
        'C00'      | 'C03'     | ['C00.*', 'C01.*', 'C02.*', 'C03.*']
    }


    def "icd string prefixes from range exception cases"() {
        def result
        Throwable e

        when:
        result = CodeRefGroup.icdCodeStringPrefixesFromRange('1', '1a')

        then:
        e = thrown()
    }


    def "icd string prefixes from range"() {
        when:
        def result = CodeRefGroup.icdCodeStringPrefixesFromRange(rangeStart, rangeStop)

        then:
        result.size() == expectedResult.size()
        expectedResult.eachWithIndex { er, erIdx -> assert result[erIdx] == er }

        where:
        rangeStart | rangeStop | expectedResult
        '8'        | '11'      | ['8', '9', '10', '11']
        '140'      | '143'     | ['140', '141', '142', '143']
        'C00'      | 'C03'     | ['C00', 'C01', 'C02', 'C03']
    }


    def "create must have name arg"() {
        given:
        Throwable t
        def cg

        when:
        cg = CodeRefGroup.create(name_:"a")

        then:
        t = thrown()
    }


    def "create must provide name"() {
        given:
        Throwable t
        def cg

        when:
        cg = CodeRefGroup.create(name:name)

        then:
        t = thrown()

        where:
        name << [null, "", " "]
    }


    def "create basic"() {
        given:
        def cg

        when:
        cg = CodeRefGroup.create(name:"a", icd:icd, icd9:icd9, icd10:icd10)

        then:
        cg.name == "a"
        cg.codeRefs.size() == icd.size() + icd9.size() + icd10.size()
        if (icd) icd.each { value -> 
            assert cg.codeRefs.find { it.system == CodeSystem.ICD && it.value == value }  
        }
        if (icd9) icd9.each { value -> 
            assert cg.codeRefs.find { it.system == CodeSystem.ICD9 && it.value == value }  
        }
        if (icd10) icd10.each { value -> 
            assert cg.codeRefs.find { it.system == CodeSystem.ICD10 && it.value == value }  
        }

        where:
        icd         | icd9        | icd10
        ['1']       | ['2']       | ['3']
        []          | ['2']       | ['3']
        ['1']       | []          | ['3']
        ['1']       | ['2']       | []
    }


    def "addCodeRef invalid syntax"() {
        given:
        Throwable t
        def cg = new CodeRefGroup(name:"a")

        when:
        cg.addCodeRef(CodeSystem.ICD, refStr)

        then:
        t = thrown()

        where:
        refStr << ['1%', '1**', '*1']
    }


    def "addCodeRefs empty list"() {
        given:
        Throwable t
        def cg = new CodeRefGroup(name:"a")

        when:
        cg.addCodeRefs(CodeSystem.ICD, [])

        then:
        t = thrown()

        when:
        cg.addCodeRefs(CodeSystem.ICD, null)

        then:
        t = thrown()
    }



    def "addCodeRefs basic"() {
        when:
        def cg = new CodeRefGroup(name:"a")
        cg.addCodeRefs(CodeSystem.ICD, strings)
        def cgCodes = cg.getIndividualCodeRefs()
        def cgWilds = cg.getWildcardRefs()

        then:
        cg.codeRefs.size() == codes.size() + wilds.size()

        cgCodes.size() == codes.size()
        if (codes) {
            codes.each { value ->
                assert cgCodes.find { it.value == value }
                assert cg.codeRefs.find { it.value == value }
            }
        }

        cgWilds.size() == wilds.size()
        if (wilds) {
            wilds.each { value ->
                assert cgWilds.find { it.value == value }
                assert cg.codeRefs.find { it.value == value }
            }
        }

        where:
        strings     | codes       | wilds
        ['1']       | ['1']       | []
        ['1', '1*'] | ['1']       | ['1*']
        ['1*', '1'] | ['1']       | ['1*']
        ['1*']      | []          | ['1*']
        ['F11.*']      | []          | ['F11.*']
    }


    def "addCodeRef basic"() {
        when:
        def cg = new CodeRefGroup(name:"a")
        strings.each { str ->
            cg.addCodeRef(CodeSystem.ICD, str)
        }
        def cgCodes = cg.getIndividualCodeRefs()
        def cgWilds = cg.getWildcardRefs()

        then:
        cg.codeRefs.size() == codes.size() + wilds.size()

        cgCodes.size() == codes.size()
        if (codes) {
            codes.each { value ->
                assert cgCodes.find { it.value == value }
                assert cg.codeRefs.find { it.value == value }
            }
        }

        cgWilds.size() == wilds.size()
        if (wilds) {
            wilds.each { value ->
                assert cgWilds.find { it.value == value }
                assert cg.codeRefs.find { it.value == value }
            }
        }

        where:
        strings     | codes       | wilds
        ['1']       | ['1']       | []
        ['1', '1*'] | ['1']       | ['1*']
        ['1*', '1'] | ['1']       | ['1*']
        ['1*']      | []          | ['1*']
    }



}





