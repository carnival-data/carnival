package carnival.util


import java.text.SimpleDateFormat

import groovy.sql.*
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared



/**
 * gradle test --tests "carnival.core.util.FeatureReportSpec"
 *
 */
class FeatureReportSpec extends Specification {


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
    // FEATURE SET RECIPE
    ///////////////////////////////////////////////////////////////////////////






    ///////////////////////////////////////////////////////////////////////////
    // DATA ADD VARIABLES
    ///////////////////////////////////////////////////////////////////////////

    def "contains subject"() {
        given:
        def res
        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([
            FeatureReport.DataMode.ADD_SUBJECT
        ])

        expect:
        !frp.containsSubject("1")

        when:
        frp.addSubject("1")

        then:
        frp.containsSubject("1")
    }


    def "shift date time"() {
        given:
        def res
        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([
            FeatureReport.DataMode.ADD_SUBJECT, 
            FeatureReport.DataMode.ADD_FEATURE, 
            FeatureReport.DataMode.SET_FEATURE
        ])
        def dateShiftVals
        def rec

        when:
        (1..3).each { i ->
            def id = "$i"
            frp.addSubject(id)
            frp.addFeature(id, 'DATE', '2019-1-1')
            frp.addFeature(id, 'SOME_DATE_TIME', '2019-2-1 23:41:02')
            frp.addFeature(id, 'DATE_SOMETHING', '2019-3-10')
            frp.addFeature(id, 'SOME_OTHER_FIELD', 'abc')
        }

        dateShiftVals = [
            "1": -1,
            "2": -5,
            "3": -8
        ]

        res = frp.shiftDates(
            calendarUnit: Calendar.DAY_OF_YEAR, 
            shiftVals: dateShiftVals,
            dateFields: ['SOME_DATE_TIME'], 
            dateFormat: new SimpleDateFormat('yyyy-M-d HH:mm:ss')
        )

        then:
        res
        res.dateFields
        res.dateFields.size() == 1

        when:
        rec = frp.dataGet("1")
        println "rec: $rec"

        then:
        rec
        rec.size() == 5
        rec['SOME_DATE_TIME_SHIFTED'] == '2019-1-31 23:41:02'

        when:
        rec = frp.dataGet("2")
        println "rec: $rec"

        then:
        rec
        rec.size() == 5
        rec['SOME_DATE_TIME_SHIFTED'] == '2019-1-27 23:41:02'
    }



    def "shift dates basic"() {
        given:
        def res
        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([
            FeatureReport.DataMode.ADD_SUBJECT, 
            FeatureReport.DataMode.ADD_FEATURE, 
            FeatureReport.DataMode.SET_FEATURE
        ])
        def dateShiftVals
        def rec

        when:
        (1..3).each { i ->
            def id = "$i"
            frp.addSubject(id)
            frp.addFeature(id, 'DATE', '1/1/2019')
            frp.addFeature(id, 'SOME_DATE', '2/1/2019')
            frp.addFeature(id, 'DATE_SOMETHING', '3/10/2019')
            frp.addFeature(id, 'SOME_OTHER_FIELD', 'abc')
        }

        dateShiftVals = [
            "1": -1,
            "2": -5,
            "3": -8
        ]

        res = frp.shiftDates(Calendar.DAY_OF_YEAR, dateShiftVals, 'M/d/yyyy')

        then:
        res
        res.dateFields
        res.dateFields.size() == 3
        res.dateFields.contains('DATE')
        res.dateFields.contains('SOME_DATE')
        res.dateFields.contains('DATE_SOMETHING')
        frp.keySet.contains('DATE' + FeatureReport.DATE_SHIFT_SUFFIX)
        frp.keySet.contains('SOME_DATE' + FeatureReport.DATE_SHIFT_SUFFIX)
        frp.keySet.contains('DATE_SOMETHING' + FeatureReport.DATE_SHIFT_SUFFIX)
        !frp.keySet.contains('DATE')
        !frp.keySet.contains('SOME_DATE')
        !frp.keySet.contains('DATE_SOMETHING')

        when:
        rec = frp.dataGet("1")
        println "rec: $rec"

        then:
        rec
        rec.size() == 5
        !rec.containsKey('DATE')
        !rec.containsKey('SOME_DATE')
        !rec.containsKey('DATE_SOMETHING')
        rec['DATE_SHIFTED'] == '12/31/2018'
        rec['SOME_DATE_SHIFTED'] == '1/31/2019'
        rec['DATE_SOMETHING_SHIFTED'] == '3/9/2019'
        rec['SOME_OTHER_FIELD'] == 'abc'

        when:
        rec = frp.dataGet("2")
        println "rec: $rec"

        then:
        rec
        rec.size() == 5
        !rec.containsKey('DATE')
        !rec.containsKey('SOME_DATE')
        !rec.containsKey('DATE_SOMETHING')
        rec['DATE_SHIFTED'] == '12/27/2018'
        rec['SOME_DATE_SHIFTED'] == '1/27/2019'
        rec['DATE_SOMETHING_SHIFTED'] == '3/5/2019'
        rec['SOME_OTHER_FIELD'] == 'abc'
    }


