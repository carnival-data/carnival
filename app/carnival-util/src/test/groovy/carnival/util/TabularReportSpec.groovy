package carnival.util


import groovy.sql.*
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared




/**
 * gradle test --tests "carnival.core.util.TabularReportSpec"
 *
 */
class TabularReportSpec extends Specification {


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
    // DATA ADD VARIABLES
    ///////////////////////////////////////////////////////////////////////////


    def "dataAddAttributes"() {
        given:
        def mdt
        def rec
        
        when:
        mdt = new TabularReport(name:'mdt-test')

        then:
        mdt.keySet.size() == 0

        when:
        mdt.dataAdd(ID:'id1', V1:'v11')
        mdt.dataAdd(ID:'id2', V1:'v12')

        then:
        mdt.data.size() == 2
        mdt.keySet.size() == 2
        mdt.keySet.contains('ID')
        mdt.keySet.contains('V1')

        when:
        rec = mdt.data[0]

        then:
        rec['ID'] == 'id1'
        rec['V1'] == 'v11'

        when:
        rec = mdt.data[1]

        then:
        rec['ID'] == 'id2'
        rec['V1'] == 'v12'

        when:
        mdt.start()
        mdt.dataAddAttributes('ID', [ID:'id1', V2:'v21'])

        then:
        mdt.keySet.size() == 3
        mdt.keySet.contains('V2')

        when:        
        rec = mdt.data[0]

        then:
        rec['ID'] == 'id1'
        rec['V1'] == 'v11'
        rec['V2'] == 'v21'
    }


    def "dataAddAttributes attributes do not change existing values"() {
        given:
        Exception e
        def rec
        def mdt = new TabularReport(name:'mdt-test')

        when:
        mdt.dataAdd(ID:'id1', V1:'v11')
        mdt.dataAdd(ID:'id2', V1:'v12')

        then:
        mdt.data.size() == 2

        when:
        rec = mdt.data[0]

        then:
        rec.size() == 2
        rec['ID'] == 'id1'
        rec['V1'] == 'v11'

        when:
        mdt.dataAddAttributes('ID', [ID:'id2', V1:'v12_', V2:'v33'])

        then:
        e = thrown()
    }


    def "dataAddAttributes supplied id must match against existing data"() {
        given:
        Exception e
        def rec
        def mdt = new TabularReport(name:'mdt-test')

        when:
        mdt.dataAdd(ID:'id1', V1:'v11')
        mdt.dataAdd(ID:'id2', V1:'v12')

        then:
        mdt.data.size() == 2

        when:
        rec = mdt.data[0]

        then:
        rec.size() == 2
        rec['ID'] == 'id1'
        rec['V1'] == 'v11'

        when:
        mdt.dataAddAttributes('ID', [ID:'id3', V2:'v33'])

        then:
        e = thrown()
    }


    def "dataAddAttributes attributes contain id value"() {
        given:
        Exception e
        def rec
        def mdt = new TabularReport(name:'mdt-test')

        when:
        mdt.dataAdd(ID:'id1', V1:'v11')
        mdt.dataAdd(ID:'id2', V1:'v12')

        then:
        mdt.data.size() == 2

        when:
        rec = mdt.data[0]

        then:
        rec.size() == 2
        rec['ID'] == 'id1'
        rec['V1'] == 'v11'

        when:
        mdt.dataAddAttributes('ID', [ID_:'id1', V2:'v21'])

        then:
        e = thrown()
    }


    def "dataAddAttributes idField is a secondary id"() {
        given:
        Exception e
        def rec
        def mdt = new TabularReport(name:'mdt-test')

        when:
        mdt.dataAdd(ID:'id1', V1:'v11')
        mdt.dataAdd(ID:'id2', V1:'v12')

        then:
        mdt.data.size() == 2

        when:
        rec = mdt.data[0]

        then:
        rec.size() == 2
        rec['ID'] == 'id1'
        rec['V1'] == 'v11'

        when:
        mdt.dataAddAttributes('ID', [ID:'id1', V2:'v21'])

        then:
        e = thrown()
    }


}