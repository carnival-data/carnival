package carnival.clinical.graph



import java.util.concurrent.atomic.AtomicInteger

import groovy.util.AntBuilder
import groovy.util.logging.Slf4j
import groovy.transform.ToString

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

import carnival.core.graph.CarnivalTraversalSource

import com.google.common.collect.*

import carnival.core.vine.Vine
import groovy.transform.*




/**
 * The abstract superclass of all encounter strata.  It defines three methods that must be implemented and
 * provides a public method classifyEncounters() that will be called to classify given encounters as per
 * the strata implementation.
 *
 * (EncounterStrata)-[:contains_group]->(EncounterGroup)
 * (Encounter)-[:is_member_of]->(EncounterGroup)
 *
 */
@Slf4j
abstract class EncounterStrata {

	/** the core graph to use as the data source */
	CoreGraph coreGraph

	/** the vertex that will be the focal point of all data associated with the strata */
	Vertex vertex 

	/** the name of this strata */
	String name

	/** a map from encounter strata group to the criteria for that group */
	Map<EncounterStrataGroup, Object> strataGroups = [:]

	/** a strate for encounters who do not fit into other strata */
	EncounterStrataGroup unspecifiedEncounterGroup

	/**
	 * Returns a complete cypher statement that puts encounters into the appropriate strata
	 * based on a substituted SUB_WHERE_CLAUSE, which will be generated for each strata
	 * criteria by generateCriteriaWhereClause(criteria).
	 *
	 */
	abstract String getCriteraClassificationCypher()

	/**
	 * Returns the cypher classification field, which is used by generateCriteriaWhereClause
	 * to generate the where clause for a spefic criteria.  It should be a field from a node
	 * matched by the statement from getCriteraClassificationCypher().
	 *
	 */
	abstract String getCypherClassificationField()


	/**
	 * Must return Cypher where clause for the given criteria.
	 *
	 */
	abstract protected String generateCriteriaWhereClause(def criteria)


	/** 
	 * Constructor 
	 *
	 */
	public EncounterStrata(CoreGraph coreGraph, String name) {
		assert coreGraph
		this.coreGraph = coreGraph

		assert name
		this.name = name

		//def g = coreGraph.graph.traversal(CarnivalTraversalSource.class)
		vertex = coreGraph.graph.addVertex(T.label, 'EncounterStrata', 'name', name)

		// create "unspecified" group
		unspecifiedEncounterGroup = new EncounterStrataGroup(coreGraph, "unspecified", vertex)
	}


	/**
	 * Classifies all encounters that are in the encounter group.
	 *
	 * @param iputGroupVertex The encounter group that contains the encounters to be stratified.
	 *
	 */
	public void classify(Vertex iputGroupVertex) {
		assert iputGroupVertex

		strataGroups.each { outStrataGrp, criteria ->
			def SUB_WHERE_CLAUSE = generateCriteriaWhereClause(criteria)

			def cypher = criteraClassificationCypher.replaceAll('SUB_WHERE_CLAUSE', SUB_WHERE_CLAUSE)

			coreGraph.graph.cypher(
				cypher, 
				[
					outputGroupId:outStrataGrp.vertex.id(), 
					inputGroupId:iputGroupVertex.id()
				]
			).toList()
		}

		addUnclassifiedEncountersToUnspecifiedGroup(iputGroupVertex)
	}

	
	/**
	 *
	 *
	 */
	protected void addUnclassifiedEncountersToUnspecifiedGroup(Vertex inputGroupVertex) {
		def cypher = '''
MATCH 
	(strata:EncounterStrata), (outputGroup:EncounterGroup)
WHERE 
	ID(strata) = $strataId
	AND ID(outputGroup) = $outputGroupId
WITH 
	strata, outputGroup
MATCH 
	(enc)-[:is_member_of]->(inputGroup:EncounterGroup)
WHERE 
	ID(inputGroup) = $inputGroupId 
	AND NOT (enc)-[:is_member_of]->(:EncounterGroup)<-[:contains_group]-(strata)
CREATE 
	(enc)-[:is_member_of]->(outputGroup)
RETURN 
	enc
        '''

        assert vertex
        assert inputGroupVertex
        assert unspecifiedEncounterGroup

		coreGraph.graph.cypher(
			cypher, 
			[
				strataId:vertex.id(), 
				inputGroupId:inputGroupVertex.id(), 
				outputGroupId:unspecifiedEncounterGroup.vertex.id()
			]
		).toList()
	}
}




