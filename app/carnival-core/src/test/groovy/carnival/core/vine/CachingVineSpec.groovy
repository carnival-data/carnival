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

import org.apache.tinkerpop.gremlin.neo4j.structure.*

import carnival.core.*
import carnival.core.matcher.*
import carnival.pmbb.*
import carnival.pmbb.vine.*

import carnival.util.Defaults
import carnival.core.config.DatabaseConfig
import carnival.util.MappedDataTable
import carnival.util.KeyType
import carnival.core.vine.MappedDataTableVineMethod
import carnival.core.graph.query.QueryProcess





/**
 * gradle test --tests "carnival.core.vine.CachingVineSpec"
 *
 */
class CachingVineSpec extends Specification {


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

    def setupSpec() {
        // set up target directory
        def ant = new AntBuilder()
        if (!Defaults.targetDirectory.exists()) ant.mkdir(dir:Defaults.targetDirectory)
    }  



    ///////////////////////////////////////////////////////////////////////////
    // TESTS - VINE METADATA
    ///////////////////////////////////////////////////////////////////////////

    def "vine metaData with vine args"() {
        given:
        def cachingVine = new TestCachingVine()
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
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

        when:
        def qp = new QueryProcess('qp1')
        args = [arg1:'val1', queryProcess:qp]

        mdt = cachingVine.vineMethodWithQueryProcess(args)

        def files = mdt.writeFiles(buildDir)
        println "files: ${files}"
        def df = files.find { it.canonicalPath.endsWith('.csv') }
        def mf = files.find { it.canonicalPath.endsWith('.yaml') }

        then:
        df.exists()
        df.name == 'vine-method-with-query-process.csv'
        df.length() > 0
        mf.exists()
        mf.name == 'vine-method-with-query-process.yaml'
        //mf.length() > 0

        when:
        def mfText = mf.text

        then:
        mfText
        !mfText.contains('queryProcess')
    }


    def "vine metaData with no args"() {
        given:
        def cachingVine = new TestCachingVine()
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE

        when:
        def mdt0 = cachingVine.staticVineMethod()

        then:
        matchStaticVineData(mdt0)
        matchStaticVineMetaData(mdt0)

        when:
        def cacheDataFile = mdt0.findDataFile(cachingVine.cacheDirectory)
        println "cacheDataFile: ${cacheDataFile?.canonicalPath}"

        then:
        cacheDataFile

        when:
        def ant = new AntBuilder()
        ant.replace(file: cacheDataFile, token: "v11", value: "v11_")
        println cacheDataFile.text
        def mdt1 = cachingVine.staticVineMethod()

        then:
        matchStaticVineData(mdt1)
        matchStaticVineMetaData(mdt1)
    }

    def "vine metaData with args"() {
        given:
        def cachingVine = new TestCachingVine()
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
        def mdt

        def args = [arg1:'val1']

        when:
        mdt = cachingVine.staticVineMethod(args)

        then:
        matchStaticVineData(mdt)
        matchStaticVineMetaData(mdt, args)

        when:
        args = [arg1:'val1', arg2:'val2']
        mdt = cachingVine.staticVineMethod(args)

        then:
        matchStaticVineData(mdt)
        matchStaticVineMetaData(mdt, args)

        when:
        args = [arg1:'val1', arg2:['lval1', 'lval2']]
        mdt = cachingVine.staticVineMethod(args)

        then:
        matchStaticVineData(mdt)
        matchStaticVineMetaData(mdt, args)
    }

    ///////////////////////////////////////////////////////////////////////////
    // TESTS - CACHE MODE 
    ///////////////////////////////////////////////////////////////////////////

