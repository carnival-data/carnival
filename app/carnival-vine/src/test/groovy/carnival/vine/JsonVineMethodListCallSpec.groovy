package carnival.vine



import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeInfo.As



class JsonVineMethodListCallSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // classes
    ///////////////////////////////////////////////////////////////////////////

    @ToString(includeNames=true)
    static class Person { String name }


    static class PersonsVineMethod extends JsonVineMethod<List<Person>> { 
        List<Person> fetch(Map args) { 
            [
                new Person(name:'alice'),
                new Person(name:'bob') 
            ]
        }
    }



    static File tmpDir() {
        String tmpDirStr = System.getProperty("java.io.tmpdir")
        File tmpDir = new File(tmpDirStr)
        assert tmpDir.exists()
        assert tmpDir.isDirectory()
        tmpDir
    }


    ///////////////////////////////////////////////////////////////////////////
    // tests json files
    ///////////////////////////////////////////////////////////////////////////


    def "result is wrapped"() {
        when:
        def pv = new PersonsVineMethod()
        def mc1 = pv.call(CacheMode.IGNORE)
        print "mc1: $mc1"
        def js1 = mc1.toJson()
        println "js1: $js1"
        def mc2 = JsonVineMethodCall.createFromJson(js1)

        then:
        mc2 != null
        mc2.result != null
        mc2.result instanceof List
        mc2.result.size() == 2
        mc2.result[0] instanceof Person
        mc2.result[0].name == 'alice'
    }


    def "JsonIgnore removes result from json render"() {
        when:
        def pv = new PersonsVineMethod()
        def mc = pv.call(CacheMode.IGNORE)
        print "mc: $mc"
        def js = mc.toJson()
        println "js: $js"

        then:
        js != null
        !js.contains('"result"')
    }

}

