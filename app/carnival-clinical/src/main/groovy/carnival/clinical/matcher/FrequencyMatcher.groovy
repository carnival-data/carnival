package carnival.clinical.matcher


import static org.math.array.IntegerArray.*
import org.apache.commons.math3.util.CombinatoricsUtils
import java.util.Random

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import au.com.bytecode.opencsv.*

import carnival.core.*
import carnival.util.Defaults
import carnival.util.*


enum RowSelectionStrategy {
	RANDOM_CHOICE,
	COMBINATIONS,
	ENRICH_PREVIOUS,
	DYNAMIC
}

/**
* For a given case population and potential control population and a set of features to match on,
* attempt to generate a control population that for each feature has the same distribution as the case population.
*
* To use:
* 	def matcher = new FrequencyMatcher(caseFeature, controlFeature, MappedDataTableStrata)
* 	def result = matcher.frequencyMatchCompoundFeatures(numRequestedControls)
*
*/
class FrequencyMatcher {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////
	static Logger elog = LoggerFactory.getLogger('db-entity-report')
	static Logger log = LoggerFactory.getLogger(FrequencyMatcher)

	// -- input
	String resultFeatureName = "fMatchControlSelection"
	def resultFeatureIdFieldName

	int numRequestedControls  // number of requested controls

	Collection<MappedDataTableStratum> featureStrata // specification of the features to frequency match on

	MappedDataInterface caseData // the data that represents the case population
	MappedDataInterface controlData // the data that represents the control population

	// -- generated
	int[][] controlPopulationFeatureMatrix  // dimentions of 'controlPopulationNumRows' by 'numFeatures'
	Map controlPopulationRowKeyLookup //  [key, key, key]
	int numFeatures // number of features defined by featureStrata

	// -- recursive algorithm parameters
	def featureIndexRanking
	def countsPerFeature
	int globalIteration
	def random

	def controlSelectionFeature


	// -- recursive algorithm 
	def GENERAL_STRATEGY = RowSelectionStrategy.DYNAMIC

	def MAX_ITERATIONS = 7000 // quit if MAX_ITERATIONS reached and no solution has been found
	def ITERATION_PER_RUN = 100
	def MAX_RANDOM_ITERATIONS = 100// max number of random choices allowed

	// after SHIFT_MOVE_ITERATION iterations, shift the row selection index by SHIFT_DISTANCE
	def SHIFT_MOVE_ITERATION = 2
	def SHIFT_DISTANCE = 15
	def MAX_SHIFTS = 50

	// if USE_RANDOM, randomize the selected rows
	def RANDOM_START = false
	long RANDOM_SEED = 1.2

	// how often to println text
	def VERBOSE_ITERATION = 1


	/**
	* Attempt to frequency match the cohort represented in controlFeature and to the cohort represented
	* by caseFeature on the feature keys linked in featureKeysToMatch
	*
	* bucket types: all values, collection of non-overlapping ranges, sets of non-overlapping discrete values
	*
	* @return 
	* 	message (String)
	* 	exactFrequencyMatch (Boolean)
	*	matchedControlFeature (CompundFeature)
	*/
	public FrequencyMatcher(MappedDataInterface caseFeature, MappedDataInterface controlFeature, Collection<MappedDataTableStratum> featureStrata, String name=null) {
		assert caseFeature
		assert controlFeature
		assert featureStrata
		assert caseFeature.idFieldName == controlFeature.idFieldName

		this.caseData = caseFeature
		this.controlData = controlFeature
		this.featureStrata = featureStrata

		if(name) this.resultFeatureName = resultFeatureName
		this.resultFeatureIdFieldName = caseFeature.idFieldName

		initilizeFeatures()
		initilizeControlPopulationFeatureMatrix()
	}

	/**
	* Attempt to generate a control population with numRequestedControls elements
	*/
	public Map frequencyMatchCompoundFeatures(int numRequestedControls, maxIterations = null) {
		this.numRequestedControls = numRequestedControls
		if (maxIterations) this.MAX_ITERATIONS = maxIterations

		// generate possible frequency count vectors for the desired control population
		def fPercent = generateFrequencyPercentageVector(caseData)

		// calculate the total possible hit combinations for each feature 
		def requiredFeatureCounts = generatePossibleFrequencyCountVectors(fPercent, numRequestedControls)
		if (!requiredFeatureCounts) {
			return [message:"no solutions!"]
		}
		def requiredFeatureCount = requiredFeatureCounts[0]

		// try to get a solution
		def res = doFeatureMatching(requiredFeatureCount)

		if (res.success) {
			log.trace "ids: $res.rowIds"
			def keys = []
			res.rowIds.each { i -> keys.add(controlPopulationRowKeyLookup[i])}
			res.keys = keys
			return res
		}

		return res
	}

