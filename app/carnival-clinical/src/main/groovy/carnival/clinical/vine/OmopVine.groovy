package carnival.clinical.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.sql.*

import carnival.core.config.RelationalDatabaseConfig

import carnival.util.GenericDataTable
import carnival.core.vine.GenericDataTableVineMethod
import carnival.core.vine.CachingVine
import carnival.core.vine.RelationalVinePostgres

import carnival.util.KeyType
import carnival.util.SqlUtils

import java.security.MessageDigest

// For mocking with testing
interface OmopVineInterface {	

}

/**
 * Vine is the superclass of objects that interact read and write data to
 * data sources.
 *
 */
class OmopVine extends RelationalVinePostgres implements CachingVine, OmopVineInterface {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////
	static Logger sqllog = LoggerFactory.getLogger('sql')
	static Logger elog = LoggerFactory.getLogger('db-entity-report')
	static Logger log = LoggerFactory.getLogger('carnival')

	static MessageDigest MD5 = MessageDigest.getInstance("MD5")
	static String dbName = ""

	/** */
	static public OmopVine createFromDatabaseConfigFile(String filename, String dbName) {
		def db = RelationalDatabaseConfig.getDatabaseConfigFromFile(filename, "omop")
		this.dbName = dbName
		assert (this.dbName)
		assert (this.dbName != "")
		return new OmopVine(db)
	}

	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////////////
	public OmopVine(RelationalDatabaseConfig rdbConfig) {
		super(rdbConfig)
	}

	///////////////////////////////////////////////////////////////////////////
	//
	// VINE METHODS
	//
	///////////////////////////////////////////////////////////////////////////

	/** */
	static class GetOmopPatientIds implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						"""+dbName+""".person.person_id
						
						from """+dbName+""".person

						"""

        GenericDataTable.MetaData meta(Map args = [:]) {
        	validateArgs(args)
            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-patient-all_ids-${inputHash}",
                idFieldName:'person_id',
                idKeyType:KeyType.GENERIC_PATIENT_ID
            ) 

        }

        void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.limit)
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.limit.toString().isInteger())
	        }
	        if (args.reapAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && args.limit))
	        }
	    }

        GenericDataTable fetch(Map args) {
        	validateArgs(args)
            log.trace "GetRecords.fetch()"
            //log.trace(q)
            if (args.limit) sqlQuery += " LIMIT $args.limit "
            sqllog.info(sqlQuery)

            def gdt = createEmptyDataTable(args)
            enclosingVine.withSql { sql ->
                sql.eachRow(sqlQuery) { row ->
                    gdt.dataAdd(row)
                }
            }
            return gdt
        }
    }

	/** */
	static class GetOmopPatientDemographicData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						"""+dbName+""".person.person_id,
						"""+dbName+""".person.person_source_value, 
						"""+dbName+""".person.birth_datetime,
						"""+dbName+""".person.race_concept_id,
						"""+dbName+""".person.gender_concept_id,
						gender_concept.concept_name as gender_concept_name,
						race_concept.concept_name as race_concept_name,
						race_concept.concept_code as race_concept_code,
						gender_concept.concept_code as gender_concept_code

						from """+dbName+""".person

						inner join """+dbName+""".concept gender_concept
						on """+dbName+""".person.gender_concept_id =
						gender_concept.concept_id

						inner join """+dbName+""".concept race_concept
						on """+dbName+""".person.race_concept_id =
						race_concept.concept_id

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.ids || args.limit)
	        if (args.ids)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (!args.limit)
	        	assert (args.ids.size() > 0)
	        }
	        if (args.reapAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && (args.ids || args.limit)))
	        	if (args.reapAllData == true) assert (!args.limit && !args.ids)
	        }
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (!args.ids)
	        	assert (args.limit > 0)
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-patient-demographics-${inputHash}",
                idFieldName:'person_source_value',
                idKeyType:KeyType.GENERIC_PATIENT_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)
            def gdt = createEmptyDataTable(args)

            if (args.limit) sqlQuery += " LIMIT $args.limit "
            if (args.ids) 
            {
            	sqlQuery += " AND person_id IN SUB_WHERE_CLAUSE "
            	sqllog.info(sqlQuery)
            	gdt = enclosingVine.populateGenericDataTable(sqlQuery, gdt, args.ids)
            }
            else
            {
            	sqllog.info(sqlQuery)
	            enclosingVine.withSql { sql ->
	                sql.eachRow(sqlQuery) { row ->
	                    gdt.dataAdd(row)
	                }
	            }
            }
            return gdt
        }
    }

	static class GetOmopHealthcareEncounterData implements GenericDataTableVineMethod {

		def sqlQuery = """
						select 
						person.person_id,
						person.person_source_value,
						visit_occurrence.visit_occurrence_id,
						visit_occurrence.visit_source_value,
						visit_occurrence.visit_start_datetime

						from """+dbName+""".visit_occurrence 

						inner join """+dbName+""".person
						on """+dbName+""".visit_occurrence.person_id =
						"""+dbName+""".person.person_id

