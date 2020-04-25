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

import carnival.core.*
import carnival.core.graph.*
import carnival.pmbb.*
import carnival.pmbb.graph.*
import carnival.pmbb.vine.*

import com.google.common.collect.*

import carnival.core.vine.Vine
import groovy.transform.*



/** */
class PatientMatcher {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    static Logger log = LoggerFactory.getLogger(PatientMatcher)
	static Logger sqllog = LoggerFactory.getLogger('sql')    


	/**
	* Given a patientGroup and some patient strata, generate the critera counts for a matching control group
	* done for the demo, needs vetting
	*
	* @param CoreGraph coreGraph
	* @param Vertex includePgVert
	* @param Collection[PatientStrata] patientStrataGroups
	*
	*
	* @return Collection[criteriaGroups:[patientGroupVerts], count:int]	
	*
	*/
	static Collection demoGenerateCriteria(Map args) {
		assert args.containsKey('coreGraph')
		assert args.coreGraph instanceof CoreGraph

		assert args.containsKey('includePgVert')
		assert args.includePgVert instanceof Vertex

		assert args.containsKey('patientStratas') || args.containsKey('patientStrataVerts')

		if (args.containsKey('patientStratas')) assert args.patientStratas instanceof Collection<PatientStrata>
		if (args.containsKey('patientStrataVerts')) assert args.patientStrataVerts instanceof Collection<Vertex> 


		def g = args.coreGraph.graph.traversal(CarnivalTraversalSource.class)
		def graph = args.coreGraph.graph

		// get patient strata verts from patientStratas
		// i.e. [[pg:age20-30, pg:age31-40, pg:ageOther], [s:F, s:M]]
		def strataGroups = []
		if (args.containsKey('patientStratas')) {
			args.patientStratas.each { patientStrata ->
				def pgs = patientStrata.patientStrataGroups*.strataVertex
				pgs << patientStrata.unspecifiedPatientGroup.strataVertex

				assert pgs.size() > 0

				strataGroups << pgs
			}
		}
		else {
			args.patientStrataVerts.each {psVert ->
				def pgs = g.V(psVert).out('contains_group').toList()
				assert pgs.size() > 0
				strataGroups << pgs
			}
		}

		//println "strataGroups: $strataGroups"

		// generate all patient strata combinations
		// i.e.[ [pg:age20-30, s:F], [pg:age20-30, s:M], [pg:age31-40, s:F], [pg:age31-40, s:M], ...]
		def criteriaGroupsList = groovy.util.GroovyCollections.combinations(strataGroups)

		//println "criteriaGroupsList: $criteriaGroupsList"

		// query for counts
		def criteriaCountCyper = '''
MATCH
	(p:Patient)-[:is_member_of]->(incGroup:PatientGroup)
	SUB_CRITERIA_MATCH
WHERE
	id(incGroup) = $incGroupId
	SUB_CRITERIA_WHERE
RETURN 
	count(distinct p) as c
'''

		def criteria = []
		criteriaGroupsList.each { criteriaGroups ->	
			def SUB_CRITERIA_WHERE = ""
			def SUB_CRITERIA_MATCH = ""

			criteriaGroups.eachWithIndex { criteriaGroup, i ->
				def pgName = "cg${String.valueOf(i)}"
				def pgId = criteriaGroup.id()
				SUB_CRITERIA_MATCH = SUB_CRITERIA_MATCH + ", (p)-[:is_member_of]->($pgName:PatientGroup)"
				SUB_CRITERIA_WHERE = SUB_CRITERIA_WHERE + " AND id($pgName) = $pgId"
			}

			def cypherArgs = [incGroupId:args.includePgVert.id()]
			def exeCypher = criteriaCountCyper.replaceAll('SUB_CRITERIA_WHERE', SUB_CRITERIA_WHERE)
			exeCypher = exeCypher.replaceAll('SUB_CRITERIA_MATCH', SUB_CRITERIA_MATCH)

			//println "cypher: $exeCypher"
			def res = graph.cypher(exeCypher, cypherArgs).toList()
			//println "res: $res"
			def count = res[0].c
			if (count > 0) {
				criteria << [criteriaGroups:criteriaGroups, count:count]
			}
		}

		//println "criteria: $criteria"
		return criteria
	}


