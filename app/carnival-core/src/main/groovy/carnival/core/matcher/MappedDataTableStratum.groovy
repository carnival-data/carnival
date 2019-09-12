package carnival.core.matcher



import com.google.common.collect.*



enum CriteriaType {INT_RANGE, STRING_SET}


/**
* Class that describes buckets to be used in frequency matching
*/
class MappedDataTableStratum {

	/**
	* Name of the key this bucket applies to
	*/
	String keyName

	/**
	* Human readable bucket names
	*/
	List bucketNames = []

	/**
	* int buckets: Ordered list of non-overlapping Range objects 
	* string buckets: Ordered list of sets of non-overlapping discrete string values
	*/
	List bucketCriteria = []
	Boolean caseSensitive = false
	CriteriaType criteriaType = CriteriaType.STRING_SET

	/**
	* Offset used for generating the master bucket index.
	* For example, if a value 'V' matches the second bucket function and masterBucketIndexOffset is 5,
	* 	a value of 7 will be returned.
	*
	* Slightly hacky, will eventually be handled by a class that contains multiple MappedDataTableStratums.
	*/
	int masterBucketIndexOffset = 0


	////////////////////////////////////
	// Common bucket generation methods
	////////////////////////////////////

	public static MappedDataTableStratum generateStandardAgeMappedDataTableStratum(String keyName, int minValue = 18, int maxValue = 85, int bucketSize = 1) {
		return generateStandardIntMappedDataTableStratum(keyName, minValue, maxValue, bucketSize)
	}
	/**
	*
	* Generates Range buckets of the form:
	* 	(-infinity .. minValue), [minValue..a), [a..b), ... , [z..maxValue), [maxValue .. infinity)
	*
	*/
	public static MappedDataTableStratum generateStandardIntMappedDataTableStratum(String keyName, int minValue = 0, int maxValue = 100, int bucketSize = 1) {
		def bucket = new MappedDataTableStratum()

		bucket.keyName = keyName
		bucket.criteriaType = CriteriaType.INT_RANGE

		List<String> names = []

		// (-infinity .. minValue)
		bucket.bucketCriteria.add(Range.lessThan(minValue))
		names << "x < $minValue"

		// [minValue..a), [a..b), ... , [z..maxValue)
		def bMin = minValue
		for (def bMax = (minValue + bucketSize); bMax <= maxValue; bMax += bucketSize) {
			if (bMax > maxValue) bMax = maxValue

			bucket.bucketCriteria.add(Range.closedOpen(bMin,bMax))
			names << "$bMin <= x < $bMax"

			bMin = bMax
		}

		if (bMin != maxValue) {
			bucket.bucketCriteria.add(Range.closedOpen(bMin,maxValue))
			names << "$bMin <= x < $maxValue"
		}

		// [maxValue .. infinity)
		bucket.bucketCriteria.add(Range.atLeast(maxValue))
		names.add("$maxValue <= x")

		// not specified bucket
		names.add("not specified")

		bucket.bucketNames = names

		return bucket
	}


	public static MappedDataTableStratum generateStandardGenderMappedDataTableStratum(String keyName) {
		def bucket = new MappedDataTableStratum()

		bucket.keyName = keyName
		bucket.bucketCriteria.add(["M", "MALE"] as Set)
		bucket.bucketCriteria.add(["F", "FEMALE"] as Set)
		bucket.caseSensitive = false
		bucket.criteriaType = CriteriaType.STRING_SET
		bucket.bucketNames = ['male', 'female', 'not specified']

		return bucket
	}

