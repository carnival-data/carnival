package carnival.core.matcher


import spock.lang.Specification
import spock.lang.Unroll
import org.apache.commons.math3.util.CombinatoricsUtils

import carnival.core.*
import carnival.util.KeyType

import carnival.util.*


/**
* gradle -Dtest.single=FrequencyMatcherSpec test
*/
class FrequencyMatcherSpec extends Specification {

    /*
    List<String> featureNames(int numFeatures) {
        List<String> names = []
        (1..numFeatures).each { i -> names << "f${i}" }
    }

    CompoundFeature createCaseFeature() {
        def fnames = featureNames(10)

        data["1"] = ["f1":1]

        int[][] multi = new int[][]{
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
        };        

        multi.eachWithIndex { row, rowi -> }
    }

    CompoundFeature createControlFeature() {

    }


    def "test test"() {
        when:
        def caseFeature = createCaseFeature()
        caseFeature.writeToFile()

        then:
        1 == 1
    }
    */


    def "frequencyMatchCompoundFeatures placeholder test"() {
    	when:
    	def caseFeature = new MappedDataTable(
            name:"testCase",
            idFieldName:'EMPI',
            idKeyType:KeyType.EMPI)
    	caseFeatureData.eachWithIndex { data, i ->
    		caseFeature.dataAdd(data)
    	}

    	def controlFeature = new MappedDataTable(name:"testControl",
            idFieldName:'EMPI',
            idKeyType:KeyType.EMPI)
    	controlFeatureData.eachWithIndex { data, i ->
    		controlFeature.dataAdd(data)
    	}
    	def matcher = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
    	def result = matcher.frequencyMatchCompoundFeatures(numRequestedControls)

    	then:
    	result.message.equals(expectedResult.message)

    	where:
    	caseFeatureData << [
 			[["empi":"case1", "sex":"m", "race":"black", "age":"18"],
 			["empi":"case2", "sex":"female", "race":"black", "age":"28"]]
 		]
    	controlFeatureData << [
 			[["empi":"control1", "sex":"m", "race":"black", "age":"18"],
 			["empi":"control2", "sex":"female", "race":"black", "age":"28"]]
 		]
    	featureStrata << [[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex")]]
    	numRequestedControls << [5]
    	expectedResult << [[message:"no exact solution exists"]]
    }

    def "initilization and generateFeatureMatrix test" () {
    	when:
    	def caseFeature = new MappedDataTable(name:"testCase",
            idFieldName:'EMPI',
            idKeyType:KeyType.EMPI)
    	caseFeatureData.eachWithIndex { data, i ->
    		caseFeature.add("case$i", data)
    	}

    	def controlFeature = new MappedDataTable(name:"testControl",
            idFieldName:'EMPI',
            idKeyType:KeyType.EMPI)
    	controlFeatureData.eachWithIndex { data, i ->
    		controlFeature.dataAdd(data)
    	}

    	def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)

    	then:
    	fMatch.numFeatures == expectedNumFeatures

    	(0..expectedControlPopulationNumRows-1).each {i ->
    		(0..expectedNumFeatures-1).each { j ->
    			//println "$i, $j: ${fMatch.controlPopulationFeatureMatrix[i][j]}"
    			if ([i,j] in expectedControlPopulationHitLocations) {
    				assert fMatch.controlPopulationFeatureMatrix[i][j] == 1
    				//println "hit?"
    			}
    			else
    				assert fMatch.controlPopulationFeatureMatrix[i][j] == 0
    		}
    	}
    	//println fMatch.controlPopulationFeatureMatrix

    	where:
    	caseFeatureData << [[:]]
    	controlFeatureData << [
 			[["empi":"control1", "sex":"m", "race":"black", "age":"18"],
 			["empi":"control2", "sex":"female", "race":"black", "age":"28"]]
 		]
    	featureStrata << [
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race"), 
    		MappedDataTableStratum.generateStandardAgeMappedDataTableStratum("age")]
    	]

    	expectedControlPopulationHitLocations << [
    		[[0,0],[0,3],[0,12],[1,1],[1,3],[1,22]]
    	]
    	expectedControlPopulationNumRows << [2]
    	expectedNumFeatures << [81]
    }
    
