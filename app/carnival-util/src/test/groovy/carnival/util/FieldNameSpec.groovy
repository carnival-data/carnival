package carnival.util


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import groovy.sql.*



/**
 * gradle -Dtest.single=FieldNameSpec test
 *
 * NOTE - FieldName IS NOT USED!  KEEPING THE TESTS AROUND ANYWAY>
 *
 */
class FieldNameSpec extends Specification {


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



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
    } 


    def cleanupSpec() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // CSV FILES
    ///////////////////////////////////////////////////////////////////////////


    def "field name extension String.fieldName"() {
        when:
        def fn = str.fieldName   // "name".fieldName

        then:
        fn instanceof FieldName
        fn.value == 'NAME'

        where:
        str << ["name", 'name']
    }


    def "field name extension String.getFieldName()"() {
        when:
        def fn = str.getFieldName()

        then:
        fn instanceof FieldName
        fn.value == 'NAME'

        where:
        str << ["name", 'name']
    }


    def "field name object equals and hash"() {
        given:
        def fn0 = FieldName.create('name')
        def fn

        when:
        fn = FieldName.create(str)

        then:
        fn == fn0
        fn.hashCode() == fn0.hashCode()

        where:
        str << ['name', 'nAmE', 'NAME']
    }


    def "field name create upper case"() {
        given:
        def fn

        when:
        fn = FieldName.create(str)

        then:
        fn.value == 'NAME'

        where:
        str << ['name', 'nAmE', 'NAME']
    }


    def "field name constructor upper case"() {
        given:
        def fn

        when:
        fn = new FieldName(str)

        then:
        fn.value == 'NAME'

        where:
        str << ['name', 'nAmE', 'NAME']
    }


    /*
    could no get this to work

    def "field name extension String.fieldName"() {
        when:
        def fn = str.fieldName   // "name".fieldName

        def metaMethods = 'NAME'.metaClass.metaMethods
        metaMethods.each { if ("$it".indexOf('equals') > -1) println "$it ${it.name} ${it.signature} ${it.declaringClass.name}" }
        def methods = 'NAME'.metaClass.methods
        methods.each { if ("$it".indexOf('equals') > -1) println "$it ${it.name} ${it.signature} ${it.declaringClass.name}" }

        then:
        fn instanceof FieldName
        fn.value == 'NAME'

        'NAME'.compareTo(fn) == 0
        'NAME'.equals(fn) 

        fn == 'NAME'
        fn.hashCode() == "NAME".hashCode()
        'NAME' == fn   // 'NAME'.equals(fn)

        where:
        str << ["name", 'name']
    }

    def "field name string equals and hash"() {
        when:
        def fn0 = FieldName.create('name')

        then:
        str == fn0
        str.hashCode() == fn0.hashCode()

        where:
        str << ['name', 'nAmE', 'NAME']
    }
    */


}





