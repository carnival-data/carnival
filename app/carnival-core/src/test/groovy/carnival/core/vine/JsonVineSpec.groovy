package carnival.core.vine


import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared




/**
JvsTestVine has to be top level classes.
  - inner classes won't work with the JsonVineMethod stuff
  - static inner classes won't work with methodMissing from JsonVine

I wonder if this is too restrictive... If we make JsonVine a class instead
of a trait, the methodMissing thing would probably not be an issue.
Probably nice that JsonVine is a trait. Since there is not multiple 
inheritance in Java, it would be restrictive to force all Vines to extend
a class.
*/
class JvsTestVine implements JsonVine { 

    @ToString(includeNames=true)
    static class Person { String name }

    static File tmpDir() {
        String tmpDirStr = System.getProperty("java.io.tmpdir")
        File tmpDir = new File(tmpDirStr)
        println "tmpDir: ${tmpDir}"
        assert tmpDir.exists()
        assert tmpDir.isDirectory()
        tmpDir
    }

    static class PersonVineMethod extends JsonVineMethod<Person> { 
        @Override
        File _cacheDirectory() { tmpDir() }

        Person fetch(Map args) { new Person(name:args.p1) }
    }

}


class JsonVineSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////

    def "convenience call by name"() {
        when:
        def pv = new JvsTestVine()
        def res = pv.personVineMethod(p1:"alice")

        then:
        noExceptionThrown()
        res != null
        res instanceof JsonVineMethodCall
    }


    def "create vine method instance"() {
        when:
        def pv = new JvsTestVine()
        def vmi = pv.createVineMethodInstance('personVineMethod')

        then:
        vmi != null
        vmi instanceof JvsTestVine.PersonVineMethod
    }


    def "find vine method class"() {
        when:
        def pv = new JvsTestVine()
        def vmc = pv.findVineMethodClass('personVineMethod')

        then:
        vmc != null
        vmc == JvsTestVine.PersonVineMethod
    }


    def "all vine method classes"() {
        when:
        def pv = new JvsTestVine()
        def vmcs = pv.allVineMethodClasses()

        then:
        vmcs != null
        vmcs.size() == 1
        vmcs.contains(JvsTestVine.PersonVineMethod)
    }


}