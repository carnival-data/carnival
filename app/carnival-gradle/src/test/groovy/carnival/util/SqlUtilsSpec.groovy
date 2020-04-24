package carnival.util


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import groovy.sql.*

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper



/**
 * gradle test --tests="carnival.core.util.SqlUtilsSpec"
 *
 *
 */
class SqlUtilsSpec extends Specification {


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


    @Unroll
    def "chunkedWhereClauseCaseInsensitive basic"() {
        when:
        def res = SqlUtils.chunkedWhereClauseCaseInsensitive(
            fieldName,
            allItems,
            maxItems
        )

        then:
        res == expected

        where:
        fieldName | maxItems | allItems         | expected
        'lab'     | 2        | ['a', 'b']       | "(upper(lab) in ('A', 'B'))"
        'lab'     | 2        | ['a', 'B', 'c']  | "(upper(lab) in ('A', 'B')\nor upper(lab) in ('C'))"
        'lab'     | 2        | ['1', '2', '3']  | "(upper(lab) in ('1', '2')\nor upper(lab) in ('3'))"
    }


    def "timestampAsString current date"() {
        given:
        def cal = Calendar.getInstance()
        def str

        when:
        // 2002-2-3 11:13AM
        cal.set(2002, Calendar.FEBRUARY, 03) 
        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 13)
        cal.set(Calendar.SECOND, 27)

        /*
        def tz = TimeZone.getTimeZone("GMT+10")
        println "tz: $tz"
        cal.setTimeZone(tz)
        */
        
        str = SqlUtils.timestampAsString(cal.time)

        then:
        str == "2002-02-03 11:13:27"
    }

}





