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
class DefaultsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() { } 


    def cleanupSpec() { }


    def setup() { }


    def cleanup() { }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////


    def "all methods return something"() {
        expect:
        Defaults.getTargetDirectoryPath() != null
        Defaults.getTargetDirectory() != null
        Defaults.getDataDirectoryPath() != null
        Defaults.getDataDirectory() != null
        Defaults.getDataCacheDirectoryPath() != null
        Defaults.getDataCacheDirectory() != null
        Defaults.getDataGraphDirectoryPath() != null
        Defaults.getDataGraphDirectory() != null
    }


    def "cannot replace a map with a scalar"() {
        when:
        Defaults.setConfigData(
            [scalar:[a:'map']],
            [scalar:1].entrySet().first()
        )

        then:
        Exception e = thrown()
        e instanceof IllegalArgumentException
    }


    def "cannot override a scalar with a map"() {
        when:
        Defaults.setConfigData(
            [scalar:1],
            [scalar:[a:'map']].entrySet().first()
        )

        then:
        Exception e = thrown()
        e instanceof IllegalArgumentException
    }


    def "supply config data"() {
        Map m

        when:
        def existingDataDir = Defaults.getDataDirectoryPath()
        def existingTargetDir = Defaults.getTargetDirectoryPath()
        m = [
            carnival: [
                directories: [
                    data: [
                        root: "${existingDataDir}_".toString()
                    ]
                ]
            ]
        ]
        Defaults.setConfigData(m)

        then:
        Defaults.getDataDirectoryPath() == "${existingDataDir}_"
        Defaults.getTargetDirectoryPath() == existingTargetDir
    }



}