	public static MappedDataTableStratum generateStandardRaceMappedDataTableStratum(String keyName) {
		def bucket = new MappedDataTableStratum()

		bucket.keyName = keyName
		bucket.bucketCriteria.add(['BLACK', 'BLACK OR AFRICAN AMERICAN'] as Set)
		bucket.bucketCriteria.add(['UNKNOWN', 'UNKNOWN'] as Set)
		bucket.bucketCriteria.add(['HI PAC ISLAND', 'NATIVE HAWAIIAN OR OTHER PACIFIC ISLANDER'] as Set)
		bucket.bucketCriteria.add(['ASIAN', 'ASIAN'] as Set)
		bucket.bucketCriteria.add(['AM IND AK NATIVE', 'AMERICAN INDIAN OR ALASKAN NATIVE'] as Set)
		bucket.bucketCriteria.add(['OTHER', 'OTHER'] as Set)
		bucket.bucketCriteria.add(['WHITE', 'WHITE'] as Set)
		bucket.caseSensitive = false
		bucket.criteriaType = CriteriaType.STRING_SET
		bucket.bucketNames = ['BLACK','UNKNOWN','HI PAC ISLAND','ASIAN','AM IND AK NATIVE','OTHER','WHITE','not specified']

		return bucket
	}


	////////////////////////////////////
	// Bucket list management
	////////////////////////////////////
	/**
	* Given an ordered collection of MappedDataTableStratums, set the offsets for each spec so that the
	* masterBucketIndex for each bucket is unique.
	*/
	public static updateBucketIndiciesForMappedDataTableStratumList(Collection<MappedDataTableStratum> specs) {
		def lastIndex = 0
		specs.each { spec ->
			spec.masterBucketIndexOffset = lastIndex
			lastIndex += spec.getNumberOfBuckets()
		}
	}

	public static totalFeaturesForBucketList(Collection<MappedDataTableStratum> specs) {
		def lastIndex = 0
		specs.each { spec ->
			lastIndex += spec.getNumberOfBuckets()
		}
		return lastIndex
	}

	public static getLabelsForBucketList(Collection<MappedDataTableStratum> specs) {
		List<String> labels = []

		specs.each { spec ->
			labels.addAll(spec.bucketNames)
		}
		return labels
	}

	public static getBucketNamesForBucketList(Collection<MappedDataTableStratum> specs) {
		List<String> labels = []

		specs.each { spec ->
			spec.bucketNames.each {
				labels.add(spec.keyName)
			}
		}
		return labels
	}

	public static getLabelForBucketList(Collection<MappedDataTableStratum> specs, def idx) {
		List<String> labels = []

		specs.each { spec ->
			labels.addAll(spec.bucketNames)
		}
		return labels[idx]
	}


	////////////////////////////////////
	// Instance methods
	////////////////////////////////////
	/**
	* Get the total number of buckets.  This is the total number of bucketCriteria, plus one
	* for the bucket that holds values that did not match any criteria.
	*/
	public getNumberOfBuckets() {
		return bucketCriteria.size() + 1
	}
 
 	public getBucketIndexFromData(Map data) {
 		assert data.containsKey(keyName)
 		return getBucketIndex(data[keyName])
 	}

	public getBucketIndex(def value) {
		def bucketIndex = bucketCriteria.size() // default "NOS" bucket

		bucketCriteria.eachWithIndex {c, i ->
			if (criteriaType == CriteriaType.STRING_SET) {
				String testValue = value.toString()
				if (!caseSensitive) testValue = testValue.toUpperCase()
				if (testValue in c) {
					bucketIndex = i
					return
				}
			}
			else if (criteriaType == CriteriaType.INT_RANGE) {
				def testValue
				try {
				    testValue = value as Double
				} catch (e) {
				    return
				}
				if (testValue && c.contains(testValue.round() as Integer)) {
					bucketIndex = i
					return
				}
			}
		}

		return bucketIndex
	}

	////////////////////////////////
	// Bucket list instance methods
	////////////////////////////////
	public List getMasterIndicies() {
		def idxes = []
		(masterBucketIndexOffset..(masterBucketIndexOffset + bucketNames.size()-1)).each {
			idxes.add(it)
		}

		return idxes
	}

	public getMasterBucketIndex(def value) {
		return getBucketIndex(value) + masterBucketIndexOffset
	}
}