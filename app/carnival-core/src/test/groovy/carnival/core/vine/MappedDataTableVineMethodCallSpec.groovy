package carnival.core.vine



import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared
import carnival.util.MappedDataTable


class MdtvmcsVine {
    @ToString(includeNames=true)
    static class Person { String name }

    static class PersonVineMethod extends MappedDataTableVineMethod { 
        MappedDataTable fetch(Map args) {
            def mdt = createDataTable(idFieldName:'ID')
            mdt.dataAdd(id:'1', name:args.p1)
            mdt
        }
    }
}


class MappedDataTableVineMethodCallSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // classes
    ///////////////////////////////////////////////////////////////////////////

    @ToString(includeNames=true)
    static class Person { String name }

    static class PersonVineMethod extends MappedDataTableVineMethod { 
        MappedDataTable fetch(Map args) {
            def mdt = createDataTable(idFieldName:'ID')
            mdt.dataAdd(id:'1', name:args.p1)
            mdt
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
        def dtf = mc1.writeDataTableFiles(tmpDir)
        println "dtf: $dtf"
        def mc2 = MappedDataTableVineMethodCall.createFromFiles(dtf)

        then:
        mc1 != null
        mc2 != null
        mc1.thisClass == mc2.thisClass
        mc1.vineMethodClass == mc2.vineMethodClass
        mc1.arguments != null
        mc2.arguments != null
        mc1.arguments.size() == mc2.arguments.size()
        mc1.arguments.get('p1') != null
        mc1.arguments.get('p1') == mc2.arguments.get('p1')
        mc1.result != null
        mc2.result != null
        mc1.result instanceof MappedDataTable
        mc2.result instanceof MappedDataTable
        mc1.result.data.size() == mc2.result.data.size()
        mc1.result.dataGet('1', 'name') == mc2.result.dataGet('1', 'name')
    }


    def "write file"() {
        given:
        File tmpDir = tmpDir()

        when:
        def pv = new PersonVineMethod()
        def mc = pv.call(CacheMode.IGNORE, [p1:"alice"])
        def mcf = mc.writeDataTableFiles(tmpDir)
        println "mcf: $mcf"

        then:
        mcf != null
        mcf.exist()

        when:
        def mct = mcf.data.text
        println "mct: $mct"

        then:
        mct != null
    }


    def "computed names differ by enclosing class"() {
        when:
        def pvm1 = new PersonVineMethod()
        def mc1 = pvm1.call(CacheMode.IGNORE, [a:'a'])
        def cn1 = mc1.computedName()

        def pvm2 = new MdtvmcsVine.PersonVineMethod()
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
        def pvm = new PersonVineMethod()
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
        def pvm = new PersonVineMethod()
        def mc = pvm.call(CacheMode.IGNORE, [a:'a'])
        def cn = mc.computedName()

        then:
        cn.startsWith("carnival-core-vine-MappedDataTableVineMethodCallSpec-PersonVineMethod")
        cn =~ /[0-9a-f]{32}/
    }


    def "computed name no args"() {
        when:
        def pvm = new PersonVineMethod()
        def mc = pvm.call(CacheMode.IGNORE, [:])
        def cn = mc.computedName()

        then:
        cn == "carnival-core-vine-MappedDataTableVineMethodCallSpec-PersonVineMethod"
    }


}