    /**
     *   EMPI   STAGE   DATE
     *   1      3a      1/1/2019
     *   1      3       2/1/2019
     *   2      1       5/1/2017
     *   3      4       11/8/2016
     */    
    def "extractCompoundMultiFeatures includeNum"() {
        given:
        def res

        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        (1..3).each { frp.addSubject("$it") }

        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', STAGE:'3a', DATE:'1/1/2019']
        data << [EMPI:'1', STAGE:'3', DATE:'2/1/2019']
        data << [EMPI:'2', STAGE:'1', DATE:'5/1/2017']
        data << [EMPI:'3', STAGE:'4', DATE:'11/8/2016']

        frp.extractCompoundMultiFeatures(
            data:data, 
            subjectIdKey:'EMPI', 
            featureName:'SEVERITY', 
            featureValueKeys:['STAGE', 'DATE'],
            includeNum:true
        )

        println "frp.keySet: ${frp.keySet}"

        then:
        frp.keySetContains('EMPI')
        frp.keySetContains('SEVERITY_COUNT')
        frp.keySetContains('SEVERITY_1_STAGE')
        frp.keySetContains('SEVERITY_1_DATE')
        frp.keySetContains('SEVERITY_2_STAGE')
        frp.keySetContains('SEVERITY_2_DATE')

        frp.dataGet('1', 'SEVERITY_COUNT') == '2'
        frp.dataGet('1', 'SEVERITY_1_STAGE') == '3a'
        frp.dataGet('1', 'SEVERITY_1_DATE') == '1/1/2019'
        frp.dataGet('1', 'SEVERITY_2_STAGE') == '3'
        frp.dataGet('1', 'SEVERITY_2_DATE') == '2/1/2019'

        frp.dataGet('2', 'SEVERITY_COUNT') == '1'
        frp.dataGet('2', 'SEVERITY_1_STAGE') == '1'
        frp.dataGet('2', 'SEVERITY_1_DATE') == '5/1/2017'
        frp.dataGet('2', 'SEVERITY_2_STAGE') == null

        frp.dataGet('3', 'SEVERITY_COUNT') == '1'
        frp.dataGet('3', 'SEVERITY_1_STAGE') == '4'
        frp.dataGet('3', 'SEVERITY_1_DATE') == '11/8/2016'
        frp.dataGet('3', 'SEVERITY_2_STAGE') == null
    } 


    /**
     *   EMPI   STAGE   DATE
     *   1      3a      1/1/2019
     *   1      3       2/1/2019
     *   2      1       5/1/2017
     *   3      4       11/8/2016
     */    
    def "extractCompoundMultiFeatures"() {
        given:
        def res

        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        (1..3).each { frp.addSubject("$it") }

        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', STAGE:'3a', DATE:'1/1/2019']
        data << [EMPI:'1', STAGE:'3', DATE:'2/1/2019']
        data << [EMPI:'2', STAGE:'1', DATE:'5/1/2017']
        data << [EMPI:'3', STAGE:'4', DATE:'11/8/2016']

        frp.extractCompoundMultiFeatures(
            data:data, 
            subjectIdKey:'EMPI', 
            featureName:'SEVERITY', 
            featureValueKeys:['STAGE', 'DATE']
        )

        then:
        frp.keySetContains('EMPI')
        frp.keySetContains('SEVERITY_1_STAGE')
        frp.keySetContains('SEVERITY_1_DATE')
        frp.keySetContains('SEVERITY_2_STAGE')
        frp.keySetContains('SEVERITY_2_DATE')
        frp.dataGet('1', 'SEVERITY_1_STAGE') == '3a'
        frp.dataGet('1', 'SEVERITY_1_DATE') == '1/1/2019'
        frp.dataGet('1', 'SEVERITY_2_STAGE') == '3'
        frp.dataGet('1', 'SEVERITY_2_DATE') == '2/1/2019'
        frp.dataGet('2', 'SEVERITY_1_STAGE') == '1'
        frp.dataGet('2', 'SEVERITY_1_DATE') == '5/1/2017'
        frp.dataGet('2', 'SEVERITY_2_STAGE') == null
        frp.dataGet('3', 'SEVERITY_1_STAGE') == '4'
        frp.dataGet('3', 'SEVERITY_1_DATE') == '11/8/2016'
        frp.dataGet('3', 'SEVERITY_2_STAGE') == null
    } 