	// for QC
	public double[] getCaseFrequencyPercentVector() {
		return generateFrequencyPercentageVector(caseData)
	}

	public double[] getControlFrequencyPercentVector() {
		return generateFrequencyPercentageVector(controlData)
	}


	public getFeatureLabels() {
		return MappedDataTableStratum.getLabelsForBucketList(featureStrata)
	}

	////////////////////////
	// Initilization
	////////////////////////

	private initilizeFeatures() {
		// set master indicies for MappedDataTableStratums to get feature indicies
		MappedDataTableStratum.updateBucketIndiciesForMappedDataTableStratumList(featureStrata)
		this.numFeatures = MappedDataTableStratum.totalFeaturesForBucketList(featureStrata)
	}

	private initilizeControlPopulationFeatureMatrix() {
		this.controlPopulationFeatureMatrix = generateFeatureMatrix(controlData)
		this.controlPopulationRowKeyLookup = generateIndexKeyMap(controlData)
	}

	////////////////////////
	// Utility
	////////////////////////
	/**
	* Return a list of the size of the features defined by featureStrata.
	* Element i of the list represents the percentage of keys in the compound feature that have a value in the feature
	* 	represeneted by bucket i.
	*/
	private double[] generateFrequencyPercentageVector(MappedDataInterface feature) {
		def numRows = feature.data.size()
		def featureMatrix = generateFeatureMatrix(feature)
		if (!featureMatrix) return []

		def colSumVector = sum(featureMatrix)

		double[] percentageVector = new double[numFeatures]

		colSumVector.eachWithIndex { colSum, i ->
			percentageVector[i] = ((colSum as double) / numRows as double)
		}

		return percentageVector
	}

	/**
	* Generate a possible solution, as close to the percentages as possible.
	*
	* TODO: generate multiple possible solutions
	*
	*/
	private int[][] generatePossibleFrequencyCountVectors(double[] pVec, def inTotalCount) {
		def count = fill(1, numFeatures, 0)
		def row = 0

		double totalCount = inTotalCount

		// get counts for each feature, ensuring that for a particular bucket, the total count == totalCount
		featureStrata.each { spec ->
			def indexes = spec.getMasterIndicies()
			def numToDistribute = totalCount

			indexes.each {i ->
				def val = Math.floor(pVec[i] * totalCount)

				count[row][i] = val
				numToDistribute = numToDistribute - val
			}


			indexes.sort{ a, b -> -1.0*pVec[a] <=> -1.0*pVec[b] }

			indexes.each { i ->
				if (numToDistribute <= 0 ) return
				count[row][i] = count[row][i]+1
				numToDistribute--
			}
		}

		return count
	}


	/**
	* generate a numRows by numFeatures matrix such that:
	* 	for position (i,j): 1 if person i has feature j, otherwise 0
	*/
	private int[][] generateFeatureMatrix(MappedDataInterface data) {
		def rows = data.data.size()
		def featureMatrix = fill(rows, numFeatures, 0)

		data.data.eachWithIndex { rowKey, dataMap, i ->
			featureStrata.each { featureGenerator ->
				def dataMapKey = featureGenerator.keyName.toUpperCase()
				assert dataMap.containsKey(dataMapKey)
				def j = featureGenerator.getMasterBucketIndex(dataMap.get(dataMapKey))
				featureMatrix[i][j] = 1
			}
		}
		return featureMatrix
	}

	private Map generateIndexKeyMap(MappedDataInterface data) {
		def lookup = [:]
		data.data.eachWithIndex { rowKey, dataMap, i ->
			lookup[i] = rowKey
		}
		return lookup
	}

