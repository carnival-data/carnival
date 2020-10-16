package carnival.core.vine



import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeInfo.As


class JsonVineMethodCallSpecVine {
    @ToString(includeNames=true)
    static class Person { String name }

    static class PersonVineMethod extends JsonVineMethod<Person> { 
        Person fetch(Map args) { new Person(name:args.p1) }
    }
}


class JsonVineMethodCallSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // classes
    ///////////////////////////////////////////////////////////////////////////

    @ToString(includeNames=true)
    static class Person { String name }

    static class PersonVineMethod extends JsonVineMethod<Person> { 
        Person fetch(Map args) { new Person(name:args.p1) }
    }

    static class PersonsVineMethod extends JsonVineMethod<List<Person>> { 
        List<Person> fetch(Map args) { 
            [
                new Person(name:'alice'),
                new Person(name:'bob') 
            ]
        }
    }

    static class PersonHolder {
        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        Person person
    }

    static class PersonHolderVineMethod extends JsonVineMethod<PersonHolder> { 
        PersonHolder fetch(Map args) { 
            new PersonHolder(person: new Person(name:args.p1)) 
        }
    }

    //@JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    static class PersonsHolder {
        
        List<Person> persons
    }

    static class PersonsHolderVineMethod extends JsonVineMethod<PersonsHolder> { 
        PersonsHolder fetch(Map args) { 
            new PersonsHolder(
                persons: [
                    new Person(name:'alice'), 
                    new Person(name:'bob')
                ]
            ) 
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

    def "create from file"() {
        given:
        File tmpDir = tmpDir()

        when:
        def pv = new PersonVineMethod()
        def mc1 = pv.call(CacheMode.IGNORE, [p1:"alice"])
        def mcf = mc1.writeFiles(tmpDir)[0]
        println "mcf: $mcf"
        def mc2 = JsonVineMethodCall.createFromFile(mcf)

        then:
        mc1 != null
        mc2 != null
        mc1.thisClass == mc2.thisClass
        mc1.vineMethodClass == mc2.vineMethodClass
        mc1.resultClass == mc2.resultClass
        mc1.arguments != null
        mc2.arguments != null
        mc1.arguments.size() == mc2.arguments.size()
        mc1.arguments.get('p1') != null
        mc1.arguments.get('p1') == mc2.arguments.get('p1')
        mc1.result != null
        mc2.result != null
        mc1.result instanceof Person
        mc2.result instanceof Person
        mc1.result.name == mc2.result.name
    }


    def "write file"() {
        given:
        File tmpDir = tmpDir()

        when:
        def pv = new PersonVineMethod()
        def mc = pv.call(CacheMode.IGNORE, [p1:"alice"])
        def mcf = mc.writeFiles(tmpDir)[0]
        println "mcf: $mcf"

        then:
        mcf != null
        mcf.exists()

        when:
        def mct = mcf.text
        println "mct: $mct"

        then:
        mct != null
    }

    def "computed names differ by enclosing class"() {
        when:
        def pvm1 = new PersonVineMethod()
        def mc1 = pvm1.call(CacheMode.IGNORE, [a:'a'])
        def cn1 = mc1.computedName()

        def pvm2 = new JsonVineMethodCallSpecVine.PersonVineMethod()
        def mc2 = pvm2.call(CacheMode.IGNORE, [a:'a'])
        def cn2 = mc2.computedName()

        then:
        cn1 != null
        cn2 != null
        cn1 != cn2
        !cn1.equals(cn2)
    }


    def "computed names with args differ"() {
        when:
        def pvm = new PersonsHolderVineMethod()
        def mc1 = pvm.call(CacheMode.IGNORE, [a:'a'])
        def cn1 = mc1.computedName()
        def mc2 = pvm.call(CacheMode.IGNORE, [a:'b'])
        def cn2 = mc2.computedName()

        then:
        cn1 != null
        cn2 != null
        cn1 != cn2
        !cn1.equals(cn2)
    }


    def "computed name with args"() {
        when:
        def pvm = new PersonsHolderVineMethod()
        def mc = pvm.call(CacheMode.IGNORE, [a:'a'])
        def cn = mc.computedName()

        then:
        cn.startsWith("carnival-core-vine-JsonVineMethodCallSpec-PersonsHolderVineMethod")
        cn.endsWith(".json")
        cn =~ /[0-9a-f]{32}/
    }


    def "computed name no args"() {
        when:
        def pvm = new PersonsHolderVineMethod()
        def mc = pvm.call(CacheMode.IGNORE, [:])
        def cn = mc.computedName()

        then:
        cn == "carnival-core-vine-JsonVineMethodCallSpec-PersonsHolderVineMethod.json"
    }



    ///////////////////////////////////////////////////////////////////////////
    // tests json
    ///////////////////////////////////////////////////////////////////////////

    def "unified json with call arguments"() {
        when:
        def pvm = new PersonsHolderVineMethod()
        def objectParam = new Person(name:'object-param-test-person')
        def mc1 = pvm.call(CacheMode.IGNORE, [a:'a', b:1, c:['a','b'], d:objectParam])
        def js = mc1.toJson()
        println "js: $js"
        def mc2 = JsonVineMethodCall.createFromJson(js)

        then:
        mc2 != null
        mc2 instanceof JsonVineMethodCall
        mc2.result != null
        mc2.result instanceof PersonsHolder
        mc2.result.persons instanceof List<Person>
        mc2.result.persons[0] instanceof Person
        mc2.arguments != null
        mc2.arguments instanceof Map
        mc2.arguments.size() == 4
        mc2.arguments.get('a') == 'a'
        mc2.arguments.get('b') == 1
        mc2.arguments.get('c') instanceof List
        mc2.arguments.get('c').size() == 2
        mc2.arguments.get('d') instanceof Person
    }


    def "unified json result is holder of list of objects"() {
        when:
        def pvm = new PersonsHolderVineMethod()
        def mc1 = pvm.call(CacheMode.IGNORE, [:])
        def js = mc1.toJson()
        println "js: $js"
        def mc2 = JsonVineMethodCall.createFromJson(js)

        then:
        mc2 != null
        mc2 instanceof JsonVineMethodCall
        mc2.result != null
        mc2.result instanceof PersonsHolder
        mc2.result.persons instanceof List<Person>
        mc2.result.persons[0] instanceof Person        
    }


    /*def "segmented json result is object with list field"() {
        when:
        def pvm = new PersonsHolderVineMethod()
        def mc1 = pvm.call(CacheMode.IGNORE, [:])
        def jsMeta = mc1.metaJson()
        println "jsMeta: $jsMeta"
        def jsResult = mc1.resultJson()
        println "jsResult: $jsResult"
        def mc2 = JsonVineMethodCall.createFromJson(jsMeta, jsResult)

        then:
        mc2 != null
        mc2 instanceof JsonVineMethodCall
        mc2.result != null
        mc2.result instanceof PersonsHolder
        mc2.result.persons instanceof List<Person>
        mc2.result.persons[0] instanceof Person
    }*/


    /*def "segmented json result is object with sub-object"() {
        when:
        def pvm = new PersonHolderVineMethod()
        def mc1 = pvm.call(CacheMode.IGNORE, [p1:'alice'])
        def jsMeta = mc1.metaJson()
        println "jsMeta: $jsMeta"
        def jsResult = mc1.resultJson()
        println "jsResult: $jsResult"
        def mc2 = JsonVineMethodCall.createFromJson(jsMeta, jsResult)

        then:
        mc2 != null
        mc2 instanceof JsonVineMethodCall
        mc2.result != null
        mc2.result instanceof PersonHolder
        mc2.result.person instanceof Person
    }*/


    /** 
     * Unfortunately, the List of people gets rendered in JSON as an
     * list of maps.  One solution is to use a holder object like
     * PersonsHolder.  There might be a better way.
     *
     */
    /*def "segmented json result is list of objects"() {
        when:
        def pvm = new PersonsVineMethod()
        def mc1 = pvm.call(CacheMode.IGNORE, [:])
        def jsMeta = mc1.metaJson()
        println "jsMeta: $jsMeta"
        def jsResult = mc1.resultJson()
        println "jsResult: $jsResult"
        def mc2 = JsonVineMethodCall.createFromJson(jsMeta, jsResult)

        then:
        mc2 != null
        mc2 instanceof JsonVineMethodCall
        mc2.result != null
        mc2.result instanceof List<Person>
        mc2.result[0] instanceof Map //Person
        mc2.result[0].name == 'alice'
        mc2.result[1] instanceof Map //Person
        mc2.result[1].name == 'bob'

        when:
        Person alice = mc2.result[0] as Person

        then:
        alice instanceof Person
        alice.name == 'alice'
    }*/

/*
    def "create from json"() {
        when:
        String jsMeta = '''{
  "thisClass" : "carnival.core.vine.JsonVineMethodCall",
  "vineMethodClass" : "carnival.core.vine.JsonVineMethodCallSpec$PersonVineMethod",
  "arguments" : {
    "p1" : "alice"
  },
  "resultClass" : "carnival.core.vine.JsonVineMethodCallSpec$Person"
}'''
        String jsResult = '''{
  "name" : "alice"
}'''

        def mc = JsonVineMethodCall.createFromJson(jsMeta, jsResult)

        then:
        mc != null
        mc instanceof JsonVineMethodCall
        mc.result != null
        mc.result instanceof Person
    }


    def "meta from json"() {
        when:
        String js = '''{
  "thisClass" : "carnival.core.vine.JsonVineMethodCall",
  "vineMethodClass" : "carnival.core.vine.JsonVineMethodCallSpec$PersonVineMethod",
  "arguments" : {
    "p1" : "alice"
  },
  "resultClass" : "carnival.core.vine.JsonVineMethodCallSpec$Person"
}'''

        def mc = JsonVineMethodCall.Meta.createFromJson(js)

        then:
        mc != null
        mc instanceof JsonVineMethodCall.Meta
    }


    def "result json"() {
        when:
        def pv = new PersonVineMethod()
        def cr = pv.call(p1:"alice")
        def js = cr.resultJson()
        println "js: $js"

        then:
        js != null
        js == '''{
  "name" : "alice"
}'''
    }


    def "meta json"() {
        when:
        def pv = new PersonVineMethod()
        def cr = pv.call(p1:"alice")
        def js = cr.metaJson()
        println "js: $js"

        then:
        js != null
        js == '''{
  "thisClass" : "carnival.core.vine.JsonVineMethodCall",
  "vineMethodClass" : "carnival.core.vine.JsonVineMethodCallSpec$PersonVineMethod",
  "arguments" : {
    "p1" : "alice"
  },
  "resultClass" : "carnival.core.vine.JsonVineMethodCallSpec$Person"
}'''
    }

*/

}