    /*
    EMPI   STAGE
    1      3a
    1      3
    2      1
    3      4
    4
    */    
    def "extractMultiFeatures empty data"() {
        given:
        def res

        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        (1..4).each { frp.addSubject("$it") }

        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', STAGE:'3a']
        data << [EMPI:'1', STAGE:'3']
        data << [EMPI:'2', STAGE:'1']
        data << [EMPI:'3', STAGE:'4']
        data << [EMPI:'4']

        frp.extractMultiFeatures(
            data:data, 
            subjectIdKey:'EMPI', 
            featureName:'SEVERITY', 
            featureValueKey:'STAGE',
            includeNum:true
        )

        then:
        frp.keySetContains('EMPI')
        frp.keySetContains('SEVERITY_1')
        frp.keySetContains('SEVERITY_2')
        frp.dataGet('1', 'SEVERITY_COUNT') == '2'
        frp.dataGet('1', 'SEVERITY_1') == '3a'
        frp.dataGet('1', 'SEVERITY_2') == '3'
        frp.dataGet('2', 'SEVERITY_COUNT') == '1'
        frp.dataGet('2', 'SEVERITY_1') == '1'
        frp.dataGet('2', 'SEVERITY_2') == null
        frp.dataGet('3', 'SEVERITY_COUNT') == '1'
        frp.dataGet('3', 'SEVERITY_1') == '4'
        frp.dataGet('3', 'SEVERITY_2') == null
        frp.dataGet('4', 'SEVERITY_COUNT') == '0'
    }    


    /*
    EMPI   STAGE
    1      3
    2      1
    3      4
    */    
    def "extractMultiFeatures no multiplicity"() {
        given:
        def res

        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        (1..3).each { frp.addSubject("$it") }

        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', STAGE:'3']
        data << [EMPI:'2', STAGE:'1']
        data << [EMPI:'3', STAGE:'4']

        frp.extractMultiFeatures(
            data:data, 
            subjectIdKey:'EMPI', 
            featureValueKey:'STAGE'
        )

        then:
        frp.keySetContains('EMPI')
        frp.keySetContains('STAGE')
        frp.dataGet('1', 'STAGE') == '3'
        frp.dataGet('2', 'STAGE') == '1'
        frp.dataGet('3', 'STAGE') == '4'
    }


    /*
    EMPI   STAGE
    1      3a
    1      3
    2      1
    3      4
    */    
    def "extractMultiFeaturesIncludeNum"() {
        given:
        def res

        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        (1..3).each { frp.addSubject("$it") }

        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', STAGE:'3a']
        data << [EMPI:'1', STAGE:'3']
        data << [EMPI:'2', STAGE:'1']
        data << [EMPI:'3', STAGE:'4']

        frp.extractMultiFeatures(
            data:data, 
            subjectIdKey:'EMPI', 
            featureValueKey:'STAGE',
            includeNum:true
        )

        then:
        frp.keySetContains('EMPI')
        frp.keySetContains('STAGE_COUNT')
        frp.keySetContains('STAGE_1')
        frp.keySetContains('STAGE_2')
        frp.dataGet('1', 'STAGE_COUNT') == '2'
        frp.dataGet('1', 'STAGE_1') == '3a'
        frp.dataGet('1', 'STAGE_2') == '3'
        frp.dataGet('2', 'STAGE_COUNT') == '1'
        frp.dataGet('2', 'STAGE_1') == '1'
        frp.dataGet('2', 'STAGE_2') == null
        frp.dataGet('3', 'STAGE_COUNT') == '1'
        frp.dataGet('3', 'STAGE_1') == '4'
        frp.dataGet('3', 'STAGE_2') == null
    }          