	/**
	* For each feature, calculates the total combinations of 'numRequestedControls' that meet the requirement.
	* Calculates -1 if the requirements for the feature are unable to be met.
	* 
	*
	*  There are 'populationNumRows' individuals the population, from which we want to 
	*  choose 'numRequestedControls'. Let:
	*		'featureIndex' be the index of a particular feature F
	*		'requiredCount' be the number of individuals that must have feature 'featureIndex'
	*		'popCount' be the number of individuals in the population that have feature 'featureIndex'
	*  			
	*	Then the combinations for feature F are:
	*		choose(popCount, requiredCount) * choose((popSize-popCount), (numRequestedControls - requiredCount))
	*
	* 
	* populationFeatureMatrix - an n x m matrix, with n individuals and m features.
	*	element (i, j) = 1 if individual i has feature j.
	*
	* populationNumRows - number of rows in populationFeatureMatrix
	*
	* requiredCountVector - a vector of size j, that for each feature has the total
	*	number of seleted individuals that we want to have the feature. 
	* 
	* (class field) numRequestedControls - the number of individuals we want to choose
	* 	 from the feature matrix 
	*
	* @return - a vector of size j that has the number of combinatations of 
	*	controls that meet the requirements for the given feature
	*
	*/
	private int[] calculatePossibleCombinationsPerFeature(int[][] populationFeatureMatrix, int[] requiredCountVector) {
		
		def populationNumRows = populationFeatureMatrix.size()
		def popCountVector = sum(populationFeatureMatrix)
		int[] combinationVector = new int[numFeatures]

		for (int f = 0; f < numFeatures; f++) {
			// there are not enough controls to meet the requirement
			if ((requiredCountVector[f] > popCountVector[f]) || ((numRequestedControls - requiredCountVector[f]) > (populationNumRows - popCountVector[f]))) {
				combinationVector[f] = -1
			}

			else {
				// individuals that have the feature
				combinationVector[f] = CombinatoricsUtils.binomialCoefficient(popCountVector[f], requiredCountVector[f])

				// individuals that do not have the feature
				combinationVector[f] *= CombinatoricsUtils.binomialCoefficient((populationNumRows - popCountVector[f]), (numRequestedControls - requiredCountVector[f]))
			}
		}

		return combinationVector
	}

	/**
	* For each feature, calculates the total combinations of controls that have a '1' for the feature that meet the 
	* requirement.  
	* Calculates -1 if the requirements for the feature are unable to be met.
	*
	*  There are 'populationNumRows' individuals the population, from which we want to 
	*  choose 'numRequestedControls'. Let:
	*		'featureIndex' be the index of a particular feature F
	*		'requiredCount' be the number of individuals that must have feature 'featureIndex'
	*		'popCount' be the number of individuals in the population that have feature 'featureIndex'
	*  			
	*	Then the hit combinations for feature F are:
	*		choose(popCount, requiredCount)
	*
	* 
	* populationFeatureMatrix - an n x m matrix, with n individuals and m features.
	*	element (i, j) = 1 if individual i has feature j.
	*
	* populationNumRows - number of rows in populationFeatureMatrix
	*
	* requiredCountVector - a vector of size j, that for each feature has the total
	*	number of seleted individuals that we want to have the feature. 
	* 
	* (class field) numRequestedControls - the number of individuals we want to choose
	* 	 from the feature matrix 
	*
	* @return - a vector of size j that has the number of combinatations of 
	*	controls that meet the requirements for the given feature
	*
	*/
	private int[] calculatePossibleHitCombinationsPerFeature(int[][] populationFeatureMatrix, int[] requiredCountVector) {
		
		def populationNumRows = populationFeatureMatrix.size()
		def popCountVector = sum(populationFeatureMatrix)
		int[] combinationVector = new int[numFeatures]

		for (int f = 0; f < numFeatures; f++) {
			// there are not enough controls to meet the requirement
			if ((requiredCountVector[f] > popCountVector[f]) || ((numRequestedControls - requiredCountVector[f]) > (populationNumRows - popCountVector[f]))) {
				combinationVector[f] = -1
			}
			else if (requiredCountVector[f] == 0) combinationVector[f] = 0

			else {
				// individuals that have the feature
				combinationVector[f] = CombinatoricsUtils.binomialCoefficient(popCountVector[f], requiredCountVector[f])
			}
		}

		return combinationVector
	}

	private int[] calculateRelativeHitRankingPerFeature(int[][] populationFeatureMatrix, int[] requiredCountVector) {
		
		def populationNumRows = populationFeatureMatrix.size()
		def popCountVector = sum(populationFeatureMatrix)
		int[] combinationVector = new int[numFeatures]

		for (int f = 0; f < numFeatures; f++) {
			// there are not enough controls to meet the requirement
			if ((requiredCountVector[f] > popCountVector[f]) || ((numRequestedControls - requiredCountVector[f]) > (populationNumRows - popCountVector[f]))) {
				combinationVector[f] = -1
			}
			else if (requiredCountVector[f] == 0) combinationVector[f] = 0

			else {
				// a relative ranking for individuals that have the feature
				combinationVector[f] = popCountVector[f] - requiredCountVector[f]
			}
		}

		return combinationVector
	}

