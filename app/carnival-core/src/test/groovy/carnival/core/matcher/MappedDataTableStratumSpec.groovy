package carnival.core.matcher

import spock.lang.Specification
import spock.lang.Unroll
import carnival.core.*
import carnival.core.matcher.*
import carnival.pmbb.*
import carnival.pmbb.vine.*


/**
 * gradle -Dtest.single=MappedDataStratumSpec test
 */
class MappedDataTableStratumSpec extends Specification {
	    def "MappedDataTableStratum placeholder test"() {
    	setup:
    	def bSpec = new MappedDataTableStratum()

    	when: bSpec.masterBucketIndexOffset = offset

    	then:
    	bSpec.masterBucketIndexOffset == offset

    	where:
    	offset << [5]
    }

    //@Unroll
    def "generateStandardGenderMappedDataTableStratum test"() {
    	when:
    	def bSpec = MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("genderKey")

    	then:
    	expectedBucket == bSpec.getMasterBucketIndex(value)
    	bSpec.bucketNames.size() == 3

    	where:
    	value << ["m", "M", "mAle", "f", "F", "FeMaLe", "~", "", "unknown"]
    	expectedBucket << [0, 0, 0, 1, 1, 1, 2, 2, 2]
    }

    //@Unroll
    def "generateStandardRaceMappedDataTableStratum test"() {
    	when:
    	def bSpec = MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("raceKey")

    	then:
    	expectedBucket == bSpec.getMasterBucketIndex(value)
    	bSpec.bucketNames.size() == 8

    	where:
    	value << ['BLACK','UNKNOWN','HI PAC ISLAND','ASIAN','AM IND AK NATIVE','OTHER','WHITE','~',"","foobar"]
    	expectedBucket << [0,1,2,3,4,5,6,7,7,7]
    }

    //@Unroll
    def "generateStandardAgeMappedDataTableStratum test"() {
    	when:
    	def bSpec = MappedDataTableStratum.generateStandardAgeMappedDataTableStratum("ageKey")
    	def expectedTotalBuckets = 70

    	then:
    	//println bSpec.bucketNames

    	bSpec.getNumberOfBuckets() == expectedTotalBuckets
    	bSpec.bucketNames.size() == expectedTotalBuckets
    	bSpec.bucketCriteria.size() == expectedTotalBuckets - 1

    	expectedBucket == bSpec.getMasterBucketIndex(value)

    	where:
    	value << [16, 17, 18, 41, 84, 85, 100, "~"]
    	expectedBucket << [0, 0, 1, 24, 67, 68, 68, 69]
    }

    def "updateBucketIndiciesForMappedDataTableStratumList test" () {
    	when:
    	def specs = []
    	specs.add(MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("spec1"))
    	specs.add(MappedDataTableStratum.generateStandardRaceMappedDataTableStratum("spec2"))
    	specs.add(MappedDataTableStratum.generateStandardAgeMappedDataTableStratum("spec3"))
    	specs.add(MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("spec4"))

    	MappedDataTableStratum.updateBucketIndiciesForMappedDataTableStratumList(specs)

    	then:
    	specs[0].getMasterBucketIndex("m") == 0
    	specs[0].getMasterBucketIndex("f") == 1
    	specs[0].getMasterBucketIndex("~") == 2

    	specs[1].getMasterBucketIndex("black") == 3
    	specs[1].getMasterBucketIndex("~") == 10

    	specs[2].getMasterBucketIndex(2) == 11
    	specs[2].getMasterBucketIndex(18) == 12
    	specs[2].getMasterBucketIndex("~") == 80

    	specs[3].getMasterBucketIndex("m") == 81
    	specs[3].getMasterBucketIndex{"~"} == 83
    }

    def "totalFeaturesForBucketList test" () {
    	when:
    	def specs = []
    	specs.add(MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("spec1"))
    	specs.add(MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("spec2"))

    	then:
    	MappedDataTableStratum.totalFeaturesForBucketList(specs) == 6
    }

    def "getMasterIndicies test"() {
        when:
        def specs = []
        specs.add(MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("spec1"))
        specs.add(MappedDataTableStratum.generateStandardGenderMappedDataTableStratum("spec2"))
        MappedDataTableStratum.updateBucketIndiciesForMappedDataTableStratumList(specs)

        then:
        MappedDataTableStratum.totalFeaturesForBucketList(specs) == 6

        specs[0].masterBucketIndexOffset == 0
        specs[1].masterBucketIndexOffset == 3


        specs[0].getMasterIndicies() == [0, 1, 2]
        specs[1].getMasterIndicies() == [3, 4, 5]
    }


}