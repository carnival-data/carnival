package carnival.core.vineold


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import groovy.sql.*
import groovy.util.AntBuilder

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





/**
 * gradle test --tests "carnival.core.vineold.VineSpec"
 *
 */
class VineSpec extends Specification {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    //@Shared vine



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

    def setupSpec() {
        // set up target directory
        def ant = new AntBuilder()
        if (!Defaults.targetDirectory.exists()) ant.mkdir(dir:Defaults.targetDirectory)
        if (!Defaults.dataCacheDirectory.exists()) ant.mkdir(dir:Defaults.dataCacheDirectory)        
    }  



    ///////////////////////////////////////////////////////////////////////////
    // TESTS - VINE METADATA
    ///////////////////////////////////////////////////////////////////////////

    def "vine metaData with vine args"() {
        given:
        def vine = new TestVine()
        def mdt
        def args

        def buildDir = new File('build')
        def edf = new File('build/vine-method-with-query-process.csv')
        def emf = new File('build/vine-method-with-query-process.yaml')

        when:
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()

        then:
        buildDir.exists()
        buildDir.isDirectory()
        !edf.exists()
        !emf.exists()
    }


    def "vine metaData with no args"() {
        given:
        def vine = new TestVine()

        when:
        def mdt0 = vine.staticVineMethod()

        then:
        matchStaticVineData(mdt0)
        matchStaticVineMetaData(mdt0)
    }
    

    def "vine metaData with args"() {
        given:
        def vine = new TestVine()
        def mdt

        def args = [arg1:'val1']

        when:
        mdt = vine.staticVineMethod(args)

        then:
        matchStaticVineData(mdt)
        matchStaticVineMetaData(mdt, args)

        when:
        args = [arg1:'val1', arg2:'val2']
        mdt = vine.staticVineMethod(args)

        then:
        matchStaticVineData(mdt)
        matchStaticVineMetaData(mdt, args)

        when:
        args = [arg1:'val1', arg2:['lval1', 'lval2']]
        mdt = vine.staticVineMethod(args)

        then:
        matchStaticVineData(mdt)
        matchStaticVineMetaData(mdt, args)
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS - GENERIC
    ///////////////////////////////////////////////////////////////////////////

    def "test data matches"() {
        given:
        def vine = new TestVine()
        def mdt
        def args

        when:
        mdt = vine.call('StaticVineMethod')

        then:
        println "$mdt"
        println "${mdt?.data}"
        mdt != null
        mdt.keySet?.size() == 2
        mdt.keySet.find { it == 'ID' }
        mdt.keySet.find { it == 'V1' }
        mdt.data?.size() == 2
        mdt.data['id1']['V1'] == 'v11'
        mdt.data['id1']['ID'] == 'id1'
        mdt.data['id2']['V1'] == 'v12'
        mdt.data['id2']['ID'] == 'id2'
    }


    def "call by vine method class name"() {
        given:
        def vine = new TestVine()
        def data
        def args
        Throwable e

        when:
        data = vine.call('StaticVineMethod')

        then:
        data != null

        when:
        data = vine.call('StaticVineMethod', [:])

        then:
        data != null

        when:
        data = vine.call('StaticVineMethod', [arg1:'val1'])

        then:
        data != null

        when:
        data = vine.call('NonExistentVineMethod')

        then:
        e = thrown()
        e instanceof MissingMethodException
    }


    def "method missing"() {
        given:
        def vine = new TestVine()
        def data
        def args
        Throwable e

        when:
        data = vine.staticVineMethod()

        then:
        data != null

        when:
        data = vine.staticVineMethod(arg1:'val1')

        then:
        data != null

        when:
        data = vine.nonExistentMethod()

        then:
        e = thrown()

        when:
        data = vine.nonExistentMethod(arg1:'arg1')

        then:
        e = thrown()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TEST HELPERS
    ///////////////////////////////////////////////////////////////////////////

    void matchStaticVineMetaData(MappedDataTable mdt, Map args = [:]) {
        println "mdt.vine:"
        println "$mdt.vine"

        assert mdt.vine
        assert mdt.vine.name == "carnival.core.vineold.TestVine"
        assert mdt.vine.method == "StaticVineMethod"
        assert mdt.vine.args == args
    }


    void matchNumArgsVineMetaData(MappedDataTable mdt, Map args = [:]) {
        assert mdt.vine
        assert mdt.vine.name == "carnival.core.vineold.TestVine"
        assert mdt.vine.method == "NumArgsVineMethod"
        assert mdt.vine.args == args
    }


    void matchModifiedStaticData(MappedDataTable mdt) {
        assert mdt
        assert mdt.data
        assert mdt.data.size() == 2

        def d

        d = mdt.data['id1']
        assert d['ID'] == 'id1'
        assert d['V1'] == 'v11_'

        d = mdt.data['id2']
        assert d['ID'] == 'id2'
        assert d['V1'] == 'v12'
    }


    void matchStaticVineData(MappedDataTable mdt) {
        assert mdt
        assert mdt.data
        assert mdt.data.size() == 2

        def d

        d = mdt.data['id1']
        assert d['ID'] == 'id1'
        assert d['V1'] == 'v11'

        d = mdt.data['id2']
        assert d['ID'] == 'id2'
        assert d['V1'] == 'v12'
    }


    void matchNumArgsVineData(MappedDataTable mdt, int numArgs = 0) {
        assert mdt
        assert mdt.data
        assert mdt.data.size() == numArgs + 1

        def d

        (0..numArgs).each { i ->
            d = mdt.data["id$i"]
            assert d['ID'] == "id$i"
            assert d['V1'] == "v1$i"
        }
    }


}




/** */
class TestVine extends Vine {

    public TestVine(Map args = [:]) { }


    /**
     * A vine method that returns the same static data regardless of the
     * arguments provided.
     *
     */
    static class StaticVineMethod implements MappedDataTableVineMethod {

        MappedDataTable.MetaData meta(Map args = [:]) {
            new MappedDataTable.MetaData(
                name:'static-vine-method',
                idFieldName:'id') 
        }


        MappedDataTable fetch(Map args = [:]) {
            def mdt = createEmptyDataTable(args)

            mdt.dataAddAllListList(
                [
                    ['id', 'v1']
                    , ['id1', 'v11']
                    , ['id2', 'v12']
                ]
            )
            return mdt
        }
    }


    /**
     * A vine method that returns different data based on the number of
     * arguments only.
     *
     */
    static class NumArgsVineMethod implements MappedDataTableVineMethod {

        MappedDataTable.MetaData meta(Map args = [:]) {
            new MappedDataTable.MetaData(
                name:"num-args-vine-method-${args.size()}",
                idFieldName:'id'
            ) 
        }

        MappedDataTable fetch(Map args = [:]) {
            def mdt = createEmptyDataTable(args)

            (0..args.size()).each { i ->
                mdt.dataAdd(id:"id$i", v1:"v1$i")
            }
            return mdt
        }
    }

}