	private int[] calculateBucketRankingPerFeature(int[][] populationFeatureMatrix, int[] requiredCountVector) {
		
		def populationNumRows = populationFeatureMatrix.size()
		def popCountVector = sum(populationFeatureMatrix)
		int[] combinationVector = new int[numFeatures]

		for (int f = 0; f < numFeatures; f++) {
			// there are not enough controls to meet the requirement
			if ((requiredCountVector[f] > popCountVector[f]) || ((numRequestedControls - requiredCountVector[f]) > (populationNumRows - popCountVector[f]))) {
				combinationVector[f] = -1
			}
			else if (requiredCountVector[f] == 0) combinationVector[f] = 0

			else {
				// just in order
				combinationVector[f] = f
			}
		}

		return combinationVector
	}

	private generateControlSelectionFeature(def selectedRows, def potentialRows) {
		controlSelectionFeature = new MappedDataTable(name:resultFeatureName,
			idFieldName: resultFeatureIdFieldName)

		(0..<controlPopulationRowKeyLookup.size()).each { i ->
			def val = [:]
			if (i in selectedRows) val["selection"] = "SELECTED"
			//else if (i in potentialRows) val["selection"] = "POTENTIAL"
			//else val["selection"] = "EXCLUDED"

			controlSelectionFeature.add(controlPopulationRowKeyLookup[i], val)
		}
	}

	////////////////////////
	// Generate Solutions
	////////////////////////

	/**
	* Initilize the soution space and call the recursive choosing algorithm
	*/
	private Map doFeatureMatching(int[] countsPerFeature) {
		// set the initial potential solution space:
		this.globalIteration = 0
		this.countsPerFeature = countsPerFeature

		this.featureIndexRanking = []
		this.controlSelectionFeature = null

		if (RANDOM_SEED) this.random = new Random(RANDOM_SEED)
		else this.random = Defaults.random

		//set this.featureIndexList; order features in the control population by least number of combinations to the most
		def unsortedFeatures = []

		/*calculatePossibleHitCombinationsPerFeature(controlPopulationFeatureMatrix, countsPerFeature).eachWithIndex {rankVal, idx -> 
			unsortedFeatures.add(["index":idx, "rank":rankVal])
		}*/
		
		/*
		calculateBucketRankingPerFeature(controlPopulationFeatureMatrix, countsPerFeature).eachWithIndex {rankVal, idx -> 
			unsortedFeatures.add(["index":idx, "rank":rankVal])
		}*/
		
		calculateRelativeHitRankingPerFeature(controlPopulationFeatureMatrix, countsPerFeature).eachWithIndex {rankVal, idx -> 
			unsortedFeatures.add(["index":idx, "rank":rankVal])
		}

		this.featureIndexRanking = unsortedFeatures.sort{ it.rank }

		writeStatReport()

		if (featureIndexRanking[0].rank == -1) {
			return [success:false, message:"no exact solution exists"]
		}


		//	selectedRows is empty
		//  potentialRows is all rows of the control population
		//	currentFeatureIdx = 0
		def selectedRows = []
		def potentialRows = (0..controlPopulationFeatureMatrix.size() - 1)
		def currentFeatureLookupIdx = 0

		log.trace "ranking:$featureIndexRanking"
		log.trace "counts: $countsPerFeature"
		log.trace "starting selectedRows:$selectedRows"
		elog.trace "starting doFeatureMatching()"
		elog.trace "ranking:$featureIndexRanking"
		elog.trace "counts: $countsPerFeature"

		def res = recrusivelyChooseRows(selectedRows, potentialRows, currentFeatureLookupIdx)
		if (res.success) generateControlSelectionFeature(res.rowIds, [])
		return res
	}