	/**
	 * Creates a patient group named cohortName, 
	 * Finds patients that match the given criteria and adds them to the patient group.
	 *
	 * @param CoreGraph coreGraph
	 * @param String cohortName
	 * @param Vertex includePgVert
	 * @param Collection[criteriaGroups:[patientGroupVerts], count:int] criteria
	 * TODO: @param String filter
	 * TODO: @param Priority
	 *
	 */
	static Map generateCohortGroup(Map args) {
		assert args.containsKey('coreGraph')
		assert args.coreGraph instanceof CoreGraph

		assert args.containsKey('cohortName')
		assert args.cohortName instanceof String

		assert args.containsKey('includePgVert')
		assert args.includePgVert instanceof Vertex

		assert args.containsKey('criteria')
		assert args.criteria instanceof Collection<Map>

		args.criteria.each {c -> 
			assert c.containsKey('criteriaGroups')
			assert c.criteriaGroups instanceof Collection<Vertex>
			assert c.containsKey('count')
		}

		def graph = args.coreGraph.graph

		def potCohortGroup = graph.addVertex(T.label, 'PatientGroup', 'name', String.valueOf("${args.cohortName}_potential"))
		def selCohortGroup = graph.addVertex(T.label, 'PatientGroup', 'name', String.valueOf("${args.cohortName}_selected"))


		String cypher = '''
MATCH 
	(incGroup:PatientGroup),
	(potCohortGroup:PatientGroup),
	(selCohortGroup:PatientGroup)
WHERE 
	id(incGroup) = $incGroupId AND
	id(potCohortGroup) = $potCohortGroupId AND
	id(selCohortGroup) = $selCohortGroupId
WITH 
	incGroup, potCohortGroup, selCohortGroup
MATCH 
	(p:Patient)-[:is_member_of]->(incGroup),
	(p)-[:is_identified_by]->(pkPatId:Identifier)-[:is_instance_of]->(:IdentifierClass {name:"pk_patient_id"})
	SUB_CRITERIA_MATCH
WHERE
	not ((p)-[:is_member_of]->(potCohortGroup))
	SUB_CRITERIA_WHERE
WITH 
	p, potCohortGroup, selCohortGroup
CREATE 
	(p)-[:is_member_of]->(potCohortGroup)
WITH 
	p, potCohortGroup, selCohortGroup
ORDER BY ID(p)
LIMIT SUB_CRITERIA_COUNT
CREATE 
	(p)-[:is_member_of]->(selCohortGroup)
RETURN (p)
		'''

		args.criteria.each { c ->
			def SUB_CRITERIA_WHERE = ""
			def SUB_CRITERIA_MATCH = ""
			def SUB_CRITERIA_COUNT = String.valueOf(c.count)

			c.criteriaGroups.eachWithIndex { criteriaGroup, i ->
				def pgName = "cg${String.valueOf(i)}"
				def pgId = criteriaGroup.id()
				SUB_CRITERIA_MATCH = SUB_CRITERIA_MATCH + ", (p)-[:is_member_of]->($pgName:PatientGroup)"
				SUB_CRITERIA_WHERE = SUB_CRITERIA_WHERE + " AND id($pgName) = $pgId"
			}

			def exeCypher = cypher.replaceAll('SUB_CRITERIA_WHERE', SUB_CRITERIA_WHERE)
			exeCypher = exeCypher.replaceAll('SUB_CRITERIA_MATCH', SUB_CRITERIA_MATCH)
			exeCypher = exeCypher.replaceAll('SUB_CRITERIA_COUNT', SUB_CRITERIA_COUNT)

			def cypherArgs = [incGroupId:args.includePgVert.id(), potCohortGroupId:potCohortGroup.id(), selCohortGroupId:selCohortGroup.id()]

			def criteriaGroupHumanReadableName = c.criteriaGroups*.value('name').join(',')
			log.trace "Finding ${c.count} matching controls for criteria: ${criteriaGroupHumanReadableName}"
			
			sqllog.debug "critera: $c"
			sqllog.debug "SUB_CRITERIA_MATCH: $SUB_CRITERIA_MATCH"
			sqllog.debug "SUB_CRITERIA_WHERE: $SUB_CRITERIA_WHERE"

			sqllog.info "exeCypher: $exeCypher"
			sqllog.info "cypherArgs: $cypherArgs"

			graph.cypher(exeCypher, cypherArgs).toList()
		}

		return [potCohortGroup:potCohortGroup, selCohortGroup:selCohortGroup]
	}


}