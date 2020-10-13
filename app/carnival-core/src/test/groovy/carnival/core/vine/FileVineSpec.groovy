package carnival.core.vine


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import groovy.sql.*

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





/**
 * gradle test --tests "carnival.core.vine.FileVineSpec"
 *
 */
class FileVineSpec extends Specification {


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



    def "get set file"() {
        given:
        def vine = new TestFileVine()
        Throwable e

        when:
        vine.getFile()

        then:
        e = thrown()
        e.message == "TestFileVine getFile() file is null"

        when:
        vine.setFile(null)

        then:
        e = thrown()
        e.message == "TestFileVine setFile() file is null"

        when:
        File f1 = File.createTempFile('FileVineSpecGetSetFile', 'txt')
        if (f1.exists()) f1.delete()

        then:
        !f1.exists()

        when:
        vine.setFile(f1)

        then:
        e = thrown()
        e.message.endsWith "does not exist"

        when:
        f1.write 'Working with files the Groovy way is easy.\n'
        vine.setFile(f1)

        then:
        1 == 1
    }





    /**
     * A caching vine used for testing.
     *
     */
    class TestFileVine extends FileVine {

        /** */
        public TestFileVine(Map args = [:]) {
            super()
        }

    }


}