	private writeStatReport() {
		if (caseData.data.size() == 0 || controlData.data.size() == 0) return

		def stats = []
		def header = ["FeatureIndex", "FeatureName", "BucketName", "FeatureRankValue", "PercCase", "PercControl", "NCases" , "NControls", "NControlsNeeded"]

		// data
		def casePopulationFeatureMatrix = generateFeatureMatrix(caseData)

		def labels = MappedDataTableStratum.getLabelsForBucketList(featureStrata)
		def bucketNames = MappedDataTableStratum.getBucketNamesForBucketList(featureStrata)
		def caseCounts = sum(casePopulationFeatureMatrix)
		def controlCounts = sum(controlPopulationFeatureMatrix)
		def casePercents = generateFrequencyPercentageVector(caseData)
		def controlPercents = generateFrequencyPercentageVector(controlData)

		(0..labels.size()-1).each {i ->
			def featureStat = ["FeatureIndex":i,
				"FeatureName" : labels[i],
				"BucketName" : bucketNames[i],
				"FeatureRankValue" : featureIndexRanking.find {it.index == i}.rank,
				"PercCase" : casePercents?casePercents[i]: "n/a",
				"PercControl" : controlPercents[i],
				"NCases" : caseCounts[i],
				"NControlsNeeded" : countsPerFeature[i],
				"NControls" : controlCounts[i]
			] 
			stats.add(featureStat)
		}

		writeStatsToFile(stats, header)
	}


	private Map checkFutureSelectionRequirements(def selectedRows, def potentialRows, def currentFeatureLookupIdx) {
		def totalSelectedVector = getSumVectorOfControlPopMatrixSelectedRows(selectedRows)
		def totalPotentialVector = getSumVectorOfControlPopMatrixSelectedRows(potentialRows)

		def potentialNeededVector = vectorSubtract(countsPerFeature, totalSelectedVector)
		def choices = vectorSubtract(totalPotentialVector, potentialNeededVector)

		/*
		// feature selection
		for (feature in featureStrata) {
			def sel = 0
			def pot = 0
			feature.getMasterIndicies().each { i ->
				sel += totalSelectedVector[i]
				pot += totalPotentialVector[i]
			}
			if (sel > numRequestedControls) return [success:false, minChoices:min, message:"too many n selected for bucket $feature.keyName: $sel of $numRequestedControls"]
			if (pot < numRequestedControls - sel) return [success:false, minChoices:min, message:"not enough n for bucket $feature.keyName: $pot of ${numRequestedControls - sel}"]
		}*/

		//overfilled feature requirement
		def overfilled = potentialNeededVector.min()
		if (overfilled < 0) {
			return [success:false, enrichmentVal:overfilled, featureToEnrich:potentialNeededVector.indexOf(overfilled), message:"too many r for feature idx ${potentialNeededVector.indexOf(overfilled)}, #overfill: $overfilled"]
			//return [success:false, message:"too many r for feature idx ${potentialNeededVector.indexOf(overfilled)}, #overfill: $overfilled"]
		}

		// too few potential rows to fufill a particular requirement
		def minChoice = choices.min()
		if (minChoice < 0) {
			return [success:false, enrichmentVal:(minChoice*-1), featureToEnrich:choices.indexOf(minChoice), message:"not enough r for feature idx ${choices.indexOf(minChoice)} #choices: $minChoice"]
		}
		else return [success:true]


	}