    /*
    EMPI   STAGE
    1      3a
    1      3
    2      1
    3      4
    */    
    def "extractMultiFeaturesDefaultName"() {
        given:
        def res

        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        (1..3).each { frp.addSubject("$it") }

        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', STAGE:'3a']
        data << [EMPI:'1', STAGE:'3']
        data << [EMPI:'2', STAGE:'1']
        data << [EMPI:'3', STAGE:'4']

        frp.extractMultiFeatures(
            data:data, 
            subjectIdKey:'EMPI', 
            featureValueKey:'STAGE'
        )

        then:
        frp.keySetContains('EMPI')
        frp.keySetContains('STAGE_1')
        frp.keySetContains('STAGE_2')
        frp.dataGet('1', 'STAGE_1') == '3a'
        frp.dataGet('1', 'STAGE_2') == '3'
        frp.dataGet('2', 'STAGE_1') == '1'
        frp.dataGet('2', 'STAGE_2') == null
        frp.dataGet('3', 'STAGE_1') == '4'
        frp.dataGet('3', 'STAGE_2') == null
    }  


    /*
    EMPI   STAGE
    1      3a
    1      3
    2      1
    3      4
    */    
    def "extractMultiFeatures"() {
        given:
        def res

        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        (1..3).each { frp.addSubject("$it") }

        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', STAGE:'3a']
        data << [EMPI:'1', STAGE:'3']
        data << [EMPI:'2', STAGE:'1']
        data << [EMPI:'3', STAGE:'4']

        frp.extractMultiFeatures(
            data:data, 
            subjectIdKey:'EMPI', 
            featureName:'SEVERITY', 
            featureValueKey:'STAGE'
        )

        then:
        frp.keySetContains('EMPI')
        frp.keySetContains('SEVERITY_1')
        frp.keySetContains('SEVERITY_2')
        frp.dataGet('1', 'SEVERITY_1') == '3a'
        frp.dataGet('1', 'SEVERITY_2') == '3'
        frp.dataGet('2', 'SEVERITY_1') == '1'
        frp.dataGet('2', 'SEVERITY_2') == null
        frp.dataGet('3', 'SEVERITY_1') == '4'
        frp.dataGet('3', 'SEVERITY_2') == null
    }    


    def "multiFeatureNames"() {
        given:
        def frp = new FeatureReport(name:'frp-test', idFieldName:'EMPI')
        def res
        List<Map<String,String>> data = new ArrayList<Map>()

        when:
        data << [EMPI:'1', TUMOR_ID:'1', STAGE:'3a']
        data << [EMPI:'1', TUMOR_ID:'2', STAGE:'3']
        data << [EMPI:'2', TUMOR_ID:'3', STAGE:'1']
        data << [EMPI:'3', TUMOR_ID:'4', STAGE:'4']
        data << [EMPI:'4', RANDO_FEATURE:'rd1']
        data << [EMPI:'4', RANDO_FEATURE:'rd2']
        data << [EMPI:'4', RANDO_FEATURE:'rd3']

        res = frp.multiFeatureNames(
            data:data, 
            subjectIdKey:'EMPI', 
            featureValueKeys:['STAGE'],
            featureName:'SEVERITY'
        )

        then:
        res != null
        res.size() == 2
        res.find {it == 'SEVERITY_1'}
        res.find {it == 'SEVERITY_2'}
    }


    def "extract boolean features"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        frp.addSubject('id1')
        def data
        final String TSTR = String.valueOf(true)

        expect:
        !frp.keySetContains('c1')
        !frp.keySetContains('c2')

        when:
        data = new ArrayList<Map>()
        data << [subjectId:'id1', codeAssignment:'c1']
        data << [subjectId:'id1', codeAssignment:'c2']
        frp.extractBooleanFeatures(data, 'subjectId', 'codeAssignment')
        