/**
 * Groups of stratified entities.
 *
 * For example, a "current age" strata might stratify encounters by ages 20-29, 30-39, etc.
 * Encounters whose current ages are 20-29 will be grouped in a EncounterStrataGroup.
 *
 */
@Slf4j
class EncounterStrataGroup {

	/** the data source */
	CoreGraph coreGraph

	/** a name for this strata group */
	String name

	/** the vertex in the graph for this group */
	Vertex vertex

	/**
	 * Constructor.
	 *
	 */
	public EncounterStrataGroup(CoreGraph coreGraph, String name, Vertex strataVertex) {
		assert coreGraph
		assert name
		assert strataVertex

		this.coreGraph = coreGraph
		this.name = name

		def cypher = '''
MATCH 
	(strata:EncounterStrata)
WHERE 
	ID(strata) = $strataVertexId
CREATE 
	(pg:EncounterGroup {name:$name})
CREATE 
	(strata)-[r:contains_group]->(pg)
RETURN 
	pg, strata
  	 	'''

		def res = coreGraph.graph.cypher(cypher, [strataVertexId:strataVertex.id(), name:name]).toList()
		assert res.size() == 1
		assert res.first().containsKey('pg')

		this.vertex = res.first().pg
	}
}





///////////////////////////////////////////////////////////////////////////////
// STRATA IMPLEMENTATIONS
///////////////////////////////////////////////////////////////////////////////


/**
 *
 */
class BmiEncounterStrata extends FloatRangeEncounterStrata {

	static AtomicInteger ctr = new AtomicInteger()

	/** 
	 * These are the packet identifiers that will be used to select the
	 * encounters from which we will get bmi.  It is assumed that only
	 * one packet per encounter will be provided.
	 */
	Collection<String> packetIds = []

	/**
	 * the bmi measurement we want to use for this strata.
	 *
	 */
	String getCriteraClassificationCypher() {
		String q = '''
MATCH 
	(outputGroup:EncounterGroup)
WHERE 
	ID(outputGroup) = $outputGroupId
WITH 
	outputGroup
MATCH 
	(enc:BiobankEncounter)-[:is_member_of]->(inputGroup:EncounterGroup)
	, (enc)-[:has_conclusionated_bmi]->(d:BodyMassIndex)
WHERE 
	ID(inputGroup) = $inputGroupId AND
	SUB_WHERE_CLAUSE
MERGE 
	(enc)-[:is_member_of]->(outputGroup)
RETURN 
	enc
''' 
		return q
	}

	/**
	 * the field that will be used to generate SUB_WHERE_CLAUSE, which does the
	 * work of stratifying the encounters.
	 *
	 */
	String cypherClassificationField = "d.value"


	public BmiEncounterStrata(
		CoreGraph coreGraph, 
		String name,
		double minValue = 10, 
		double maxValue = 40, 
		double rangeSize = 5
	) {
		super(coreGraph, name, minValue, maxValue, rangeSize)
		if (packetIds) this.packetIds = packetIds
	}
	
	public BmiEncounterStrata(CoreGraph coreGraph, String name, Collection<Range> criteria) {
		super(coreGraph, name, criteria)
	}
}



/**
 * A strata for the age of the patient at the date of the encounter.
 *
 */
class AgeEncounterStrata extends IntegerRangeEncounterStrata {

	String criteraClassificationCypher =  '''
MATCH 
	(outputGroup:EncounterGroup)
WHERE 
	ID(outputGroup) = $outputGroupId
WITH 
	outputGroup
MATCH
	(enc:BiobankEncounter)-[:is_member_of]->(inputGroup:EncounterGroup)
WHERE 
	ID(inputGroup) = $inputGroupId AND
	enc.ageOfPatient IS NOT NULL AND
	SUB_WHERE_CLAUSE
MERGE 
	(enc)-[:is_member_of]->(outputGroup)
RETURN 
	enc
	''' 