	/**
	*	- while there are unexplored combinations of selectedRows and additional rows from potentialRows that
	*		meet the total requirement:
	*		- this.globalIteration++
	*		- choose additional rows from potentialRows that fufill the feature total requirement
	*		- add those rows to selectedRows to get newSelectedRows
	*		- remove all rows that have a 1 for the currentFeature to get newPotentialFeatures
	*		- call recrusivelyChooseRows(newSelectedRows, newPotentialFeatures, currentFeatureLookupIdx++, globalIteration)
	*		- if a solution is returned, return the solution
	*
	*	- return 'no solutions found'
	*/
	private Map recrusivelyChooseRows(def selectedRows, def potentialRows, def currentFeatureLookupIdx, def inputSeedRows = []) {
		//log.trace "recrusivelyChooseRows($selectedRows, $potentialRows, $currentFeatureLookupIdx)"
		//log.trace "recrusivelyChooseRows(${selectedRows?.size()}, ${potentialRows?.size()}, $currentFeatureLookupIdx)"

		// we have met all the feature requirements, find any other rows needed 
		if (currentFeatureLookupIdx >= featureIndexRanking.size()) {
			return chooseLastRows(selectedRows, potentialRows)
		}

		def res
		res = checkFutureSelectionRequirements(selectedRows, potentialRows, currentFeatureLookupIdx)
		if(!res.success) return res
		res = null

		def featureIdx = featureIndexRanking[currentFeatureLookupIdx].index

		// gets stats for current feature
		def sumSelectedRows = getControlPopMatrixSum(selectedRows, featureIdx)
		def totalSum = countsPerFeature[featureIdx]
		def numAdditionalRows = totalSum - sumSelectedRows
		def idxRowsToSelect = getControlPopMatrixNonZeroValues(potentialRows, featureIdx) // list of the ids of rows available to select from for this feature
		if (RANDOM_START) Collections.shuffle(idxRowsToSelect, random)

		// check to see if the current state of the feature with respect to selectedRows is invalid
		if (sumSelectedRows > totalSum) {
			return [success:false, message:"selectedRows > totalSum"]
		}
		if (idxRowsToSelect.size() < numAdditionalRows) {
			return [success:false, message:"not enough rows to fufill requirement"]
		}

		// iterate through all possible combinations of potentialRows that would fufill this requirement
		def newPotentialRows = []
		newPotentialRows.addAll(potentialRows) // copy potentialRows
		newPotentialRows.removeAll(idxRowsToSelect)

		def newCurrentFeatureLookupIdx = currentFeatureLookupIdx + 1
		
		def frameShiftValue = 0
		def localIteration = 0
		def maxShifts = (idxRowsToSelect.size()/SHIFT_DISTANCE) + 1
		def numberShifts = 0
		def continueSearching = true
		def curSelectedRows = []
		def randomIteration = 0

		def strategy = RowSelectionStrategy.COMBINATIONS  //RANDOM_CHOICE // COMBINATIONS // COMBINATIONS ENRICH_PREVIOUS

		def combinations = CombinatoricsUtils.combinationsIterator(idxRowsToSelect.size(), numAdditionalRows)

		def enrichFeaturesToLock = [] as Set

		// go through row selection
		while(localIteration < ITERATION_PER_RUN && randomIteration < MAX_RANDOM_ITERATIONS && continueSearching) {
			if (MAX_ITERATIONS && (globalIteration > MAX_ITERATIONS)) {
				if (!controlSelectionFeature) {
					if (curSelectedRows) generateControlSelectionFeature(selectedRows + curSelectedRows, newPotentialRows)
					else generateControlSelectionFeature(selectedRows, potentialRows) 
				}
				return [success:false, message:"MAX_ITERATIONS ($MAX_ITERATIONS) reached"]
			}
			globalIteration += 1 // ++ doesn't work in tests for some reason
			localIteration += 1


			// -- set seed for next iteration
			def nextSeedSelection = res?.containsKey("seedRows") ? res.seedRows : inputSeedRows
			//if (res?.addCurrentSelectionToSeed) nextSeedSelection.addAll(curSelectedRows)

			// ----choose strategy
			// hard-coded to always use a specific strategy
			if ((GENERAL_STRATEGY == RowSelectionStrategy.COMBINATIONS) || (GENERAL_STRATEGY == RowSelectionStrategy.RANDOM_CHOICE)) {
				strategy = GENERAL_STRATEGY
			}
			/**
			* if very few choices, test all with COMBINATIONS strategy
			* if the previous result indicated that the row selection left a future feature with too few hits
			*	enrich the selection for that feature with the ENRICH_PREVIOUS strategy
			* otherwise, just choose random with the RANDOM_CHOICE strategy
			*/
			else if (GENERAL_STRATEGY == RowSelectionStrategy.DYNAMIC) {
				if (idxRowsToSelect.size() == numAdditionalRows) 
					{ strategy = RowSelectionStrategy.COMBINATIONS }
				// if enrichment is an option, enrich
				else if (res?.containsKey("featureToEnrich") && res.enrichmentVal != 0) {
					if (strategy != RowSelectionStrategy.ENRICH_PREVIOUS) enrichFeaturesToLock.clear()
					strategy = RowSelectionStrategy.ENRICH_PREVIOUS
				}
				else {
					randomIteration += 1
					strategy = RowSelectionStrategy.RANDOM_CHOICE
				}
			}
			else {
				return "Warning: unhandled GENERAL_STRATEGY: $GENERAL_STRATEGY"
			}

			// ----choose rows
			if (numAdditionalRows == 0) {
				continueSearching = false 
			}
			else if (strategy == RowSelectionStrategy.RANDOM_CHOICE) {
				curSelectedRows = selectRandom(idxRowsToSelect, numAdditionalRows, nextSeedSelection)
			}
			else if (strategy == RowSelectionStrategy.COMBINATIONS) {
				if(!combinations.hasNext()) {
					continueSearching = false
					return [success:false, message:"exhausted all combinations"]
				}
				curSelectedRows.clear()
				def idxes = combinations.next()
				curSelectedRows = idxes.collect { idxRowsToSelect[it] }
			}
			else if (strategy == RowSelectionStrategy.ENRICH_PREVIOUS) {
				def localRes = enrichSelectionForFeature(curSelectedRows, idxRowsToSelect, res.featureToEnrich, (res.enrichmentVal), enrichFeaturesToLock)
				enrichFeaturesToLock << res.featureToEnrich

				if (localRes.success) curSelectedRows = localRes.rowSelection
				else if (currentFeatureLookupIdx == 0) { 
					curSelectedRows = selectRandom(idxRowsToSelect, numAdditionalRows)
					nextSeedSelection = []
				}
				else {
					def selEnrichmentVal = getControlPopMatrixSum(curSelectedRows, res.featureToEnrich)
					def newEnrichmentVal = res.enrichmentVal>0 ? res.enrichmentVal - selEnrichmentVal : res.enrichmentVal + selEnrichmentVal
					//return [success:false, featureToEnrich:res.featureToEnrich, enrichmentVal:res.enrichmentVal - selEnrichmentVal, seedRows:curSelectedRows, addCurrentSelectionToSeed:true]
					return [success:false, featureToEnrich:res.featureToEnrich, enrichmentVal:newEnrichmentVal, message:"feature $currentFeatureLookupIdx unable to enrich ${res.featureToEnrich} for val ${res.enrichmentVal}"]
				}
			}
			else {return [success:false, message:"Warning, unhandles row selection strategy '$strategy'"]}

/*
			if (VERBOSE_ITERATION && ((localIteration-1%VERBOSE_ITERATION == 0) || (localIteration%VERBOSE_ITERATION == 0) || ((localIteration+1)%VERBOSE_ITERATION == 0) || ((localIteration+3)%VERBOSE_ITERATION == 0) || ((localIteration+4)%VERBOSE_ITERATION == 0))) {
				//println "\titeration: $globalIteration featureIdx: ${currentFeatureLookupIdx} selectedRowLookup: $selectedRowLookup"
				println "\t  fIdx: ${currentFeatureLookupIdx} Gitr: $globalIteration Litr: $localIteration strat:$strategy curSelectedRows: ${curSelectedRows.min()} - ${curSelectedRows.max()}"
				//println "\t fIdx: ${currentFeatureLookupIdx} Gitr: $globalIteration Litr: $localIteration strat:$strategy lock: $selectedRows pot: $potentialRows select: $curSelectedRows"
				if (res) println "\t\t  lastResult: $res"
			}
*/
			// check that the current selected rows are valid
			/*
			assert curSelectedRows?.size() == numAdditionalRows
			curSelectedRows.each { assert it in potentialRows }
			def originalSize = curSelectedRows.size()
			assert curSelectedRows.unique().size() == originalSize
			*/

			// call next level
			//println ''.padLeft((currentFeatureLookupIdx%10) * 2) + "$globalIteration - fIdx:$currentFeatureLookupIdx - Litr: $localIteration - recursing - strat:$strategy curSelectedRows: ${curSelectedRows.min()} - ${curSelectedRows.max()}"

			res = recrusivelyChooseRows(selectedRows + curSelectedRows, newPotentialRows, newCurrentFeatureLookupIdx, [])
			if (res.success) return res
		}

		if (MAX_ITERATIONS && (globalIteration > MAX_ITERATIONS)) return [success:false, message:"MAX_ITERATIONS ($MAX_ITERATIONS) reached"]

		return [success:false, message:"no selection found"]
	}

