package carnival.core.vine


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import groovy.sql.*
import groovy.util.AntBuilder

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import org.apache.commons.io.FileUtils

import org.apache.tinkerpop.gremlin.*
import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.util.*
import org.apache.tinkerpop.gremlin.groovy.loaders.*

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of
//import static org.apache.tinkerpop.gremlin.structure.T.*

import carnival.core.*
import carnival.core.matcher.*
import carnival.pmbb.*
import carnival.pmbb.vine.*

import carnival.util.Defaults
import carnival.core.config.DatabaseConfig
import carnival.util.MappedDataTable
import carnival.core.vine.MappedDataTableVineMethod
import carnival.core.graph.query.QueryProcess





/**
 * gradle test --tests "carnival.core.vine.CsvFileVineSpec"
 *
 */
class CsvFileVineSpec extends Specification {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    //@Shared cachingVine



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */


    ///////////////////////////////////////////////////////////////////////////
    // TESTS - VINE METADATA
    ///////////////////////////////////////////////////////////////////////////


    def "file with quotes"() {
        File file
        def vine
        def dt
        def rec
        Throwable e

        given:
        file = File.createTempFile('CsvFileVineSpecTestFile', 'csv')
        if (file.exists()) file.delete()
        file << 'COL1,COL2\n'
        file << '1,"a"\n'
        file << '2,"b b"\n'
        file << '3,"c c c"\n'
        vine = new CsvFileVine(file)

        when:
        dt = vine.allRecords()
        dt.dataIterator().each { println "$it" }

        then:
        dt
        dt.data.size() == 3

        when:
        rec = dt.data.get(idx)

        then:
        rec.size() == 2
        rec.get('COL1') == v1
        rec.get('COL2') == v2

        where:
        idx | v1  | v2
        0   | '1' | 'a'
        1   | '2' | 'b b'
        2   | '3' | 'c c c'
    }


    def "simple file"() {
        File file
        def vine
        def dt
        def rec
        Throwable e

        given:
        file = File.createTempFile('CsvFileVineSpecTestFile', 'csv')
        if (file.exists()) file.delete()
        file << "COL1,COL2\n"
        file << "1,a\n"
        file << "2,b\n"
        file << "3,c\n"
        vine = new CsvFileVine(file)

        when:
        dt = vine.allRecords()
        dt.dataIterator().each { println "$it" }

        then:
        dt
        dt.data.size() == 3

        when:
        rec = dt.data.get(idx)

        then:
        rec.size() == 2
        rec.get('COL1') == v1
        rec.get('COL2') == v2

        where:
        idx | v1  | v2
        0   | '1' | 'a'
        1   | '2' | 'b'
        2   | '3' | 'c'
    }





}