						where 
						person.person_id is not null
						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.ids || args.limit)
	        if (args.ids)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (!args.limit)
	        	assert (args.ids.size() > 0)
	        }
	        if (args.reapAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && (args.ids || args.limit)))
	        	if (args.reapAllData == true) assert (!args.limit && !args.ids)
	        }
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (!args.ids)
	        	assert (args.limit > 0)
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-healthcare-encounters-${inputHash}",
                idFieldName:'visit_occurrence_id',
                idKeyType:KeyType.ENCOUNTER_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)
            def gdt = createEmptyDataTable(args)
            if (args.limit) sqlQuery += " LIMIT $args.limit "
            if (args.ids) 
            {
            	sqlQuery += " AND person.person_source_value IN SUB_WHERE_CLAUSE "
            	sqllog.info(sqlQuery)
            	gdt = enclosingVine.populateGenericDataTable(sqlQuery, gdt, args.ids)
            }
            else
            {
            	//log.trace(q)
	            sqllog.info(sqlQuery)
	            enclosingVine.withSql { sql ->
	                sql.eachRow(sqlQuery) { row ->
	                    gdt.dataAdd(row)
	                }
	            }
            }
            return gdt
        }
    }

    static class GetOmopDiagnosisData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select

						"""+dbName+""".visit_occurrence.visit_occurrence_id,
						"""+dbName+""".visit_occurrence.visit_source_value,
						"""+dbName+""".concept.concept_name as condition_concept_name,
						"""+dbName+""".concept.concept_code as condition_concept_code,
						"""+dbName+""".condition_occurrence.condition_concept_id,
						"""+dbName+""".concept.vocabulary_id as condition_vocabulary_id,
						"""+dbName+""".condition_occurrence.condition_occurrence_id

						from """+dbName+""".condition_occurrence

						inner join """+dbName+""".concept on
						"""+dbName+""".condition_occurrence.condition_concept_id = 
						"""+dbName+""".concept.concept_id

						inner join """+dbName+""".visit_occurrence on
						"""+dbName+""".condition_occurrence.visit_occurrence_id = 
						"""+dbName+""".visit_occurrence.visit_occurrence_id

						where 
						
						"""+dbName+""".condition_occurrence.visit_occurrence_id is not null
						
						and 
						
						"""+dbName+""".concept.concept_code != 'No matching concept'

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.ids || args.limit)
	        if (args.ids)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.ids.size() > 0)
	        }
	        if (args.reapAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && (args.ids || args.limit)))
	        	if (args.reapAllData == true) assert (!args.limit && !args.ids)
	        }
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (!args.ids)
	        	assert (args.limit > 0)
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-diagnoses-${inputHash}",
                idFieldName:'condition_occurrence_id',
                idKeyType:KeyType.ENCOUNTER_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)
            def gdt = createEmptyDataTable(args)
            if (args.limit) sqlQuery += " LIMIT $args.limit "
            if (args.ids) 
            {
            	sqlQuery += " AND visit_occurrence.visit_source_value IN SUB_WHERE_CLAUSE "
            	sqllog.info(sqlQuery)
            	gdt = enclosingVine.populateGenericDataTable(sqlQuery, gdt, args.ids)
            }
            else
            {
            	//log.trace(q)
	            sqllog.info(sqlQuery)
	            enclosingVine.withSql { sql ->
	                sql.eachRow(sqlQuery) { row ->
	                    gdt.dataAdd(row)
	                }
	            }
            }
            return gdt
        }
    }

    static class GetOmopMedicationData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						"""+dbName+""".visit_occurrence.visit_occurrence_id,
						"""+dbName+""".visit_occurrence.visit_source_value,
						"""+dbName+""".concept.concept_name as drug_concept_name,
						"""+dbName+""".concept.vocabulary_id as drug_vocabulary_id,
						"""+dbName+""".concept.concept_code as drug_concept_code,
						"""+dbName+""".drug_exposure.drug_exposure_id as drug_id,
						"""+dbName+""".drug_exposure.drug_concept_id

						from """+dbName+""".drug_exposure

						inner join """+dbName+""".concept on
						"""+dbName+""".drug_exposure.drug_concept_id = 
						"""+dbName+""".concept.concept_id

						inner join """+dbName+""".visit_occurrence on
						"""+dbName+""".drug_exposure.visit_occurrence_id = 
						"""+dbName+""".visit_occurrence.visit_occurrence_id

						where """+dbName+""".drug_exposure.visit_occurrence_id is not null
						
						and 
						
						"""+dbName+""".concept.concept_code != 'No matching concept'

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.ids || args.limit)
	        if (args.ids)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.ids.size() > 0)
	        }
	        if (args.reapAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && (args.ids || args.limit)))
	        	if (args.reapAllData == true) assert (!args.limit && !args.ids)
	        }
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (!args.ids)
	        	assert (args.limit > 0)
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-medications-${inputHash}",
                idFieldName:'drug_id',
                idKeyType:KeyType.ENCOUNTER_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)
            def gdt = createEmptyDataTable(args)
            if (args.limit) sqlQuery += " LIMIT $args.limit "
            if (args.ids)
            {
            	sqlQuery += " AND visit_occurrence.visit_source_value IN SUB_WHERE_CLAUSE "
            	sqllog.info(sqlQuery)
            	gdt = enclosingVine.populateGenericDataTable(sqlQuery, gdt, args.ids)
            }
            else
            {
            	//log.trace(q)
	            sqllog.info(sqlQuery)
	            enclosingVine.withSql { sql ->
	                sql.eachRow(sqlQuery) { row ->
	                    gdt.dataAdd(row)
	                }
	            }
            }
            return gdt
        }
    }

    static class GetOmopMeasurementData implements GenericDataTableVineMethod {

		def sqlQuery = """
						select

						"""+dbName+""".visit_occurrence.visit_occurrence_id,
						"""+dbName+""".visit_occurrence.visit_source_value,
						"""+dbName+""".measurement.value_as_number,
						"""+dbName+""".measurement.measurement_id,
						"""+dbName+""".measurement.measurement_concept_id,
						unit_concept.concept_name as unit_concept_name,
						"""+dbName+""".measurement.unit_concept_id,
						meas_concept.concept_name as measurement_concept_name

						from """+dbName+""".measurement 
						inner join """+dbName+""".concept meas_concept on
						"""+dbName+""".measurement.measurement_concept_id = 
						meas_concept.concept_id

						inner join """+dbName+""".concept unit_concept on
						"""+dbName+""".measurement.unit_concept_id =
						unit_concept.concept_id

						inner join """+dbName+""".visit_occurrence on
						"""+dbName+""".measurement.visit_occurrence_id = 
						"""+dbName+""".visit_occurrence.visit_occurrence_id
						
						OMOP_CODE_WHERE_CLAUSE

						AND
						value_as_number is not null
						AND
						"""+dbName+""".measurement.visit_occurrence_id is not null
						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.ids || args.limit)
	        assert (args.omopConceptMap)
	        assert (args.omopConceptMap.get("bmi"))
	        assert (args.omopConceptMap.get("weight"))
	        assert (args.omopConceptMap.get("height"))
	        assert (args.omopConceptMap.get("diastolicBloodPressure"))
	        assert (args.omopConceptMap.get("systolicBloodPressure"))
	        assert (args.reapAllData || args.ids || args.limit)
	        if (args.ids)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.ids.size() > 0)
	        }
	        if (args.reapAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && (args.ids || args.limit)))
	        	if (args.reapAllData == true) assert (!args.limit && !args.ids)
	        }
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (!args.ids)
	        	assert (args.limit > 0)
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-measurements-${inputHash}",
                idFieldName:'measurement_id',
                idKeyType:KeyType.ENCOUNTER_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)

            //get omop concept IDs for bmi, height, and weight
            def omopHeightId = args.omopConceptMap.get("height")
            def omopWeightId = args.omopConceptMap.get("weight")
            def omopBmiId = args.omopConceptMap.get("diastolicBloodPressure")
            def diastolicBloodPressure = args.omopConceptMap.get("bmi")
            def systolicBloodPressure = args.omopConceptMap.get("systolicBloodPressure")

            def sqlInsert = """where measurement_source_concept_id in (
						'$omopBmiId',
						'$omopWeightId',
						'$omopHeightId',
						'$diastolicBloodPressure',
						'$systolicBloodPressure'
						)"""

            def sqlToRun = sqlQuery.replaceAll('OMOP_CODE_WHERE_CLAUSE', sqlInsert)
            def gdt = createEmptyDataTable(args)

            if (args.limit) sqlToRun += " LIMIT $args.limit "
            if (args.ids) 
            {
            	sqlToRun += " AND visit_occurrence.visit_source_value IN SUB_WHERE_CLAUSE "
            	sqllog.info(sqlQuery)
            	gdt = enclosingVine.populateGenericDataTable(sqlToRun, gdt, args.ids)
            }
            else
            {
            	//log.trace(q)
	            sqllog.info(sqlToRun)
	            enclosingVine.withSql { sql ->
	                sql.eachRow(sqlToRun) { row ->
	                    gdt.dataAdd(row)
	                }
	            }
            }
            return gdt
        }
    }

    public GenericDataTable populateGenericDataTable(String parameterizedSql, GenericDataTable outputDataTable, ArrayList<String> filterValues, Integer chunkSize = 30000) {
		assert parameterizedSql.contains("SUB_WHERE_CLAUSE")

		def inClauses = filterValues.collate(chunkSize)

        inClauses.eachWithIndex { chunkedValues, inClauseIndex ->
			withSql { sql ->
                log.trace "populateGenericDataTable: ${inClauseIndex + 1} OF ${inClauses.size()}"

            	def inClause = SqlUtils.inClause(chunkedValues)
                def q = parameterizedSql.replaceAll('SUB_WHERE_CLAUSE', "$inClause")
                sqllog.info q

                outputDataTable.dataAddAllGroovyRowResults(sql.rows(q), true)
            }
        }

        //outputDataTable.writeFiles(Defaults.dataCacheDirectory)

        return outputDataTable
	}
}