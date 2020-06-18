package carnival.core.vine



import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared
import carnival.core.vine.CachingVine.CacheMode
import com.fasterxml.jackson.core.JsonParseException



class JsonVineMethodSpec extends Specification {


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

    static class PersonVineMethod extends JsonVineMethod<Person> { 
        @Override
        File _cacheDirectory() { tmpDir() }

        Person fetch(Map args) { new Person(name:args.p1) }
    }



    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////

    def "cachemode required fails if no cache file present"() {
        when:
        def pv = new PersonVineMethod()
        def cf = pv.cacheFile(p1:"alice")
        println "cf: ${cf}"
        cf.delete()
        def mc = pv.call(CacheMode.REQUIRED, [p1:"alice"])

        then:
        Exception e = thrown()
        e.message.toLowerCase().contains('required')
    }


    def "cachemode optional uses cache file if there"() {
        when:
        def pv = new PersonVineMethod()
        def cf = pv.cacheFile(p1:"alice")
        println "cf: ${cf}"
        cf.delete()
        def mc1 = pv.call(CacheMode.OPTIONAL, [p1:"alice"])
        def cft = cf.text
        cft = cft.replaceAll("alice", "bob")
        cf.write(cft)
        def mc2 = pv.call(CacheMode.OPTIONAL, [p1:"alice"])

        then:
        noExceptionThrown()
        mc2.result instanceof Person
        mc2.result.name == "bob"
    }


    def "cachemode optional fetches and writes if no cache file present"() {
        when:
        def pv = new PersonVineMethod()
        def cf = pv.cacheFile(p1:"alice")
        cf.delete()

        then:
        !cf.exists()

        when:
        def mc = pv.call(CacheMode.OPTIONAL, [p1:"alice"])

        then:
        cf.exists()
    }


    def "cachemode ignore writes cache file"() {
        when:
        def pv = new PersonVineMethod()
        def cf = pv.cacheFile(p1:"alice")
        if (cf.exists()) cf.delete()

        then:
        !cf.exists()

        when:
        def mc = pv.call(CacheMode.IGNORE, [p1:"alice"])

        then:
        cf.exists()
    }


    def "cachemode ignore ignores existing file"() {
        when:
        def pv = new PersonVineMethod()
        pv.cacheFile(p1:"alice").write(CRUD)
        def mc = pv.call(CacheMode.IGNORE, [p1:"alice"])

        then:
        noExceptionThrown()
        mc != null
        mc instanceof JsonVineMethodCall<Person>
        mc.vineMethodClass == PersonVineMethod
        mc.arguments != null
        mc.arguments instanceof Map
        mc.arguments.containsKey('p1')
        mc.arguments.get('p1') == "alice"
        mc.result != null
        mc.result instanceof Person
        mc.result.name == "alice"
    }


    def "invalid cache file causes an exception"() {
        when:
        def pv = new PersonVineMethod()
        pv.cacheFile(p1:"alice").write(CRUD)
        def mc = pv.call(CacheMode.OPTIONAL, [p1:"alice"])

        then:
        Exception e = thrown()
        //e.printStackTrace()
        e instanceof JsonParseException
    }


    def "fetch"() {
        when:
        def pv = new PersonVineMethod()
        def p = pv.fetch(p1:"alice")

        then:
        p != null
        p instanceof Person
        p.name == "alice"
    }

}