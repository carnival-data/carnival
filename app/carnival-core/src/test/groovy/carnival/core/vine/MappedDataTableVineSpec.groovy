package carnival.core.vine


import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared

import carnival.util.MappedDataTable
import carnival.util.DataTableFiles



class MdtTestVine implements Vine { 
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

    class PersonVineMethod extends MappedDataTableVineMethod { 
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

        MappedDataTable fetch(Map args) {
            def mdt = createDataTable(idFieldName:'ID')
            mdt.dataAdd(id:'1', name:args.p1)
            mdt
        }
    }
}


class MdtTestVineWithResource extends MdtTestVine { 

    String sharedResource = 'blah-'

    class VineMethodSharedResource extends MappedDataTableVineMethod { 
        //@Override
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

        MappedDataTable fetch(Map args) {
            def mdt = createDataTable(idFieldName:'ID')
            def name = "" + sharedResource + args.p1
            mdt.dataAdd(id:'1', name:name)
            mdt
        }
    }
}



class MdtTestVineDefault implements Vine { 
    @ToString(includeNames=true)
    static class Person { String name }

    class PersonVineMethod extends MappedDataTableVineMethod { 
        MappedDataTable fetch(Map args) {
            def mdt = createDataTable(idFieldName:'ID')
            mdt.dataAdd(id:'1', name:args.p1)
            mdt
        }
    }
}



class MappedDataTableVineSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////

    def "shared resource"() {
        when:
        def vine = new MdtTestVineWithResource()
        def res = vine
            .method('VineMethodSharedResource')
            .args(p1:'alice')
            .mode(CacheMode.IGNORE)
            .call()
        .getResult()

        then:
        res != null
        res.dataGet('1', 'name') == 'blah-alice'
    }


    def "call with cache mode"() {
        when:
        def vine = new MdtTestVine()
        def res = vine
            .method('PersonVineMethod')
            .args(p1:'alice')
            .mode(CacheMode.OPTIONAL)
            .call()
        .getResult()

        then:
        res != null
        res instanceof MappedDataTable
        res.data.size() == 1
        res.dataGet('1', 'name') == 'alice'
    }


    def "cache files"() {
        when:
        def vine = new MdtTestVine()
        def cfs = vine
            .method('PersonVineMethod')
            .args(p1:'alice')
        .cacheFiles()

        then:
        cfs != null
        cfs instanceof DataTableFiles
    }


    def "cache files can be deleted"() {
        when:
        def vine = new MdtTestVine()
        def vm = vine.method('PersonVineMethod').args(p1:'alice')
        def cfs = vm.cacheFiles()

        then:
        cfs != null
        cfs instanceof DataTableFiles

        when:
        if (cfs.exist()) cfs.toMap().values().each { it.delete() }
        vm.call(CacheMode.OPTIONAL)

        then:
        cfs.exist()
    }


    def "default cache directory"() {
        when:
        def vine = new MdtTestVineDefault()
        def vm = vine.method('PersonVineMethod').args(p1:'alice')
        def cfs = vm.cacheFiles()

        then:
        cfs != null
        cfs instanceof DataTableFiles

        when:
        if (cfs.exist()) cfs.toMap().values().each { it.delete() }
        vm.call(CacheMode.OPTIONAL)

        then:
        cfs.exist()
    }


    def "vine method"() {
        when:
        def pv = new MdtTestVine()
        def vm = pv.method('PersonVineMethod')

        then:
        vm != null
        vm instanceof MdtTestVine.PersonVineMethod
    }


    def "convenience vine method by name"() {
        when:
        def pv = new MdtTestVine()
        def res = pv.personVineMethod(CacheMode.IGNORE, [p1:"alice"])

        // this would call the same method with the default cache mode
        //def res = pv.personVineMethod(p1:"alice")

        then:
        noExceptionThrown()
        res != null
        res instanceof MappedDataTableVineMethodCall
    }


    def "create vine method instance"() {
        when:
        def pv = new MdtTestVine()
        def vmi = pv.createVineMethodInstance('personVineMethod')

        then:
        vmi != null
        vmi instanceof MdtTestVine.PersonVineMethod
    }


    def "find vine method class"() {
        when:
        def pv = new MdtTestVine()
        def vmc = pv.findVineMethodClass('personVineMethod')

        then:
        vmc != null
        vmc == MdtTestVine.PersonVineMethod
    }


    def "all vine method classes"() {
        when:
        def pv = new MdtTestVine()
        def vmcs = pv.allVineMethodClasses()

        then:
        vmcs != null
        vmcs.size() == 1
        vmcs.contains(MdtTestVine.PersonVineMethod)
        //vmcs.contains(MdtTestVine.VineMethodWithResource)
    }


}