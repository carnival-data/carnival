package carnival.clinical.vine

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.*

import java.sql.ResultSet
import groovy.sql.*

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import org.apache.commons.io.FileUtils

import org.apache.tinkerpop.gremlin.neo4j.structure.*

import java.security.MessageDigest

import carnival.core.*
import carnival.core.matcher.*
import carnival.util.*
import carnival.core.vine.*

//import carnival.core.relational.domain.*
//import carnival.core.relational.feature.CompoundFeature
import carnival.util.MappedDataTable

import carnival.util.Defaults

import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.util.*
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.vocabulary.XMLSchema

import java.security.MessageDigest

/**
 * gradle test --tests "carnival.clinical.vine.OmopVineSpec"
 * gradle -Dtest.vine.live.data=true test --tests "carnival.clinical.vine.OmopVineSpec"
 *
 */
class OmopVineSpec extends Specification {

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
    static MessageDigest MD5 = MessageDigest.getInstance("MD5")

    @Shared omopVine


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
        // get config
        def configFilePath = Defaults.findApplicationConfigurationFile().canonicalPath

        // create the necessary vines
        println "creating Vines..."
        omopVine = OmopVine.createFromDatabaseConfigFile(configFilePath)
        [omopVine].each { it.cacheMode = CachingVine.CacheMode.IGNORE }
    } 

    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////
    @IgnoreIf({ !Boolean.valueOf(properties['test.vine.live.data']) })
    def "test get omop patient demographic data"() {

       when:
       def resultSize = 10
       def gdt = omopVine.getOmopPatientDemographicData(limit:resultSize)
       /*def resultSize = null
       def gdt = omopVine.getOmopPatientDemographicData(reapAllData:true)*/

       then:
       gdt
       gdt.data
       gdt.data.size() > 0

       def columnNames = ['PERSON_ID', 'PERSON_SOURCE_VALUE', 
                          'GENDER_CONCEPT_NAME', 'RACE_CONCEPT_NAME', 
                          'BIRTH_DATETIME']

       columnNames.each { columnName ->

           def res = []
           gdt.data.each { row -> res += row.get(columnName)}

           if (resultSize) assert (res.size() == resultSize)
           res.each { record ->
               assert (record != null)
               if (columnName == 'PERSON_ID') assert (record.isInteger())
               if (columnName == 'GENDER_CONCEPT_NAME') 
               {
                  assert (record == "MALE" || 
                        record == "FEMALE" || 
                        record == "No matching concept")
               }
           }
       }
    }

    @IgnoreIf({ !Boolean.valueOf(properties['test.vine.live.data']) })
    def "test get omop healthcare encounter data"() {

       when:
       def resultSize = 10
       def gdt = omopVine.getOmopHealthcareEncounterData(limit:resultSize)
       /*def resultSize = null
       def gdt = omopVine.getOmopHealthcareEncounterData(reapAllData:true)*/

       then:
       gdt
       gdt.data
       gdt.data.size() > 0

       def columnNames = ['PERSON_ID', 'VISIT_OCCURRENCE_ID', 'VISIT_SOURCE_VALUE', 'VISIT_START_DATETIME', ]

       columnNames.each { columnName ->

           when:
           def res = []
           gdt.data.each { row -> res += row.get(columnName)}

           then:
           if (resultSize) assert (res.size() == resultSize)
           res.each { record ->
               assert (record != null)
               if (columnName == 'PERSON_ID') assert (record.isInteger())
               if (columnName == 'VISIT_OCCURRENCE_ID') assert (record.isInteger())
           }

       }
    }   

    @IgnoreIf({ !Boolean.valueOf(properties['test.vine.live.data']) })
    def "test get omop diagnosis data"() {

       when:
       def resultSize = 10
       def gdt = omopVine.getOmopDiagnosisData(limit:resultSize)
       /*def resultSize = null
       def gdt = omopVine.getOmopDiagnosisData(reapAllData:true)*/

       then:
       gdt
       gdt.data
       gdt.data.size() > 0

       def columnNames = ['VISIT_OCCURRENCE_ID', 'CONDITION_CONCEPT_NAME']

       columnNames.each { columnName ->

           when:
           def res = []
           gdt.data.each { row -> res += row.get(columnName)}

           then:
           if (resultSize) assert (res.size() == resultSize)
           res.each { record ->
               assert (record != null)
               if (columnName == 'VISIT_OCCURRENCE_ID') assert (record.isInteger())
           }

       }
    }   

    @IgnoreIf({ !Boolean.valueOf(properties['test.vine.live.data']) })
    def "test get omop medications data"() {

       when:
       def resultSize = 10
       def gdt = omopVine.getOmopMedicationData(limit:resultSize)
       /*def resultSize = null
       def gdt = omopVine.getOmopMedicationData(reapAllData:true)*/

       then:
       gdt
       gdt.data
       gdt.data.size() > 0

       def columnNames = ['VISIT_OCCURRENCE_ID', 'DRUG_CONCEPT_NAME']

       columnNames.each { columnName ->

           when:
           def res = []
           gdt.data.each { row -> res += row.get(columnName)}

           then:
           if (resultSize) assert (res.size() == resultSize)
           res.each { record ->
               assert (record != null)
               if (columnName == 'VISIT_OCCURRENCE_ID') assert (record.isInteger())
           }

       }
    }   

    @IgnoreIf({ !Boolean.valueOf(properties['test.vine.live.data']) })
    def "test get omop measurement data"() {

       when:
       def resultSize = 10
       def gdt = omopVine.getOmopMeasurementData(limit:resultSize)
       /*def resultSize = null
       def gdt = omopVine.getOmopMeasurementData(reapAllData:true)*/

       then:
       gdt
       gdt.data
       gdt.data.size() > 0

       def columnNames = ['VISIT_OCCURRENCE_ID', 'MEASUREMENT_CONCEPT_NAME', 'VALUE_AS_NUMBER', 'UNIT_CONCEPT_NAME']

       columnNames.each { columnName ->

           when:
           def res = []
           gdt.data.each { row -> res += row.get(columnName)}

           then:
           if (resultSize) assert (res.size() == resultSize)
           res.each { record ->
               assert (record != null)
               if (columnName == 'VISIT_OCCURRENCE_ID') assert (record.isInteger())
               if (columnName == 'VALUE_AS_NUMBER') assert (record.isNumber())
               if (columnName == 'MEASUREMENT_CONCEPT_NAME') 
               {
                  assert (record == "Body height" ||
                          record == "Body weight" ||
                          record == "Body mass index")
               }
               if (columnName == 'UNIT_CONCEPT_NAME') 
               {
                  assert (record == "kilogram per square meter" ||
                          record == "kilogram" ||
                          record == "centimeter")
               }
           }
       }
    }   
}