package carnival.clinical.graph



import groovy.util.AntBuilder
import groovy.transform.ToString

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jVertex

import carnival.core.*
import carnival.core.graph.*
import carnival.pmbb.*
import carnival.pmbb.graph.*
import carnival.pmbb.vine.*

import com.google.common.collect.*

import carnival.core.vine.Vine
import groovy.transform.*



/** */
class EncounterMatcher {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** carnival logger */
    static Logger log = LoggerFactory.getLogger('carnival')

    /** sql logger */
	static Logger sqllog = LoggerFactory.getLogger('sql')   

	/** error logger */
	static Logger elog = LoggerFactory.getLogger('db-entity-report') 


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a patient group named cohortName, 
	 * Finds patients that match the given criteria and adds them to the patient group.
	 *
	 * @param coreGraph        The graph over which to operate
	 * @param cohortName       The name of the cohort to create. 
	 * @param candidatesGroup  The encounter group vertex linked to the encounters that
	 *                         are candidates to be in the output cohort.  
	 * @param criteria         The criteria to use to perform the match. Criteria is a
	 *                         list of maps, each map must contain the following:
	 *                         criteriaGroups:Collection<EncounterStrataGroup>
	 *                         count:int
	 *
	 * @param criteria.criteriaGroups The criteria groups that contain entities
	 *                                from which we will draw members of the
	 *                                output cohort. For example, if we are
	 *                                looking for patients, there might criteria
	 *                                group for age 20-30 and bmi 25-30. If both
	 *                                these groups are present in a criteria group,
	 *                                we will look for patients that are present in
	 *                                both groups to put in the output cohort.
	 * @param criteria.count          The number of entities we want to put in the
	 *                                output cohort.
	 *
	 */
	static Map generateCohortGroup(Map args) {
		assert args.containsKey('coreGraph')
		assert args.coreGraph instanceof CoreGraph

		assert args.containsKey('cohortName')
		assert args.cohortName instanceof String

		assert args.containsKey('candidatesGroup')
		assert args.candidatesGroup instanceof Vertex
		assert args.candidatesGroup.label() == "EncounterGroup"
		def candidatesGroupId = args.candidatesGroup.id()

		assert args.containsKey('criteria')
		assert args.criteria instanceof Collection<Map>

		/** 
		 * if true, the output cohort will be selected such that a patient is
		 * represented only once.
		 */
		boolean uniquePatients = (args.uniquePatients != null) ? args.uniquePatients : true

		args.criteria.each {c -> 
			assert c.containsKey('criteriaGroups')
			assert c.criteriaGroups instanceof Collection<Vertex>
			assert c.containsKey('count')
		}

		def graph = args.coreGraph.graph
		def g = graph.traversal(CarnivalTraversalSource.class)
		String cypher 
		def res

		// the group of encounters that could be selected for the output cohort
		def potCohortGroup = graph.addVertex(
			T.label, 
			'EncounterGroup', 
			'name', 
			String.valueOf("${args.cohortName}_potential")
		)

		// the output cohort
		def selCohortGroup = graph.addVertex(
			T.label, 
			'EncounterGroup', 
			'name', 
			String.valueOf("${args.cohortName}_selected")
		)

		// uniquify the candidate pool by patient
		def uniqueCandidatesGroupId
		if (uniquePatients) {
			// find uniqueCandidatesGroup if it exists
			def uniqueCandidatesGroupName = args.candidatesGroup.value('name')
			assert uniqueCandidatesGroupName
			uniqueCandidatesGroupName += "-unique"
			log.trace "uniqueCandidatesGroupName:$uniqueCandidatesGroupName"
			
			def uniqueCandidatesGroup
			cypher = '''
MATCH (uniqueCandidatesGroup:EncounterGroup)
WHERE uniqueCandidatesGroup.name = $uniqueCandidatesGroupName
RETURN uniqueCandidatesGroup
'''			
			res = graph.cypher(cypher, [uniqueCandidatesGroupName:uniqueCandidatesGroupName]).toList()
			//log.debug "res1: $res"

			if (res.size() == 0) {
				cypher = '''
CREATE (uniqueCandidatesGroup:EncounterGroup {name:$uniqueCandidatesGroupName} )
RETURN uniqueCandidatesGroup
'''	
				res = graph.cypher(cypher, [uniqueCandidatesGroupName:uniqueCandidatesGroupName]).toList()
				//log.debug "res2: $res"
				uniqueCandidatesGroup = res[0].uniqueCandidatesGroup
			} else {
				uniqueCandidatesGroup = res[0].uniqueCandidatesGroup
			}

			// we have an empty unique candidates group
			assert uniqueCandidatesGroup
			uniqueCandidatesGroupId = uniqueCandidatesGroup.id()

			//  if they exist, delete existing relations
			cypher = '''
MATCH 
	(uniqueCandidatesGroup:EncounterGroup)
	, (enc:BiobankEncounter)-[r:is_member_of]->(uniqueCandidatesGroup)
WHERE 
	ID(uniqueCandidatesGroup) = $uniqueCandidatesGroupId
DELETE 
	r
RETURN 
	COUNT(r) AS num_deleted_relations
'''
			res = graph.cypher(cypher, [uniqueCandidatesGroupId:uniqueCandidatesGroupId]).next()
			log.trace "deleted ${res} relations"

			// add unique candidates to the unique candidate group
			cypher = '''
MATCH 
	(candidatesGroup:EncounterGroup) 
	, (uniqueCandidatesGroup:EncounterGroup)
	, (enc:BiobankEncounter)-[:is_member_of]->(candidatesGroup)
	, (p:Patient)-[:participated_in_encounter]->(enc)
WHERE 
	ID(candidatesGroup) = $candidatesGroupId
	AND ID(uniqueCandidatesGroup) = $uniqueCandidatesGroupId
WITH 
	candidatesGroup, uniqueCandidatesGroup, p, COLLECT(enc)[0] AS enc
CREATE 
	(enc)-[:is_member_of]->(uniqueCandidatesGroup)
RETURN 
	COUNT(enc) AS num_unique_candidates
'''			
			res = graph.cypher(
				cypher, 
				[uniqueCandidatesGroupId:uniqueCandidatesGroupId, candidatesGroupId:candidatesGroupId]
			).toList()
			log.trace "created unique candidates $res $uniqueCandidatesGroupId $candidatesGroupId"
		}

		// the cypher to select controls and put them in the potential and selected cohorts
		cypher = '''
MATCH 
	(candidatesGroup:EncounterGroup),
	(potCohortGroup:EncounterGroup),
	(selCohortGroup:EncounterGroup)
WHERE 
	id(candidatesGroup) = $candidatesGroupId AND
	id(potCohortGroup) = $potCohortGroupId AND
	id(selCohortGroup) = $selCohortGroupId
WITH 
	candidatesGroup, potCohortGroup, selCohortGroup
MATCH 
	(entity:BiobankEncounter)-[:is_member_of]->(candidatesGroup)
	SUB_CRITERIA_MATCH
WHERE
	not ((entity)-[:is_member_of]->(potCohortGroup))
	SUB_CRITERIA_WHERE
WITH 
	entity, potCohortGroup, selCohortGroup
CREATE 
	(entity)-[:is_member_of]->(potCohortGroup)
WITH 
	entity, potCohortGroup, selCohortGroup
ORDER BY 
	ID(entity)
LIMIT 
	SUB_CRITERIA_COUNT
CREATE 
	(entity)-[:is_member_of]->(selCohortGroup)
RETURN 
	entity
		'''

		args.criteria.each { c ->
			def SUB_CRITERIA_WHERE = ""
			def SUB_CRITERIA_MATCH = ""
			def SUB_CRITERIA_COUNT = String.valueOf(c.count)

			c.criteriaGroups.eachWithIndex { criteriaGroup, i ->
				def pgName = "cg${String.valueOf(i)}"
				def pgId = criteriaGroup.id()
				SUB_CRITERIA_MATCH += ", (entity)-[:is_member_of]->($pgName:EncounterGroup)"
				SUB_CRITERIA_WHERE += " AND id($pgName) = $pgId"
			}

			def exeCypher = cypher

			exeCypher = exeCypher
				.replaceAll('SUB_CRITERIA_WHERE', SUB_CRITERIA_WHERE)
				.replaceAll('SUB_CRITERIA_MATCH', SUB_CRITERIA_MATCH)
				.replaceAll('SUB_CRITERIA_COUNT', SUB_CRITERIA_COUNT)

			def cypherArgs = [
				potCohortGroupId:potCohortGroup.id(), 
				selCohortGroupId:selCohortGroup.id()
			]
			if (uniquePatients) cypherArgs << [candidatesGroupId:uniqueCandidatesGroupId]
			else cypherArgs << [candidatesGroupId:args.candidatesGroupId]

			sqllog.debug "critera: $c"
			sqllog.debug "SUB_CRITERIA_MATCH: $SUB_CRITERIA_MATCH"
			sqllog.debug "SUB_CRITERIA_WHERE: $SUB_CRITERIA_WHERE"

			sqllog.info "exeCypher: $exeCypher"
			sqllog.info "cypherArgs: $cypherArgs"

			def entities = graph.cypher(exeCypher, cypherArgs).toList()

			if (entities.size() != c.count) {
				def emsg = "criteria:$c num_encounters:${entities.size()}"
				log.warn emsg
				elog.warn emsg
			}
		}

		return [potCohortGroup:potCohortGroup, selCohortGroup:selCohortGroup]
	}


}