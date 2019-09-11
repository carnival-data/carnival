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

import java.security.MessageDigest

/**
 * Vine is the superclass of objects that interact read and write data to
 * data sources.
 *
 */
class OmopVine extends RelationalVinePostgres implements CachingVine {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////
	static Logger sqllog = LoggerFactory.getLogger('sql')
	static Logger elog = LoggerFactory.getLogger('db-entity-report')
	static Logger log = LoggerFactory.getLogger('carnival')

	static MessageDigest MD5 = MessageDigest.getInstance("MD5")

	/** */
	static public OmopVine createFromDatabaseConfigFile(String filename) {
		def db = RelationalDatabaseConfig.getDatabaseConfigFromFile(filename, "omop")
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
	static class GetOmopPatientDemographicData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						cdm_synthea10.person.person_id,
						cdm_synthea10.person.person_source_value, 
						cdm_synthea10.person.birth_datetime,
						gender_concept.concept_name as gender_concept_name,
						race_concept.concept_name as race_concept_name

						from cdm_synthea10.person

						inner join cdm_synthea10.concept gender_concept
						on cdm_synthea10.person.gender_concept_id =
						gender_concept.concept_id

						inner join cdm_synthea10.concept race_concept
						on cdm_synthea10.person.race_concept_id =
						race_concept.concept_id

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.limit)
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.limit.toString().isInteger())
	        }
	        if (args.realAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && args.limit))
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

            if (args.limit) sqlQuery += " LIMIT $args.limit "
            //log.trace(q)
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

	static class GetOmopHealthcareEncounterData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						person_id,
						visit_occurrence_id,
						visit_source_value,
						visit_start_datetime

						from cdm_synthea10.visit_occurrence 

						where 

						person_id is not null

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.limit)
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.limit.toString().isInteger())
	        }
	        if (args.realAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && args.limit))
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

            if (args.limit) sqlQuery += " LIMIT $args.limit "
            //log.trace(q)
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

    static class GetOmopDiagnosisData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						cdm_synthea10.condition_occurrence.visit_occurrence_id,
						cdm_synthea10.concept.concept_name as condition_concept_name

						from cdm_synthea10.condition_occurrence

						inner join cdm_synthea10.concept on
						cdm_synthea10.condition_occurrence.condition_concept_id = 
						cdm_synthea10.concept.concept_id

						where cdm_synthea10.condition_occurrence.visit_occurrence_id is not null

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.limit)
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.limit.toString().isInteger())
	        }
	        if (args.realAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && args.limit))
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-diagnoses-${inputHash}",
                idFieldName:'visit_occurrence_id',
                idKeyType:KeyType.ENCOUNTER_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)

            if (args.limit) sqlQuery += " LIMIT $args.limit "
            //log.trace(q)
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

    static class GetOmopMedicationData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						cdm_synthea10.drug_exposure.visit_occurrence_id,
						cdm_synthea10.concept.concept_name as drug_concept_name

						from cdm_synthea10.drug_exposure

						inner join cdm_synthea10.concept on
						cdm_synthea10.drug_exposure.drug_concept_id = 
						cdm_synthea10.concept.concept_id 

						where cdm_synthea10.drug_exposure.visit_occurrence_id is not null

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.limit)
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.limit.toString().isInteger())
	        }
	        if (args.realAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && args.limit))
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-medications-${inputHash}",
                idFieldName:'visit_occurrence_id',
                idKeyType:KeyType.ENCOUNTER_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)

            if (args.limit) sqlQuery += " LIMIT $args.limit "
            //log.trace(q)
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

    static class GetOmopMeasurementData implements GenericDataTableVineMethod {

		def sqlQuery = """

						select 

						cdm_synthea10.measurement.visit_occurrence_id,
						cdm_synthea10.measurement.value_as_number,
						meas_concept.concept_name as measurement_concept_name,
						unit_concept.concept_name as unit_concept_name

						from cdm_synthea10.measurement 

						inner join cdm_synthea10.concept meas_concept on
						cdm_synthea10.measurement.measurement_concept_id = 
						meas_concept.concept_id  

						inner join cdm_synthea10.concept unit_concept on
						cdm_synthea10.measurement.unit_concept_id =
						unit_concept.concept_id

						where measurement_concept_id in (
						/* BMI */
						'3038553',
						/* Height */
						'3036277',
						/* Weight */
						'3025315'
						)

						AND

						value_as_number is not null

						AND

						cdm_synthea10.measurement.visit_occurrence_id is not null

						"""

		/** validate arguments */
	    void validateArgs(Map args = [:]) {
	        assert (args.reapAllData || args.limit)
	        if (args.limit)
	        {
	        	assert (!args.reapAllData || args.reapAllData == false)
	        	assert (args.limit.toString().isInteger())
	        }
	        if (args.realAllData)
	        {
	        	assert (args.reapAllData == true || (args.reapAllData == false && args.limit))
	        }
	    }

        GenericDataTable.MetaData meta(Map args = [:]) {
            validateArgs(args)

            // create a hashed string value
            def inputHash = MD5.digest(sqlQuery.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"omop-medications-${inputHash}",
                idFieldName:'visit_occurrence_id',
                idKeyType:KeyType.ENCOUNTER_ID
            ) 

        }

        GenericDataTable fetch(Map args) {
            log.trace "GetRecords.fetch()"
            validateArgs(args)

            if (args.limit) sqlQuery += " LIMIT $args.limit "
            //log.trace(q)
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
}