	String cypherClassificationField = "enc.ageOfPatient"

	public AgeEncounterStrata(
		CoreGraph coreGraph, 
		String name,
		int minValue = 18, 
		int maxValue = 85, 
		int rangeSize = 10
	) {
		super(coreGraph, name, minValue, maxValue, rangeSize)
	}
	
	public AgeEncounterStrata(CoreGraph coreGraph, String name, Collection<Range> criteria) {
		super(coreGraph, name, criteria)
	}
}






/**
 *
 *
 */
abstract class FloatRangeEncounterStrata extends EncounterStrata {

	/** Constructor */
	public FloatRangeEncounterStrata (CoreGraph coreGraph, String name, double minValue = 18, double maxValue = 85, double rangeSize = 10) {
		super(coreGraph, name)
		def criteria = []

		// (-infinity .. minValue)
		criteria.add(Range.lessThan(minValue))

		// [minValue..a), [a..b), ... , [z..maxValue)
		def bMin = minValue
		for (def bMax = (minValue + rangeSize); bMax <= maxValue; bMax += rangeSize) {
			if (bMax > maxValue) bMax = maxValue

			// "$bMin <= x < $bMax"
			criteria.add(Range.closedOpen(bMin,bMax))
			bMin = bMax
		}

		if (bMin != maxValue) {
			// "$bMin <= x < $maxValue"
			criteria.add(Range.closedOpen(bMin,maxValue))
		}

		// [maxValue .. infinity)
		criteria.add(Range.atLeast(maxValue))

		generateEncounterGroups(criteria)
	}

	/** Constructor */
	public FloatRangeEncounterStrata (CoreGraph coreGraph, String name, Collection<Range> criteria) {
		super(coreGraph, name)

		// error checking here; criteria should not overlap

		generateEncounterGroups(criteria)

	}

	/**
	* get the group the the value falls in
	*/
	public Vertex getGroupVertex(Double value) {
		def matchingGroup = unspecifiedEncounterGroup.vertex
		strataGroups.each {psGroup, criteria ->
			if (criteria.contains(value)) matchingGroup = psGroup.vertex
		}
		return matchingGroup
	}

	/**
	* get the group the the value falls in
	*/
	public Vertex getGroupVertex(Range value) {
		def matchingGroup = unspecifiedEncounterGroup.vertex
		strataGroups.each {psGroup, criteria ->
			if (criteria.encloses(value)) matchingGroup = psGroup.vertex
		}
		return matchingGroup
	}

	/**
	* get the group the the value falls in
	*/
	public Vertex getGroupVertexByRangeString(String rangeString) {
		def matchingGroup = unspecifiedEncounterGroup.vertex
		strataGroups.each {psGroup, criteria ->
			if (criteria.toString() == rangeString) matchingGroup = psGroup.vertex
		}
		return matchingGroup
	}

	private generateEncounterGroups(Collection<Range> criteria) {
		criteria.each { range ->
			def pg = new EncounterStrataGroup(coreGraph, range.toString(), vertex)
			strataGroups[pg] = range
		}
	}

	////////////
	/*
	* Generate the where clause of a cypher query based on the given range
	* i.e. : "(toFloat(d.EMR_CURRENT_AGE) >= 20) AND (toFloat(d.EMR_CURRENT_AGE) < 40) "
	*/
	protected String generateCriteriaWhereClause(def criteria) {
		assert criteria instanceof Range
		assert cypherClassificationField
		assert criteria 

		def whereClauses = []

		if (criteria.hasLowerBound()) {
			def clause = "(toFloat($cypherClassificationField))"
			if(criteria.lowerBoundType() == BoundType.CLOSED) clause += " >= " + String.valueOf(criteria.lowerEndpoint())
			else clause += " > " + String.valueOf(criteria.lowerEndpoint())
			whereClauses << clause
		}
		if (criteria.hasUpperBound()) {
			def clause = "(toFloat($cypherClassificationField))"
			if(criteria.upperBoundType() == BoundType.CLOSED) clause += " <= " + String.valueOf(criteria.upperEndpoint())
			else clause += " < " + String.valueOf(criteria.upperEndpoint())
			whereClauses << clause
		}

		return whereClauses.join(" AND ")
	}
}



