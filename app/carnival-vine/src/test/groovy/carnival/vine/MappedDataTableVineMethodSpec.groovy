package carnival.vine



import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles
import org.yaml.snakeyaml.scanner.ScannerException



class MappedDataTableVineMethodSpec extends Specification {


    ///////////////////////////////////////////////////////////////////////////
    // static
    ///////////////////////////////////////////////////////////////////////////

    static File tmpDir() {
        String tmpDirStr = System.getProperty("java.io.tmpdir")
        File tmpDir = new File(tmpDirStr)
        assert tmpDir.exists()
        assert tmpDir.isDirectory()
        tmpDir
    }

    static final String CRUD = "*@&RUWDN&@I#UC"


    ///////////////////////////////////////////////////////////////////////////
    // classes
    ///////////////////////////////////////////////////////////////////////////

    @ToString(includeNames=true)
    static class Person { String name }

    static class PersonVineMethod extends MappedDataTableVineMethod { 
        @Override
        File _cacheDirectory() { tmpDir() }

        MappedDataTable fetch(Map args) {
            def mdt = createDataTable(idFieldName:'ID')
            mdt.dataAdd(id:'1', name:args.p1)
            mdt
        }
    }




    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////

    def "mode method"() {
        when:
        def pv = new PersonVineMethod().args(p1:"alice")
        def cfs = pv.cacheFiles()
        println "cfs: ${cfs}"
        cfs.delete()
        def mc = pv.mode(CacheMode.REQUIRED).call()

        then:
        Exception e = thrown()
        e.message.toLowerCase().contains('required')
    }


    def "cachemode required fails if no cache file present"() {
        when:
        def pv = new PersonVineMethod().args(p1:"alice")
        def cfs = pv.cacheFiles()
        println "cfs: ${cfs}"
        cfs.delete()
        def mc = pv.call(CacheMode.REQUIRED)

        then:
        Exception e = thrown()
        e.message.toLowerCase().contains('required')
    }


    def "cachemode optional uses cache file if there"() {
        when:
        def pv = new PersonVineMethod().args(p1:"alice").mode(CacheMode.OPTIONAL)
        def cfs = pv.cacheFiles()
        println "cfs: ${cfs}"
        cfs.delete()
        def mc1 = pv.call()
        cfs.each {
            def cft = it.text
            cft = cft.replaceAll("alice", "bob")
            it.write(cft)
        }
        def mc2 = pv.call()

        then:
        noExceptionThrown()
        mc2.result instanceof MappedDataTable
        mc2.result.dataGet('1', 'name') == "bob"
    }


    def "cachemode optional fetches and writes if no cache file present"() {
        when:
        def pv = new PersonVineMethod().args(p1:"alice")
        def cfs = pv.cacheFiles()
        if (cfs.exist()) cfs.toMap().values().each { it.delete() } 

        then:
        !cfs.exist()

        when:
        def mc = pv.call(CacheMode.OPTIONAL)

        then:
        cfs.exist()
    }


    def "cachemode ignore writes cache file"() {
        when:
        def pv = new PersonVineMethod().args(p1:"alice")
        def cfs = pv.cacheFiles()
        if (cfs.exist()) cfs.toMap().values().each { it.delete() } 

        then:
        !cfs.exist()

        when:
        def mc = pv.call(CacheMode.IGNORE)

        then:
        cfs.exist()
    }


    def "cachemode ignore ignores existing file"() {
        when:
        def pv = new PersonVineMethod().args(p1:"alice")
        pv.cacheFiles().toMap().values().each { it.write(CRUD) }
        def mc = pv.call(CacheMode.IGNORE)

        then:
        noExceptionThrown()
        mc != null
        mc instanceof MappedDataTableVineMethodCall
        mc.vineMethodClass == PersonVineMethod
        mc.arguments != null
        mc.arguments instanceof Map
        mc.arguments.containsKey('p1')
        mc.arguments.get('p1') == "alice"
        mc.result != null
        mc.result instanceof MappedDataTable
        mc.result.dataGet('1', 'name') == "alice"
    }


    def "invalid cache file causes an exception"() {
        when:
        def pv = new PersonVineMethod().args(p1:"alice")
        pv.cacheFiles().toMap().values().each { it.write(CRUD) }
        def mc = pv.call(CacheMode.OPTIONAL)

        then:
        Exception e = thrown()
        //e.printStackTrace()
        e instanceof ScannerException
    }


    def "fetch"() {
        when:
        def pv = new PersonVineMethod()
        def p = pv.fetch(p1:"alice")

        then:
        p != null
        p instanceof MappedDataTable
        p.dataGet('1', 'name') == "alice"
    }

}