	/**
	* Given a set of rows that have been selected for currentFeature, randomly select different rows so that the total of
	* featureToEnrich is increased by enrichmentVal
	*/
	private Map enrichSelectionForFeature(def currentSelection, def potentialRows, def featureToEnrich, def enrichmentVal, def featuresToLock) {
		def potentialRowsToAdd = []
		def selectedRowsToRemove = []

		// are we adding or removing for featureToEnrich?
		def checkVal = (enrichmentVal > 1) ? 1 : 0


		potentialRows.each { i -> 
			if (!(i in currentSelection) && controlPopulationFeatureMatrix[i][featureToEnrich] == checkVal) potentialRowsToAdd << i
		}

		currentSelection.each {i ->
			if (controlPopulationFeatureMatrix[i][featureToEnrich] != checkVal) {
				def locked = false
				featuresToLock.each {locCol -> if (controlPopulationFeatureMatrix[i][locCol] == 1) locked = true} 
				if (!locked) selectedRowsToRemove << i
			}
		}
		/*
		println "enrich val: $enrichmentVal"
		println "enrich pRows.size: ${potentialRows.size()}"
		println "enrich selection.size: ${currentSelection.size()}"
		println "enrich potToAdd.size: ${potentialRowsToAdd.size()}"
		println "enrich potToRemove.size: ${selectedRowsToRemove.size()}"
		*/

		enrichmentVal = enrichmentVal.abs()

		// if the controlPopFeature matrix is set up correctly, this should never happen...
		if (potentialRowsToAdd.size() < enrichmentVal || selectedRowsToRemove.size() < enrichmentVal)
			return [success:false, message:"cannot enrich for feature $featureToEnrich for $enrichmentVal with locked features $featuresToLock"]

		Collections.shuffle(potentialRowsToAdd, random)
		Collections.shuffle(selectedRowsToRemove, random)


		//println "enrich pToAdd: $potentialRowsToAdd sToRem: $selectedRowsToRemove"
		//println "enrich before: $currentSelection"		
		(0..<enrichmentVal).each { i ->
			currentSelection.add(potentialRowsToAdd[i])
			currentSelection.removeElement(selectedRowsToRemove[i])
		}
		//println "enrich after: $currentSelection"

		return [success:true, rowSelection:currentSelection]
	}