/**
 *
 *
 */
abstract class IntegerRangeEncounterStrata extends EncounterStrata {

	/** 
	 * Constructor 
	 */
	public IntegerRangeEncounterStrata (
		CoreGraph coreGraph, 
		String name, 
		int minValue = 18, 
		int maxValue = 85, 
		int rangeSize = 10
	) {
		super(coreGraph, name)
		def criteria = []

		// (-infinity .. minValue)
		criteria.add(Range.lessThan(minValue))

		// [minValue..a), [a..b), ... , [z..maxValue)
		def bMin = minValue
		for (def bMax = (minValue + rangeSize); bMax <= maxValue; bMax += rangeSize) {
			if (bMax > maxValue) bMax = maxValue

			// "$bMin <= x < $bMax"
			criteria.add(Range.closedOpen(bMin,bMax))
			bMin = bMax
		}

		if (bMin != maxValue) {
			// "$bMin <= x < $maxValue"
			criteria.add(Range.closedOpen(bMin,maxValue))
		}

		// [maxValue .. infinity)
		criteria.add(Range.atLeast(maxValue))

		generateEncounterGroups(criteria)
	}


	/** 
	 * Constructor 
	 */
	public IntegerRangeEncounterStrata (CoreGraph coreGraph, String name, Collection<Range> criteria) {
		super(coreGraph, name)

		// error checking here; criteria should not overlap

		generateEncounterGroups(criteria)

	}


	/**
	 * get the group the the value falls in
	 */
	public Vertex getGroupVertex(Integer value) {
		def matchingGroup = unspecifiedEncounterGroup.vertex
		strataGroups.each {psGroup, criteria ->
			if (criteria.contains(value)) matchingGroup = psGroup.vertex
		}
		return matchingGroup
	}


	/**
	 * get the group the the value falls in
	 */
	public Vertex getGroupVertex(Range value) {
		def matchingGroup = unspecifiedEncounterGroup.vertex
		strataGroups.each {psGroup, criteria ->
			if (criteria.encloses(value)) matchingGroup = psGroup.vertex
		}
		return matchingGroup
	}


	/**
	 * get the group the the value falls in
	 */
	public Vertex getGroupVertexByRangeString(String rangeString) {
		def matchingGroup = unspecifiedEncounterGroup.vertex
		strataGroups.each {psGroup, criteria ->
			if (criteria.toString() == rangeString) matchingGroup = psGroup.vertex
		}
		return matchingGroup
	}

	private generateEncounterGroups(Collection<Range> criteria) {
		criteria.each { range ->
			def pg = new EncounterStrataGroup(coreGraph, range.toString(), vertex)
			strataGroups[pg] = range
		}
	}


	/**
	 * Generate the where clause of a cypher query based on the given range
	 * i.e. : "(toInt(d.EMR_CURRENT_AGE) >= 20) AND (toInt(d.EMR_CURRENT_AGE) < 40) "
	 */
	protected String generateCriteriaWhereClause(def criteria) {
		assert criteria instanceof Range
		assert cypherClassificationField
		assert criteria 

		def whereClauses = []

		if (criteria.hasLowerBound()) {
			def clause = "(toInt($cypherClassificationField))"
			if(criteria.lowerBoundType() == BoundType.CLOSED) clause += " >= " + String.valueOf(criteria.lowerEndpoint())
			else clause += " > " + String.valueOf(criteria.lowerEndpoint())
			whereClauses << clause
		}
		if (criteria.hasUpperBound()) {
			def clause = "(toInt($cypherClassificationField))"
			if(criteria.upperBoundType() == BoundType.CLOSED) clause += " <= " + String.valueOf(criteria.upperEndpoint())
			else clause += " < " + String.valueOf(criteria.upperEndpoint())
			whereClauses << clause
		}

		return whereClauses.join(" AND ")
	}
}



