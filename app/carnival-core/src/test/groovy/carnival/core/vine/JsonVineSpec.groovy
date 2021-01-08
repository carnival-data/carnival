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
class JvsTestVine implements Vine { 

    @ToString(includeNames=true)
    static class Person { String name }

    public File tmpDir() {
        String tmpDirStr = System.getProperty("java.io.tmpdir")
        File tmpDir = new File(tmpDirStr)
        println "tmpDir: ${tmpDir}"
        assert tmpDir.exists()
        assert tmpDir.isDirectory()
        tmpDir
    }

    class PersonVineMethod extends JsonVineMethod<Person> { 
        @Override
        File _cacheDirectory() { 
            def td
            try {
                td = tmpDir() 
            } catch (Exception e) {
                e.printStackTrace()
                throw e
            }
            td
        }

        Person fetch(Map args) { new Person(name:args.p1) }
    }

}


class JvsTestVineWithResource extends JvsTestVine { 

    String sharedResource = 'blah-'


    class PersonVineMethod extends JsonVineMethod<JvsTestVine.Person> { 
        @Override
        File _cacheDirectory() { tmpDir() }

        JvsTestVine.Person fetch(Map args) { 
            String name = "" + sharedResource + args.p1
            new JvsTestVine.Person(name:name) 
        }
    }

    class VineMethodThatRefsNonexistentResource extends JsonVineMethod<JvsTestVine.Person> { 
        @Override
        File _cacheDirectory() { tmpDir() }

        JvsTestVine.Person fetch(Map args) { 
            String name = "" + nonResource + args.p1
            new JvsTestVine.Person(name:name) 
        }
    }

}



class JsonVineSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////

    def "dynamic vine method resource"() {
        when:
        def vine = new JvsTestVineWithResource()
        vine.method('VineMethodThatRefsNonexistentResource')
            .args(p1:'alice')
            .mode(CacheMode.IGNORE)
            .call()
        .getResult()

        then:
        Exception e = thrown()
        e instanceof groovy.lang.MissingPropertyException

        when:
        vine.vineMethodResource('nonResource', 'nr-')
        def res = vine
            .method('VineMethodThatRefsNonexistentResource')
            .args(p1:'alice')
            .mode(CacheMode.IGNORE)
            .call()
        .getResult()

        then:
        res != null
        res.name == 'nr-alice'
    }


    def "shared resource"() {
        when:
        def vine = new JvsTestVineWithResource()
        def res = vine
            .method('PersonVineMethod')
            .args(p1:'alice')
            .mode(CacheMode.IGNORE)
            .call()
        .getResult()

        then:
        res != null
        res.name == 'blah-alice'
    }


    def "call by method missing"() {
        when:
        def vine = new JvsTestVine()
        def res = vine.personVineMethod(p1:'alice').getResult()

        then:
        res != null
        res instanceof JvsTestVine.Person
        res.name == 'alice'

    }


    def "call with cache mode"() {
        when:
        def vine = new JvsTestVine()
        def res = vine
            .method('PersonVineMethod')
            .args(p1:'alice')
            .mode(CacheMode.OPTIONAL)
            .call()
        .getResult()

        then:
        res != null
        res instanceof JvsTestVine.Person
        res.name == 'alice'
    }


    def "cache file"() {
        when:
        def vine = new JvsTestVine()
        def cf = vine
            .method('PersonVineMethod')
            .args(p1:'alice')
        .cacheFile()

        then:
        cf != null
        cf instanceof File
    }


    def "cache file can be deleted"() {
        when:
        def vine = new JvsTestVine()
        def vm = vine.method('PersonVineMethod').args(p1:'alice')
        def cf = vm.cacheFile()

        then:
        cf != null
        cf instanceof File

        when:
        if (cf.exists()) cf.delete()
        vm.call(CacheMode.OPTIONAL)

        then:
        cf.exists()
    }


    def "vine method"() {
        when:
        def pv = new JvsTestVine()
        def vm = pv.method('PersonVineMethod')

        then:
        vm != null
        vm instanceof JvsTestVine.PersonVineMethod
    }


    def "convenience vine method by name"() {
        when:
        def pv = new JvsTestVine()
        def res = pv.personVineMethod(CacheMode.IGNORE, [p1:"alice"])

        // this would call the same method with the default cache mode
        //def res = pv.personVineMethod(p1:"alice")

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
        //vmcs.contains(JvsTestVine.VineMethodWithResource)
    }


}