	private selectRandom(def potentialRows, def totalToSelect, def seedSelection = []) {
		def selection = potentialRows.intersect(seedSelection)
		if(selection.size() > totalToSelect) selection = []

		def rndToSelect = totalToSelect - selection.size()

		potentialRows.removeAll{it in selection}

		def idx = []
		(0..<potentialRows.size()).each {idx << it}
		Collections.shuffle(idx, random)

		(0..<rndToSelect).each {i -> selection << potentialRows[idx[i]]}

		//println "selectRandom($potentialRows, $rndToSelect) -> $idx -> $selection"
		return selection
	}


	private Map chooseLastRows(def selectedRows, def potentialRows) {
		//println "ChooseLastRows($selectedRows, $potentialRows)"
		def numAdditionalRows = numRequestedControls - selectedRows.size()

		if(numAdditionalRows == 0) return [success:true, message:"solution found", rowIds:selectedRows]

		if (potentialRows.size() < numAdditionalRows) {
			return [success:false, message:"not enough rows to fufill requirement"]
		}

		(0..numAdditionalRows - 1).each { i ->
			selectedRows.add(potentialRows[i])
		}

		return [success:true, message:"solution found", rowIds:selectedRows]
	}

	//--------------

	/**
	*	@return return the sum of all the rows in the given col
	*/
	private Integer getControlPopMatrixSum(def rows, def col) {
		def sum = 0
		rows.each { row ->
			sum += controlPopulationFeatureMatrix[row][col]
		}

		return sum
	}

	/**
	*	@return return the indexes of all the rows in selectedRows and col that have a value > 0
	*/
	private Collection getControlPopMatrixNonZeroValues(def potentialRows, def col) {
		def indexes = []

		potentialRows.each { row ->
			if (controlPopulationFeatureMatrix[row][col] > 0) indexes.add(row)
		}

		return indexes
	}

	private Collection getSumVectorOfControlPopMatrixSelectedRows(def rows) {
		def sumVector = []

		(0..numFeatures-1).each{ col -> 
			def sum = 0
			rows.each { row ->
				if (controlPopulationFeatureMatrix[row][col] > 0) sum++
			}
			sumVector.add(sum)
		}
		return sumVector
	}

	private Collection vectorSubtract(def a, def b) {
		assert a.size() == b.size()
		def c = []
		a.eachWithIndex { aVal, i ->
			c.add(aVal - b[i])
		}

		return c
	}

	/**
	* @param data - list of maps
	* @param orderedKeys - keys of the map in the order they should be shown
	* @param prefix - prefix for the output file
	*/
    private void writeStatsToFile(Collection data, Collection orderedKeys, String prefix = "fMatchStats") {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("${Defaults.targetDirectory}/${prefix}-${name}.csv"));

            String[] line 
            List<String> lineList = []

			// header
            orderedKeys.each {key -> lineList << key}
            line = lineList.toArray()
			writer.writeNext(line)

            data.each { dMap ->
                lineList = []

				orderedKeys.each { k ->
                    def v = dMap.get(k)
                    lineList << (v) ? v : ""
                }

                line = lineList.toArray()
                writer.writeNext(line)
            }

            writer.close()
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}