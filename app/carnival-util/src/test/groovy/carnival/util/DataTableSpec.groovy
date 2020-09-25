package carnival.util


import groovy.mock.interceptor.StubFor
import groovy.sql.*
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper



/**
 * gradle test --tests "carnival.core.util.DataTableSpec"
 *
 */
class DataTableSpec extends Specification {


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


    private DataTable testDataTable(Map args = [:]) {
        def dt = [:]
        if (args) dt.putAll(args)
        return dt as DataTable
    }

    class DataTablePlus extends DataTable {
        public Iterator<Map> dataIterator() { return [].iterator() }
        public void dataAdd(java.sql.ResultSet row) { }
        public void dataAdd(GroovyRowResult row) { }
        public void dataAdd(Map<String,Object> vals) { }
        public File writeMetaFile(File destDir, Map args) { return new File('test') }
    }

    class ReverseStringComp implements Comparator<String> {
        public int compare(String a, String b) {
            return b.compareTo(a)
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS FOR STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////


    def "setKeySetComparator(Comparator) valid"() {
        given:
        def dt = testDataTable()

        expect:
        dt.keySet instanceof TreeSet
        dt.keySet.size() == 0

        when:
        dt.keySet << 'B'

        then:
        assertExpectedKeys(dt, ['B'])

        when:
        dt.keySet << 'A'

        then:
        assertExpectedKeys(dt, ['A', 'B'])

        when:
        dt.keySetComparator = new ReverseStringComp()

        then:
        assertExpectedKeys(dt, ['B', 'A'])
    }



    def "setKeySetComparator(List<Closure>) valid"() {
        given:
        def dt = new DataTablePlus()

        expect:
        dt.keySet instanceof TreeSet
        dt.keySet.size() == 0
        
        when:
        dt.keySet << 'B'

        then:
        assertExpectedKeys(dt, ['B'])

        when:
        dt.keySet << 'A'

        then:
        assertExpectedKeys(dt, ['A', 'B'])

        when:
        dt.keySet << 'C'

        then:
        assertExpectedKeys(dt, ['A', 'B', 'C'])
    }



    def "addOrderedKeys valid"() {
        given:
        def dt = testDataTable()

        expect:
        dt.keySet instanceof TreeSet
        dt.keySet.size() == 0

        when:
        dt.keySet << 'A'

        then:
        dt.keySet instanceof TreeSet
        dt.keySet.size() == 1
        assertExpectedKeys(dt, ['A'])

        when:
        dt.addOrderedKeys(['b'])

        then:
        dt.keySet instanceof LinkedHashSet
        dt.keySet.size() == 2
        assertExpectedKeys(dt, ['A', 'B'])

        when:
        dt.addOrderedKeys(['c', 'D'])

        then:
        dt.keySet instanceof LinkedHashSet
        dt.keySet.size() == 4
        assertExpectedKeys(dt, ['A', 'B', 'C', 'D'])
    }


    def "setOrderedKeys valid"() {
        given:
        def dt = testDataTable()

        expect:
        dt.keySet instanceof TreeSet
        dt.keySet.size() == 0

        when:
        dt.setOrderedKeys(vals)

        then:
        dt.keySet instanceof LinkedHashSet
        dt.keySet.size() == expected.size()
        assertExpectedKeys(dt, expected)

        where:
        vals                  | expected
        ['a']                 | ['A']
        ['a', 'b']            | ['A', 'B']
        ['a', 'B']            | ['A', 'B']
        ['A', 'b']            | ['A', 'B']
        []                    | []
    }


    def "addFieldNamesToKeySet invalid"() {
        given:
        Throwable e
        def dt = testDataTable()

        when:
        dt.addFieldNamesToKeySet(vals)

        then:
        e = thrown()

        where:
        vals << [      
            ['a', 'A'],
            ['a', 'b', 'a'],
            ['a', 'B', 'A'],
            ['A', 'b', 'B']
        ]
    }



    def "addFieldNamesToKeySet valid"() {
        given:
        def dt = testDataTable()

        when:
        dt.addFieldNamesToKeySet(vals)

        then:
        dt.keySet.size() == expected.size()
        assertExpectedKeys(dt, expected)

        where:
        vals                  | expected
        ['a']                 | ['A']
        ['a', 'b']            | ['A', 'B']
        ['a', 'B']            | ['A', 'B']
        ['A', 'b']            | ['A', 'B']
    }

    private void assertExpectedKeys(DataTable dt, List<String> expected) {
        assert dt.keySet.size() == expected.size()
        dt.keySet.eachWithIndex { k, i ->
            //println "$k $i ${expected[i]}"
            assert k == expected[i]
        }
        //expected.each { ev -> assert dt.keySet.find { it == ev } }
    }


    
    def "find field name expecteded fails"() {
        given:
        Throwable e

        when:
        DataTable.findFieldName(q, vals)

        then:
        e = thrown()

        where:
        vals                          |   q   
        ['a', 'A'].toSet()            |  'a'  
    }


    def "find field name"() {
        given:
        def res

        when:
        res = DataTable.findFieldName(q, vals)

        then:
        res == expected

        where:
        vals                          |   q   |      expected
        [:]                           |  'a'  |      null
        ['a'].toSet()                 |  'a'  |      'a'
        ['a', 'b'].toSet()            |  'a'  |      'a'
        ['a', 'b'].toSet()            |  'b'  |      'b'
    }


    def "find field name map expecteded fails"() {
        given:
        Throwable e

        when:
        DataTable.findFieldName(q, vals)

        then:
        e = thrown()

        where:
        vals                          |   q   
        [a:'x1', A:'x2']              |  'a'  
    }


    def "find field name map"() {
        given:
        def res

        when:
        res = DataTable.findFieldName(q, vals)

        then:
        res == expected

        where:
        vals                          |   q   |      expected
        [:]                           |  'a'  |      null
        [a:'x']                       |  'a'  |      'a'
        [a:'x', b:'y']                |  'a'  |      'a'
        [a:'x', b:'y']                |  'b'  |      'b'
    }


    def "find duplicate field names"() {
        given:
        def res

        when:
        res = DataTable.findDuplicateFieldNames(vals)

        then:
        res.size() == expectedDups.size()
        matchDupLists(expectedDups, res)

        where:
        vals                          |      expectedDups
        [].toSet()                    |      []
        ['a'].toSet()                 |      []
        [' a', 'A'].toSet()           |      [[' a', 'A']]
        ['a ', 'A'].toSet()           |      [['a ', 'A']]
        ['a', 'A'].toSet()            |      [['a', 'A']]
        ['a', 'A '].toSet()           |      [['a', 'A ']]
        ['a', ' A'].toSet()           |      [['a', ' A']]
        ['a ', 'A'].toSet()           |      [['a ', 'A']]
        [' a', 'A'].toSet()           |      [[' a', 'A']]
        ['a', 'A', 'b'].toSet()       |      [['a', 'A']]
        ['a', 'A', 'b', 'B'].toSet()  |      [['a', 'A'], ['b', 'B']]
    }


    void matchDupLists(List expected, List actual) {
        assert expected != null
        assert actual != null

        assert expected.size() == actual.size()
        expected.each { e ->
            def a = actual.findAll {
                (it[0] == e[0] && it[1] == e[1]) ||
                (it[1] == e[0] && it[0] == e[1])
            }

            assert a.size() == 1, "error finding match for expected $e in $actual: $a"
        }
    }


    def "to id key bad"() {
        given:
        Throwable e

        when:
        DataTable.toIdValue(str)

        then:
        e = thrown()

        where:
        str  << [null, '', ' ', '\n']
    }


    def "to id key"() {
        given:
        def res

        when:
        res = DataTable.toIdValue(str)

        then:
        res == out

        where:
        str  | out
        'some_name' | 'some_name'
        'some_NAME' | 'some_name'
        'SOME_NAME' | 'some_name'
        'some_name ' | 'some_name'
        ' some_name' | 'some_name'
        ' some_name ' | 'some_name'
    }

    def "formatIdValue"() {
        given:
        def res

        when:
        res = DataTable.formatIdValue(str)

        then:
        res == out

        where:
        str  | out
        'some_name' | 'some_name'
        'some_NAME' | 'some_name'
        'SOME_NAME' | 'some_name'
        'some_name ' | 'some_name'
        ' some_name' | 'some_name'
        ' some_name ' | 'some_name'
        null | null
        ''  | ''
        ' ' | ''
    }


    def "to field name bad"() {
        given:
        Throwable e

        when:
        DataTable.toFieldName(str)

        then:
        e = thrown()

        where:
        str  << [null, '', ' ', '\n']
    }


    def "to field name"() {
        given:
        def res

        when:
        res = DataTable.toFieldName(str)

        then:
        res == out

        where:
        str  | out
        'some_name' | 'SOME_NAME'
        'some_NAME' | 'SOME_NAME'
        'SOME_NAME' | 'SOME_NAME'
        'some_name ' | 'SOME_NAME'
        ' some_name' | 'SOME_NAME'
        ' some_name ' | 'SOME_NAME'
    }

}