    def "generateFrequencyPercentageVector test" () {
    	when:
    	def caseFeature = new MappedDataTable(name:"testCase",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
    	caseFeatureData.eachWithIndex { data, i ->
    		caseFeature.add("case$i", data)
    	}

    	def controlFeature = new MappedDataTable(name:"testControl",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
    	controlFeatureData.eachWithIndex { data, i ->
    		controlFeature.dataAdd(data)
    	}

    	def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
    	def fPercent = fMatch.generateFrequencyPercentageVector(controlFeature)

    	then:
    	fMatch.numFeatures == expectedNumFeatures

    	fPercent.size() == expectedNumFeatures
    	def labels = MappedDataTableStratum.getLabelsForBucketList(featureStrata)

		(0..expectedNumFeatures-1).each { i ->
			if (i in expectedFPercent) {
				//println "$i: expected percent for '${labels[i]}': ${expectedFPercent[i]} \t- found: ${fPercent[i]}"
				assert fPercent[i].trunc(3) == expectedFPercent[i]
			}
			else {
				//println "$i: expected percent for '${labels[i]}': 0 \t- found: ${fPercent[i]}"
				assert fPercent[i] == 0
			}
		}
    	//println fMatch.controlPopulationFeatureMatrix

    	where:
    	caseFeatureData << [[:]]
    	controlFeatureData << [
 			[["empi":"control1", "sex":"m", "race":"black", "age":"18"],
 			["empi":"control2", "sex":"female", "race":"black", "age":"28"],
 			["empi":"control3", "sex":"female", "race":"white", "age":"29"]]
 		]
    	featureStrata << [
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race"), 
    		MappedDataTableStratum.generateStandardAgeMappedDataTableStratum("age")]
    	]

    	expectedControlPopulationHitLocations << [
    		[[0,0],[0,3],[0,12],[1,1],[1,3],[1,22],[2,1],[2,9],[2,23]]
    	]
    	expectedFPercent << [
    		[0:0.333, 1:0.666, 3:0.666, 9:0.333, 12:0.333, 22:0.333, 23:0.333]
    	]
    	expectedNumFeatures << [81]
    }


    def "generatePossibleFrequencyCountVectors test" () {
    	when:
    	def caseFeature = new MappedDataTable(name:"testCase",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
    	def controlFeature = new MappedDataTable(name:"testControl",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)

    	double[] dFPercent  = fPercent

    	def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
    	def fCounts = fMatch.generatePossibleFrequencyCountVectors(dFPercent, totalCount)

    	then:
    	fCounts == expectedFCounts

    	where:
    	featureStrata << [
    		[MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")]

    	]
    	fPercent << [
    		[0.0, 0.3, 0.3, 0.3, 0.1, 0.0, 0.0, 0.0],
    		[0.0, 0.3, 0.3, 0.3, 0.1, 0.0, 0.0, 0.0],
    		[0.0, 0.3, 0.3, 0.3, 0.1, 0.0, 0.0, 0.0],
    		[0.0, 0.3, 0.3, 0.3, 0.1, 0.0, 0.0, 0.0],
    		[0.0, 0.3, 0.3, 0.3, 0.1, 0.0, 0.0, 0.0],
    		[0.5, 0.5, 0.0, 0.0, 0.3, 0.3, 0.3, 0.1, 0.0, 0.0, 0.0],
    	]
    	totalCount << [4,5,6,7,10,7]

    	expectedFCounts << [
    		[[0, 2, 1, 1, 0, 0, 0, 0]],
    		[[0, 2, 2, 1, 0, 0, 0, 0]],
    		[[0, 2, 2, 2, 0, 0, 0, 0]],
    		[[0, 3, 2, 2, 0, 0, 0, 0]],
    		[[0, 3, 3, 3, 1, 0, 0, 0]],
    		[[4, 3, 0, 0, 3, 2, 2, 0, 0, 0, 0]],
    	]

    }

    def "calculatePossibleCombinationsPerFeature test" () {
    	when:
    	def caseFeature = new MappedDataTable(name:"testCase",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
    	def controlFeature = new MappedDataTable(name:"testControl",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)

    	int[] requiredCountVector = new int[requiredCountData.size()]
    	requiredCountData.eachWithIndex {val, i -> requiredCountVector[i] = val}

    	int[][] populationFeatureMatrix = new int[populationFeatureData.size()][populationFeatureData[0].size]
    	populationFeatureData.eachWithIndex {row, i ->
    		row.eachWithIndex {val, j ->
    			populationFeatureMatrix[i][j] = val
    		}
    	}

    	def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
    	fMatch.numRequestedControls = numControls
    	def comboVector = fMatch.calculatePossibleCombinationsPerFeature(populationFeatureMatrix, requiredCountVector) 

    	then:
    	comboVector.size() == expectedNumFeatures
    	fMatch.numFeatures == expectedNumFeatures

    	def labels = MappedDataTableStratum.getLabelsForBucketList(featureStrata)

    	expectedComboVector.eachWithIndex {expVal, i ->
    		assert comboVector[i] == expVal
    	}

    	where:
    	featureStrata << [
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")]
    	]
    	requiredCountData << [
    			[ 1, 0, 3, 1, 1, 2, 3, 0, 0, 0, 0]
    	]
    	populationFeatureData << [
    		[	[ 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 ]
        	]
    	]
    	numControls << [3]

    	// 5 choose 3 = 10
    	expectedComboVector << [
    		[-1, 10, 10, 6, 6, 3, -1, 10, 10, 10, 10]
    	]
    	expectedNumFeatures << [11]
    }

    def "calculatePossibleHitCombinationsPerFeature test" () {
    	when:
    	def caseFeature = new MappedDataTable(name:"testCase",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
    	def controlFeature = new MappedDataTable(name:"testControl",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)

    	int[] requiredCountVector = new int[requiredCountData.size()]
    	requiredCountData.eachWithIndex {val, i -> requiredCountVector[i] = val}

    	int[][] populationFeatureMatrix = new int[populationFeatureData.size()][populationFeatureData[0].size]
    	populationFeatureData.eachWithIndex {row, i ->
    		row.eachWithIndex {val, j ->
    			populationFeatureMatrix[i][j] = val
    		}
    	}

    	def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
    	fMatch.numRequestedControls = numControls
    	def comboVector = fMatch.calculatePossibleHitCombinationsPerFeature(populationFeatureMatrix, requiredCountVector) 

    	then:
    	comboVector.size() == expectedNumFeatures
    	fMatch.numFeatures == expectedNumFeatures

    	def labels = MappedDataTableStratum.getLabelsForBucketList(featureStrata)

    	expectedComboVector.eachWithIndex {expVal, i ->
    		assert comboVector[i] == expVal
    	}

    	where:
    	featureStrata << [
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")]
    	]
    	requiredCountData << [
    			[ 1, 0, 3, 1, 1, 2, 3, 0, 0, 0, 0]
    	]
    	populationFeatureData << [
    		[	[ 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 ]
        	]
    	]
    	numControls << [3]

    	// 5 choose 3 = 10
    	expectedComboVector << [
    		[-1, 0, 10, 1, 2, 1, -1, 0, 0, 0, 0]
    	]
    	expectedNumFeatures << [11]
    }

    @Unroll
    def "iterator test" () {
    	when:
    	def combinations = CombinatoricsUtils.combinationsIterator(n, k)

    	then:
    	def i = 0
		while(combinations.hasNext() && (i <= expectedI + 5)) { 
			i++
			def c = combinations.next()
			//println "combo:"
			//println c
		}
		assert i == expectedI

		where:
		n << [3,0,1]
		k << [2,0,0]
		expectedI << [3,1,1]

    }

    @Unroll
    def "combinations doFeatureMatching test" () {
    	when:
    	println "---determinsitic---expected:$expectedResult"
    	def caseFeature = new MappedDataTable(name:"testCase",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
    	def controlFeature = new MappedDataTable(name:"testControl",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)

    	int[] requiredCountVector = new int[requiredCountData.size()]
    	requiredCountData.eachWithIndex {val, i -> requiredCountVector[i] = val}

    	int[][] populationFeatureMatrix = new int[populationFeatureData.size()][populationFeatureData[0].size]
    	populationFeatureData.eachWithIndex {row, i ->
    		row.eachWithIndex {val, j ->
    			populationFeatureMatrix[i][j] = val
    		}
    	}

    	def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
        fMatch.GENERAL_STRATEGY = RowSelectionStrategy.COMBINATIONS

    	// hardcode these for the tests
    	fMatch.numRequestedControls = numControls
    	fMatch.controlPopulationFeatureMatrix = populationFeatureMatrix
    	def result = fMatch.doFeatureMatching(requiredCountVector) 

    	then:
        println "----actual:$result"
    	def combos = fMatch.calculatePossibleHitCombinationsPerFeature(populationFeatureMatrix, requiredCountVector)
    	combos == expectedComboVector

    	//fMatch.featureIndexRanking == expectedFeatureIndexRanking
    	result == expectedResult

    	where:
    	featureStrata << [
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
    		[MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
    		MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")]
    	]

    	requiredCountData << [
    			[ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
    			[ 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    			[ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    			[ 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    			[ 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    	]
    	populationFeatureData << [
    		[	[ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
        	],
        	[	[ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
        	],
        	[	[ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
        	],
        	[	[ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 ]
        	],
        	 [	[ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
	            [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
        	]
    	]
    	numControls << [3, 3, 3, 3, 6]

    	//TODO: update for new ranking scheme
    	expectedFeatureIndexRanking << [
	    	[	
	    		["index":1, "rank":-1],
	    	 	["index":2, "rank":-1],
	    	 	["index":3, "rank":-1],
	    	 	["index":4, "rank":-1],
	    	 	["index":5, "rank":-1],
	    	 	["index":6, "rank":-1],
	    	 	["index":7, "rank":-1],
	    	 	["index":8, "rank":-1],
	    	 	["index":9, "rank":-1],
	    	 	["index":10, "rank":-1],
	    	 	["index":0, "rank":2],
	    	],
	    	[	
	    	 	["index":2, "rank":0],
	    	 	["index":3, "rank":0],
	    	 	["index":4, "rank":0],
	    	 	["index":5, "rank":0],
	    	 	["index":6, "rank":0],
	    	 	["index":7, "rank":0],
	    	 	["index":8, "rank":0],
	    	 	["index":9, "rank":0],
	    	 	["index":10, "rank":0],
	    	 	["index":0, "rank":2],
	    	 	["index":1, "rank":3],
	    	],
	    	[	
	    		["index":1, "rank":0],
	    	 	["index":2, "rank":0],
	    	 	["index":3, "rank":0],
	    	 	["index":4, "rank":0],
	    	 	["index":5, "rank":0],
	    	 	["index":6, "rank":0],
	    	 	["index":7, "rank":0],
	    	 	["index":8, "rank":0],
	    	 	["index":9, "rank":0],
	    	 	["index":10, "rank":0],
	    	 	["index":0, "rank":2],
	    	],
	    	[	
	    	 	["index":2, "rank":0],
	    	 	["index":3, "rank":0],
	    	 	["index":4, "rank":0],
	    	 	["index":5, "rank":0],
	    	 	["index":6, "rank":0],
	    	 	["index":7, "rank":0],
	    	 	["index":8, "rank":0],
	    	 	["index":9, "rank":0],
	    	 	["index":10, "rank":0],
	    	 	["index":0, "rank":2],
	    	 	["index":1, "rank":3],
	    	],
	    	[	
	    	 	["index":2, "rank":0],
	    	 	["index":3, "rank":0],
	    	 	["index":4, "rank":0],
	    	 	["index":5, "rank":0],
	    	 	["index":6, "rank":0],
	    	 	["index":7, "rank":0],
	    	 	["index":8, "rank":0],
	    	 	["index":9, "rank":0],
	    	 	["index":10, "rank":0],
	    	 	["index":0, "rank":1],
	    	 	["index":1, "rank":5],
	    	],
    	]

    	// becomes 'rank' in expectedFeatureindexRanking
    	expectedComboVector << [
    		[2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
    		[2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    		[2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    		[2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    		[1716, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    	]

    	expectedResult << [
    		[success:false, message:"no exact solution exists"],
    		[success:true, message:"solution found", rowIds:[1,3,4]],
    		[success:true, message:"solution found", rowIds:[0,2,3]],
    		[success:false, message:"no selection found"],
    		[success:true, message:"solution found", rowIds:[6,7,8,9,10,11]],
    	]
    }

    @Unroll
    def "random doFeatureMatching test" () {
        when:
        println "---random---$expectedResult"
        def caseFeature = new MappedDataTable(name:"testCase",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
        def controlFeature = new MappedDataTable(name:"testControl",
                    idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)

        int[] requiredCountVector = new int[requiredCountData.size()]
        requiredCountData.eachWithIndex {val, i -> requiredCountVector[i] = val}

        int[][] populationFeatureMatrix = new int[populationFeatureData.size()][populationFeatureData[0].size]
        populationFeatureData.eachWithIndex {row, i ->
            row.eachWithIndex {val, j ->
                populationFeatureMatrix[i][j] = val
            }
        }

        def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
        fMatch.GENERAL_STRATEGY = RowSelectionStrategy.RANDOM_CHOICE

        // hardcode these for the tests
        fMatch.numRequestedControls = numControls
        fMatch.controlPopulationFeatureMatrix = populationFeatureMatrix
        def result = fMatch.doFeatureMatching(requiredCountVector) 

        then:
        println "----actual:$result"
        def combos = fMatch.calculatePossibleHitCombinationsPerFeature(populationFeatureMatrix, requiredCountVector)
        combos == expectedComboVector

        //fMatch.featureIndexRanking == expectedFeatureIndexRanking
        result.success == expectedResult.success
        result.message == expectedResult.message
        result.rowIds as Set == expectedResult.rowIds as Set


        where:
        featureStrata << [
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")]
        ]

        requiredCountData << [
                [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
                [ 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                [ 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                [ 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        ]
        populationFeatureData << [
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
            ],
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
            ],
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
            ],
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 ]
            ],
             [  [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
            ]
        ]
        numControls << [3, 3, 3, 3, 6]

        //TODO: update for new ranking scheme
        expectedFeatureIndexRanking << [
            [   
                ["index":1, "rank":-1],
                ["index":2, "rank":-1],
                ["index":3, "rank":-1],
                ["index":4, "rank":-1],
                ["index":5, "rank":-1],
                ["index":6, "rank":-1],
                ["index":7, "rank":-1],
                ["index":8, "rank":-1],
                ["index":9, "rank":-1],
                ["index":10, "rank":-1],
                ["index":0, "rank":2],
            ],
            [   
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":2],
                ["index":1, "rank":3],
            ],
            [   
                ["index":1, "rank":0],
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":2],
            ],
            [   
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":2],
                ["index":1, "rank":3],
            ],
            [   
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":1],
                ["index":1, "rank":5],
            ],
        ]

        // becomes 'rank' in expectedFeatureindexRanking
        expectedComboVector << [
            [2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
            [2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [1716, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        ]

        expectedResult << [
            [success:false, message:"no exact solution exists"],
            [success:true, message:"solution found", rowIds:[1,3,4]],
            [success:true, message:"solution found", rowIds:[0,2,3]],
            [success:false, message:"no selection found"],
            [success:true, message:"solution found", rowIds:[6,7,8,9,10,11]],
        ]
    }

    /*
    @Unroll
    def "dynamic doFeatureMatching test" () {
        when:
        println "---random---$expectedResult"
        def caseFeature = new MappedDataTable(name:"testCase",
                idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)
        def controlFeature = new MappedDataTable(name:"testControl",
                idFieldName:'EMPI',
                idKeyType:KeyType.EMPI)

        int[] requiredCountVector = new int[requiredCountData.size()]
        requiredCountData.eachWithIndex {val, i -> requiredCountVector[i] = val}

        int[][] populationFeatureMatrix = new int[populationFeatureData.size()][populationFeatureData[0].size]
        populationFeatureData.eachWithIndex {row, i ->
            row.eachWithIndex {val, j ->
                populationFeatureMatrix[i][j] = val
            }
        }

        def fMatch = new FrequencyMatcher(caseFeature, controlFeature, featureStrata)
        fMatch.GENERAL_STRATEGY = RowSelectionStrategy.DYNAMIC

        // hardcode these for the tests
        fMatch.numRequestedControls = numControls
        fMatch.controlPopulationFeatureMatrix = populationFeatureMatrix
        def result = fMatch.doFeatureMatching(requiredCountVector) 

        then:
        println "----actual:$result"
        def combos = fMatch.calculatePossibleHitCombinationsPerFeature(populationFeatureMatrix, requiredCountVector)
        combos == expectedComboVector

        //fMatch.featureIndexRanking == expectedFeatureIndexRanking
        result.success == expectedResult.success
        result.message == expectedResult.message
        result.rowIds as Set == expectedResult.rowIds as Set


        where:
        featureStrata << [
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")],
            [MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("sex"), 
            MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("race")]
        ]

        requiredCountData << [
                [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
                [ 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                [ 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                [ 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        ]
        populationFeatureData << [
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
            ],
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
            ],
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ]
            ],
            [   [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 ]
            ],
             [  [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
                [ 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
            ]
        ]
        numControls << [3, 3, 3, 3, 6]

        //TODO: update for new ranking scheme
        expectedFeatureIndexRanking << [
            [   
                ["index":1, "rank":-1],
                ["index":2, "rank":-1],
                ["index":3, "rank":-1],
                ["index":4, "rank":-1],
                ["index":5, "rank":-1],
                ["index":6, "rank":-1],
                ["index":7, "rank":-1],
                ["index":8, "rank":-1],
                ["index":9, "rank":-1],
                ["index":10, "rank":-1],
                ["index":0, "rank":2],
            ],
            [   
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":2],
                ["index":1, "rank":3],
            ],
            [   
                ["index":1, "rank":0],
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":2],
            ],
            [   
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":2],
                ["index":1, "rank":3],
            ],
            [   
                ["index":2, "rank":0],
                ["index":3, "rank":0],
                ["index":4, "rank":0],
                ["index":5, "rank":0],
                ["index":6, "rank":0],
                ["index":7, "rank":0],
                ["index":8, "rank":0],
                ["index":9, "rank":0],
                ["index":10, "rank":0],
                ["index":0, "rank":1],
                ["index":1, "rank":5],
            ],
        ]

        // becomes 'rank' in expectedFeatureindexRanking
        expectedComboVector << [
            [2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
            [2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            [1716, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        ]

        expectedResult << [
            [success:false, message:"no exact solution exists"],
            [success:true, message:"solution found", rowIds:[1,3,4]],
            [success:true, message:"solution found", rowIds:[0,2,3]],
            [success:false, message:"no selection found"],
            [success:true, message:"solution found", rowIds:[6,7,8,9,10,11]],
        ]
    }
    */
}