        then:
        frp.keySetContains('c1')
        frp.keySetContains('c2')
        frp.dataGet('id1', 'c1') == TSTR
        frp.dataGet('id1', 'c2') == TSTR
    }


    def "mode extract boolean features"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataMode(FeatureReport.DataMode.ADD_SUBJECT)
        frp.addSubject('id1')
        
        def data = new ArrayList<Map>()
        data << [subjectId:'id1', codeAssignment:'c1']
        data << [subjectId:'id1', codeAssignment:'c2']

        when:
        frp.extractBooleanFeatures(data, 'subjectId', 'codeAssignment')

        then:
        e = thrown()
        println "${e.message}"

        when:
        frp.dataModes << FeatureReport.DataMode.ADD_FEATURE
        frp.extractBooleanFeatures(data, 'subjectId', 'codeAssignment')

        then:
        1 == 1
    }


    def "remove feature set"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])

        when:
        frp.addSubject('id1')
        frp.addSubject('id2')
        frp.addFeatures('id1', ['v1':'v11', 'v2':'v21'])
        frp.addFeatures('id2', ['v1':'v12', 'v2':'v22'])
        
        then:
        frp.data.size() == 2
        frp.data.each { k, v -> 
            assert v.size() == 3
            assert v.containsKey('ID')
            assert v.containsKey('V1')
            assert v.containsKey('V2')
        }
        frp.dataGet('id1', 'v1') == 'v11'
        frp.dataGet('id1', 'v2') == 'v21'
        frp.dataGet('id2', 'v1') == 'v12'
        frp.dataGet('id2', 'v2') == 'v22'

        when:
        frp.removeFeatureSet('V1')

        then:
        frp.data.size() == 2
        frp.data.each { k, v -> 
            assert v.size() == 2
            assert v.containsKey('ID')
            assert v.containsKey('V2')
        }
        frp.dataGet('id1', 'v2') == 'v21'
        frp.dataGet('id2', 'v2') == 'v22'
    }


    def "add features"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        frp.addSubject('id1')

        when:
        frp.addFeatures('id1', ['v1':'v11', 'v2':'v21'])
        
        then:
        frp.dataGet('id1', 'v1') == 'v11'
        frp.dataGet('id1', 'v2') == 'v21'

        when:
        frp.addFeatures('id1', ['v1':'v11', 'v2':'v21'])

        then:
        e = thrown()
        println "${e.message}"
    }


    def "mode add features"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataMode(FeatureReport.DataMode.ADD_SUBJECT)
        frp.addSubject('id1')

        when:
        frp.addFeatures('id1', ['v1':'v11', 'v2':'v21'])

        then:
        e = thrown()
        println "${e.message}"

        when:
        frp.dataModes << FeatureReport.DataMode.ADD_FEATURE
        frp.addFeatures('id1', ['v1':'v11', 'v2':'v21'])

        then:
        1 == 1
    }


    def "contains feature"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])
        frp.addSubject('id1')

        when:
        frp.addFeature('id1', 'v1', 'v11')
        boolean b1 = frp.containsFeature('id1', 'v1')
        boolean b2 = frp.containsFeature('id1', 'v2')

        then:
        b1
        !b2
    }


    def "add feature"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataModes([FeatureReport.DataMode.ADD_SUBJECT, FeatureReport.DataMode.ADD_FEATURE])

        frp.addSubject('id1')

        when:
        frp.addFeature('id1', 'v1', 'v11')

        then:
        frp.dataGet('id1', 'v1') == 'v11'

        when:
        frp.addFeature('id1', 'v1', 'v11')

        then:
        e = thrown()
        println "${e.message}"
    }


    def "mode add feature"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')
        frp.dataMode(FeatureReport.DataMode.ADD_SUBJECT)
        frp.addSubject('id1')

        when:
        frp.addFeature('id1', 'v1', 'v11')

        then:
        e = thrown()
        println "${e.message}"

        when:
        frp.dataModes << FeatureReport.DataMode.ADD_FEATURE
        frp.addFeature('id1', 'v1', 'v11')

        then:
        1 == 1
    }


    def "add subject with features"() {
        given:
        Exception e
        def features
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')

        when:
        frp.dataModes << FeatureReport.DataMode.ADD_SUBJECT
        features = ['v1':'v11', 'v2':'v21']
        frp.addSubject('id1', features)

        then:
        frp.containsIdentifier('id1')
        frp.dataGet('id1', 'v1') == 'v11'
        frp.dataGet('id1', 'v2') == 'v21'
    }


    def "add subject"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')

        when:
        frp.dataModes << FeatureReport.DataMode.ADD_SUBJECT
        frp.addSubject('id1')

        then:
        frp.containsIdentifier('id1')
    }


    def "mode add subject"() {
        given:
        Exception e
        def frp = new FeatureReport(name:'frp-test', idFieldName:'ID')

        when:
        frp.addSubject('id1')

        then:
        e = thrown()
        println "${e.message}"

        when:
        frp.dataModes << FeatureReport.DataMode.ADD_SUBJECT
        frp.addSubject('id1')

        then:
        1 == 1
    }




}