    def "withCacheMode basic"() {
        given:
        def vine = new TestCachingVine(
            cacheMode:CachingVine.CacheMode.REQUIRED
        )
        def vmc = vine.findVineMethodClass('StaticVineMethod')
        def vm = vmc.newInstance()
        def name = vm.meta([:]).name
        def files = MappedDataTable.findFiles(vine.cacheDirectory, name)
        files.each { k, f -> f.delete() }

        // first verify that we get a failure due to cache mode REQUIRED
        when:
        def mdt0 = vine.staticVineMethod()

        then:
        Throwable e = thrown()

        // run with cache mode OPTIONAL
        when:
        def cacheModePre = vine.cacheMode
        def cacheModeDuring
        def mdt1 = vine.withCacheMode(CachingVine.CacheMode.OPTIONAL) { CachingVine v ->
            cacheModeDuring = v.cacheMode
            v.staticVineMethod()
        }
        def cacheModePost = vine.cacheMode

        then:
        cacheModePre == CachingVine.CacheMode.REQUIRED
        cacheModeDuring == CachingVine.CacheMode.OPTIONAL
        cacheModePost == CachingVine.CacheMode.REQUIRED
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS - CACHE MODE REQUIRED
    ///////////////////////////////////////////////////////////////////////////


    def "required - work if cache files"() {
        given:
        def vine = new TestCachingVine(
            cacheMode:CachingVine.CacheMode.REQUIRED
        )
        def vmc = vine.findVineMethodClass('StaticVineMethod')
        def vm = vmc.newInstance()
        def name = vm.meta([:]).name
        def files = MappedDataTable.findFiles(vine.cacheDirectory, name)
        files.each { k, f -> f.delete() }

        when:
        def mdt0 = vine.staticVineMethod()

        then:
        Throwable e = thrown()

        when:
        vine.cacheMode = CachingVine.CacheMode.IGNORE
        def mdt1 = vine.staticVineMethod()

        then:
        matchStaticVineData(mdt1)        

        when:
        vine.cacheMode = CachingVine.CacheMode.REQUIRED
        def mdt2 = vine.staticVineMethod()

        then:
        matchStaticVineData(mdt2)        
    }    


    def "required - fail if no cache files"() {
        given:
        def vine = new TestCachingVine(
            cacheMode:CachingVine.CacheMode.REQUIRED
        )
        def vmc = vine.findVineMethodClass('StaticVineMethod')
        def vm = vmc.newInstance()
        def name = vm.meta([:]).name
        def files = MappedDataTable.findFiles(vine.cacheDirectory, name)
        files.each { k, f -> f.delete() }

        when:
        def mdt0 = vine.staticVineMethod()

        then:
        Throwable e = thrown()
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS - CACHE MODE OPTIONAL
    ///////////////////////////////////////////////////////////////////////////



    def "optional - use cache file if exists"() {
        given:
        def cachingVine = new TestCachingVine(
            cacheMode:CachingVine.CacheMode.OPTIONAL
        )

        when:
        def mdt0 = cachingVine.staticVineMethod()

        then:
        matchStaticVineData(mdt0)

        when:
        def cacheDataFile = mdt0.findDataFile(cachingVine.cacheDirectory)
        println "cacheDataFile: ${cacheDataFile?.canonicalPath}"

        then:
        cacheDataFile

        when:
        def ant = new AntBuilder()
        ant.replace(file: cacheDataFile, token: "v11", value: "v11_")
        println cacheDataFile.text
        def mdt1 = cachingVine.staticVineMethod()

        then:
        matchModifiedStaticData(mdt1)
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS - CACHE MODE NONE
    ///////////////////////////////////////////////////////////////////////////


    def "none - cache files are ignored"() {
        given:
        def cachingVine = new TestCachingVine()
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE

        when:
        def mdt0 = cachingVine.staticVineMethod()

        then:
        matchStaticVineData(mdt0)

        when:
        def cacheDataFile = mdt0.findDataFile(cachingVine.cacheDirectory)
        println "cacheDataFile: ${cacheDataFile?.canonicalPath}"

        then:
        cacheDataFile

        when:
        def ant = new AntBuilder()
        ant.replace(file: cacheDataFile, token: "v11", value: "v11_")
        println cacheDataFile.text
        def mdt1 = cachingVine.staticVineMethod()

        then:
        matchStaticVineData(mdt1)
    }



    def "none - multiple cache files"() {
        given:
        def cachingVine = new TestCachingVine()

        when:
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
        def mdt = cachingVine.numArgsVineMethod(args)
        List<File> files = mdt.writtenTo

        then:
        files != null
        files.size() == 2
        files.each { it.exists() }

        when:
        def mdtCache = MappedDataTable.createFromFiles(cachingVine.cacheDirectory, mdt.name)
        def mdtTarget = MappedDataTable.createFromFiles(cachingVine.targetDirectory, mdt.name)

        then:
        matchNumArgsVineData(mdtCache, numArgs)
        matchNumArgsVineData(mdtTarget, numArgs)

        where:
        args << [[:], [a:1], [a:1, b:2]]
        numArgs << [0, 1, 2]
    }



    def "none - cache file data are correct"() {
        given:
        def cachingVine = new TestCachingVine()

        when:
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
        def mdt = cachingVine.call('StaticVineMethod')
        println "$mdt"
        println "${mdt?.data}"
        List<File> files = mdt.writtenTo

        then:
        files != null
        files.size() == 2
        files.each { it.exists() }

        when:
        def mdtCache = MappedDataTable.createFromFiles(cachingVine.cacheDirectory, mdt.name)

        then:
        matchStaticVineData(mdtCache)

        when:
        def mdtTarget = MappedDataTable.createFromFiles(cachingVine.targetDirectory, mdt.name)

        then:
        matchStaticVineData(mdtTarget)
    }



    def "none - cache files are written"() {
        given:
        def cachingVine = new TestCachingVine()

        when:
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
        def mdt = cachingVine.call('StaticVineMethod')
        println "$mdt"
        println "${mdt?.data}"
        List<File> files = mdt.writtenTo

        then:
        files != null
        files.size() == 2
        files.each { it.exists() }
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS - GENERIC
    ///////////////////////////////////////////////////////////////////////////

    def "test data matches"() {
        given:
        def cachingVine = new TestCachingVine()
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
        def mdt
        def args

        when:
        mdt = cachingVine.call('StaticVineMethod')

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
        def cachingVine = new TestCachingVine()
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
        def data
        def args
        Throwable e

        when:
        data = cachingVine.call('StaticVineMethod')

        then:
        data != null

        when:
        data = cachingVine.call('StaticVineMethod', [:])

        then:
        data != null

        when:
        data = cachingVine.call('StaticVineMethod', [arg1:'val1'])

        then:
        data != null

        when:
        data = cachingVine.call('NonExistentVineMethod')

        then:
        e = thrown()
        e instanceof MissingMethodException
    }


    def "method missing"() {
        given:
        def cachingVine = new TestCachingVine()
        cachingVine.cacheMode = CachingVine.CacheMode.IGNORE
        def data
        def args
        Throwable e

        when:
        data = cachingVine.staticVineMethod()

        then:
        data != null

        when:
        data = cachingVine.staticVineMethod(arg1:'val1')

        then:
        data != null

        when:
        data = cachingVine.nonExistentMethod()

        then:
        e = thrown()

        when:
        data = cachingVine.nonExistentMethod(arg1:'arg1')

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
        assert mdt.vine.name == "carnival.core.vine.TestCachingVine"
        assert mdt.vine.method == "StaticVineMethod"
        assert mdt.vine.args == args
    }


    void matchNumArgsVineMetaData(MappedDataTable mdt, Map args = [:]) {
        assert mdt.vine
        assert mdt.vine.name == "carnival.core.vine.TestCachingVine"
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




/**
 * A caching vine used for testing.
 *
 */
class TestCachingVine extends RelationalVineGeneric implements CachingVine {

    public TestCachingVine(Map args = [:]) {
        super({} as DatabaseConfig)
        if (args.cacheMode) this.cacheMode = args.cacheMode
    }


    /**
     * A vine method that returns the same static data regardless of the
     * arguments provided.
     *
     */
    static class StaticVineMethod implements MappedDataTableVineMethod {

        MappedDataTable.MetaData meta(Map args = [:]) {
            new MappedDataTable.MetaData(
                name:'static-vine-method',
                idFieldName:'id',
                idKeyType:KeyType.EMPI
            ) 
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
                idFieldName:'id',
                idKeyType:KeyType.EMPI
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



    static class VineMethodWithQueryProcess implements MappedDataTableVineMethod {

        MappedDataTable.MetaData meta(Map args = [:]) {
            new MappedDataTable.MetaData(
                name:'vine-method-with-query-process',
                idFieldName:'id',
                idKeyType:KeyType.EMPI
            ) 
        }


        MappedDataTable fetch(Map args = [:]) {
            def mdt = createEmptyDataTable(args)

            assert vineMethodQueryProcess

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

}







