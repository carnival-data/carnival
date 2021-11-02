package carnival.util


import java.time.LocalDate
import groovy.sql.*
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared



/**
 * gradle test --tests "carnival.core.util.GenericDataTableSpec"
 *
 */
class GenericDataTableSpec extends Specification {


    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    @Shared testDate



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
        testDate = new Date("01/01/2000")
    } 


    def cleanupSpec() {
    }




    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////

    java.sql.ResultSet mockResultSet(Map m) {
        def entries = m.entrySet().toList()

        def getColumnCount = {
            return entries.size()
        }
        def getColumnLabel = { i ->
            if (i > entries.size()) throw new RuntimeException("$i")
            else return entries[i-1].getKey()
        }

        def getObject = { Object key ->
            m.get(key)
        }

        [
            getMetaData: {[
                getColumnCount: getColumnCount,
                getColumnLabel: getColumnLabel
            ] as java.sql.ResultSetMetaData },
            getObject: getObject
        ] as java.sql.ResultSet
    }



    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    def "constructor"() {
        given:
        def mdt
        def data
        def args
        Throwable e
        def now = new Date()

        when:
        mdt = new GenericDataTable(name:'mdt-test')

        then:
        mdt != null
        mdt.name == "mdt-test"
        mdt.queryDate.getTime() - now.getTime() <= 1000
        mdt.dataSourceDateOfUpdate == null

        when:
        mdt = new GenericDataTable(name:'mdt-test')

        then:
        mdt != null
        mdt.name == "mdt-test"

        when:
        mdt = new GenericDataTable(name:'mdt-test', queryDate:testDate)

        then:
        mdt != null
        mdt.name == "mdt-test"
        mdt.queryDate == testDate

        when:
        mdt = new GenericDataTable(name:'mdt-test', queryDate:DataTable.dataSetDateToString(testDate))

        then:
        mdt != null
        mdt.name == "mdt-test"
        mdt.queryDate == testDate
    }



    ///////////////////////////////////////////////////////////////////////////
    // FACTORY
    ///////////////////////////////////////////////////////////////////////////

    def "createFromFile"() {
        given:
        def edf = new File('build/md-test.csv')
        def emf = new File('build/md-test.yaml')
        if (edf.exists()) edf.delete()
        if (emf.exists()) emf.delete()
        def mdt0 = new GenericDataTable(name:'mdt-test', queryDate:testDate)
        mdt0.dataAddAllListList([
            ['id', 'v1']
            , ['id1', 'v11']
            , ['id2', 'v12']
        ])
        def buildDir = new File('build')
        def res = mdt0.writeFiles(buildDir)

        when:
        def mdt = GenericDataTable.createFromFiles(buildDir, 'mdt-test')

        then:
        mdt != null
        mdt.name == 'mdt-test'
        mdt.queryDate == testDate

        when:
        def data = mdt.data

        then:
        data != null
        data.size() == 2

        when:
        def r1 = data[0]

        then:
        r1['ID'] == 'id1'
        r1['V1'] == 'v11'

        when:
        def r2 = data[1]

        then:
        r2['ID'] == 'id2'
        r2['V1'] == 'v12'
    }



    ///////////////////////////////////////////////////////////////////////////
    // FILE I/O
    ///////////////////////////////////////////////////////////////////////////


    def "writeDataFile data"() {
        given:
        def mdt
        def data
        def file
        Throwable e

        when:
        mdt = new GenericDataTable(name:'mdt-test',)
        mdt.dataAddAllListList([
            ['id', 'v1']
            , ['id1', 'v11']
            , ['id2', 'v12']
        ])
        file = mdt.writeDataFile(new File('build'))

        then:
        file.exists()
        file.name.endsWith('.csv')
        file.canonicalPath.indexOf('build') >= 0

        when:
        def fdata = DataTable.readDataFromCsvFile(file)
        def rec = fdata[idx]
        println "rec: $rec"

        then:
        rec['ID'] == id
        rec['V1'] == v1

        where:
        idx << [0, 1]
        id << ['id1', 'id2']
        v1 << ['v11', 'v12']
    }

    
    def "writeMetaFile data"() {
        given:
        def mdt
        def data
        def file
        def metaDataSourceDateOfUpdate
        def meta
        Throwable e
        def now = new Date()
        Date yesterday = new Date(LocalDate.now().minusDays(1).toEpochDay())
        def yaml = new org.yaml.snakeyaml.Yaml(new DataTableRepresenter())

        when:
        mdt = new GenericDataTable(name:'mdt-test')
        file = mdt.writeMetaFile(new File('build'))
        println "${file.name} ${file.canonicalPath}"

        then:
        file.exists()
        file.name == 'mdt-test.yaml'
        file.canonicalPath.indexOf('build') >= 0

        when:
        meta = yaml.load(file.text)

        then:
        meta.name == 'mdt-test'
        meta.queryDate.getTime() - now.getTime() <= 1000

        when:
        mdt = new GenericDataTable(name:'mdt-test', queryDate:testDate)
        mdt.dataSourceDateOfUpdate = yesterday
        file = mdt.writeMetaFile(new File('build'))
        println "${file.name} ${file.canonicalPath}"

        then:
        file.exists()
        file.name == 'mdt-test.yaml'
        file.canonicalPath.indexOf('build') >= 0

        when:
        meta = yaml.load(file.text)

        then:
        meta.name == 'mdt-test'
        meta.queryDate.equals(testDate)
        meta.dataSourceDateOfUpdate == yesterday
        //metaDataSourceDateOfUpdate.getTime().equals(yesterday.getTime())
        //meta.dataSourceDateOfUpdate.getTime() - yesterday.getTime() <= 1000 // milliseconds truncated during yaml conversion
    }



    ///////////////////////////////////////////////////////////////////////////
    // DATA ADD
    ///////////////////////////////////////////////////////////////////////////

    def "dataAdd ResultSet"() {
        given:
        def mdt
        def res
        Throwable e
        mdt = new GenericDataTable(name:'mdt-test')

        when:
        res = [
            getMetaData: { [
                getColumnCount: { 
                    return 2 
                },
                getColumnLabel: { i -> 
                    switch (i) {
                        case 1: return 'id';
                        case 2: return 'd1';
                        default: throw new RuntimeException("$i") 
                    } 
                }
            ] as java.sql.ResultSetMetaData },
            getObject: { Object key ->
                switch (String.valueOf(key)) {
                    case 'id': return 'id1';
                    case 'd1': return 'd11';
                    default: throw new RuntimeException("$key") 
                } 
            }
        ] as java.sql.ResultSet
        mdt.dataAdd(res)

        then:
        mdt.data[0].size() == 2

        when:
        res = mockResultSet(ID:'id2', d1:'d12')
        mdt.dataAdd(res)

        then:
        mdt.data[1].size() == 2
    }



    def "dataAdd GroovyRowResult"() {
        given:
        def mdt
        def res
        Throwable e
        mdt = new GenericDataTable(name:'mdt-test')

        when:
        res = [id:'id1', d1:'d11'] as GroovyRowResult
        mdt.dataAdd(res)

        then:
        mdt.data[0].size() == 2

        when:
        res = [ID:'id2', d1:'d12'] as GroovyRowResult
        mdt.dataAdd(res)

        then:
        mdt.data[1].size() == 2
    }


    def "dataAdd Map date values"() {
        given:
        def mdt
        mdt = new GenericDataTable(name:'mdt-test')

        when:
        def cal = Calendar.getInstance()

        // 2002-2-3 11:13AM
        cal.set(2002, Calendar.FEBRUARY, 03) 
        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 13)
        cal.set(Calendar.SECOND, 27)

        def data1 = [ID:'id1', v1:cal.time]
        mdt.dataAdd(data1)

        then:
        mdt.data.size() == 1

        when:
        def d1 = mdt.data[0]

        then:
        d1['V1'] == SqlUtils.timestampAsString(cal.time)
    }


    def "dataAdd Map values single row"() {
        given:
        def mdt
        mdt = new GenericDataTable(name:'mdt-test')

        when:
        def m = [ID:id]
        m.putAll(data)
        mdt.dataAdd(m)

        then:
        matchData(mdt, id, m)

        where:
        id            | data
        'id1'         | [V1:'v11']
        'id1'         | [V1:'v11', V2:'v21']
        'id1'         | [V1:11]
        //'id1'         | [V1:new Date()]
    }


    void matchData(GenericDataTable mdt, String id, Map data) {
        assert mdt.data.size() == 1

        def mdtDataMap = mdt.data[0]
        assert mdtDataMap
        assert mdtDataMap.size() > 0

        data.each { k, v ->
            def fn = DataTable.fieldName(k)
            assert mdtDataMap.containsKey(fn)

            def mdtValue = mdtDataMap.get(fn)
            assert (mdtValue == v) || (mdtValue == String.valueOf(v))
        }
        assert mdtDataMap?.keySet()?.size() == data?.keySet()?.size()
    }


    def "dataAdd Map field names are cleaned up"() {
        given:
        def mdt
        mdt = new GenericDataTable(name:'mdt-test')

        when:
        def m = [id:'1']
        m.put(fn, 'v1')
        mdt.dataAdd(m)

        then:
        mdt.keySet.size() == 2
        mdt.keySet.find { it == 'ID' }
        mdt.keySet.find { it == 'A' }

        where:
        fn << ['a', ' a', 'a ', 'A']
    }


    def "dataAdd Map keys are mapped to field names"() {
        given:
        def mdt
        Throwable e
        mdt = new GenericDataTable(name:'mdt-test')

        when:
        def m = [:]
        m.put(idKey, idVal)
        m.put('v1', 'v11' )
        mdt.dataAdd(m)

        then:
        mdt.data.size() == 1

        when:
        def data = mdt.data.get(0)
        println "data: $data"

        then:
        data.size() == 2
        data.ID == "A${idCount}"
        data.V1 == 'v11'

        where:
        idKey << ['id', ' id', 'id ', 'ID', 'Id', 'iD']
        idVal << (1..6).collect { "A$it" }
        idCount << (1..6)
    }


    def "dataAdd Map basics"() {
        given:
        def mdt
        Throwable e
        def row

        when:
        mdt = new GenericDataTable(name:'mdt-test')

        then:
        mdt.data.size() == 0

        when:
        mdt.dataAdd(id:'id1', v1:'v11')

        then:
        mdt.data.size() == 1

        when:
        row = mdt.data[0]

        then:
        row.size() == 2
        row.ID == 'id1'
        row.V1 == 'v11'
    }


    def "GenericDataTable constructor"() {
        given:
        def mdt

        when:
        mdt = new GenericDataTable(name:'mdt-test')

        then:
        mdt.name == 'mdt-test'
    }


    def "setOrderedKeys input"() {
        given:
        def dt = new GenericDataTable(name:"ordered-keys-test")

        expect:
        dt.keySet instanceof TreeSet
        dt.keySet.size() == 0

        when:
        dt.setOrderedKeys(keyVals)

        then:
        dt.keySet instanceof LinkedHashSet
        dt.keySet.size() == 0

        when:
        dt.dataAdd(inputRow)

        then:
        dt.keySet instanceof LinkedHashSet
        dt.keySet.size() == expected.size()
        assertExpectedKeys(dt, expected)

        where:
        keyVals         | expected      | inputRow
        []              | ['A']         | [a:"test"]
        []              | ['A', 'B']    | [a:"test1", b:"test2"]
        []              | ['A', 'B']    | [A:"test1", B:"test2"]
        []              | ['B', 'A']    | [b:"test1", a:"test2"]
    }

    private void assertExpectedKeys(DataTable dt, List<String> expected) {
        assert dt.keySet.size() == expected.size()
        dt.keySet.eachWithIndex { k, i ->
            //println "$k $i ${expected[i]}"
            assert k == expected[i]
        }
        //expected.each { ev -> assert dt.keySet.find { it == ev } }
    }


}