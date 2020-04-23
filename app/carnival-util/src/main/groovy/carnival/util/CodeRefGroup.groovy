package carnival.util



import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j

import org.slf4j.Logger
import org.slf4j.LoggerFactory



/**
 * CodeRefGroup ecapsulates a collection of code references expected to be used
 * for querying.  
 *
 */
@ToString(excludes=['individualCodeRefs', 'wildcardRefs'])
@EqualsAndHashCode
@Slf4j
class CodeRefGroup {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /**
     * Convenience factory method to create a CodeRefGroup object.
     *
     * @param name The name of the code ref group.
     * @param icd A collection of ICD code strings, ICD version not specified.
     * @param icd9 A collection of ICD v9 code strings.
     * @param icd10 A collection of ICD v10 code strings.
     * @param cpt A collection of CPT procedure codes.
     *
     */
	static CodeRefGroup create(Map args) {
		assert args.name?.trim()
		CodeRefGroup grp = new CodeRefGroup(name:args.name)
		if (args.icd) grp.addCodeRefs(CodeSystem.ICD, args.icd)
		if (args.icd9) grp.addCodeRefs(CodeSystem.ICD9, args.icd9)
		if (args.icd10) grp.addCodeRefs(CodeSystem.ICD10, args.icd10)
		if (args.cpt) grp.addCodeRefs(CodeSystem.CPT, args.cpt)
		return grp
	}


    /** 
     * Construct ICD wildcard strings from a list of ranges.  This method
     * takes a variable number of arguments, each pair of which represents
     * a range.  For example:
     * 
     * icdCodeStringsFromRanges(8, 10, 140, 142) == ['8.*', '9.*', '10.*', '140.*', '141.*', '142.*']
     *
     * @param args ICD code ranges in pairs.
     *
     */
    static List<String> icdCodeStringsFromRanges(Integer... args) {
        icdCodeStringsFromRanges(args.toList()*.toString())
    }


    /**
     * Construct ICD wildcard strings from a list of ranges.  This method
     * takes a variable number of arguments, each pair of which represents
     * a range.  For example:
     * 
     * icdCodeStringsFromRanges('8', '10', 'C00', 'C02') == ['8.*', '9.*', '10.*', 'C00.*', 'C01.*', 'C02.*']
     *
     * @param args ICD code ranges in pairs.
     *
     */
    static List<String> icdCodeStringsFromRanges(String... args) {
        icdCodeStringsFromRanges(args.toList())
    }


    /** 
     * Construct ICD wildcard strings from a list of ranges.  This method
     * takes a list of strings, each pair of which represents a range.
     * For example:
     *
     * icdCodeStringsFromRanges(['8', '10', 'C00', 'C02']) == ['8.*', '9.*', '10.*', 'C00.*', 'C01.*', 'C02.*']
     *
     */
    static List<String> icdCodeStringsFromRanges(List<String> args) {
        def numArgs = args.size()
        assert numArgs >= 2
        assert numArgs % 2 == 0

        List<String> strings = new ArrayList<String>()

        def vals = args.iterator()
        while (vals.hasNext()) {
        	def rangeStart = vals.next()
        	def rangeStop = vals.next()

        	strings.addAll(icdCodeStringsFromRange(rangeStart, rangeStop))
        }

        return strings
    }


    /**
     * Construct ICD wildcard strings from an ICD code range.
     * For example:
     *
     * icdCodeStringsFromRange('8', '10') == ['8.*', '9.*', '10.*']
     * icdCodeStringsFromRange('C00', 'C02') == ['C00.*', 'C01.*', 'C02.*']
     *
     * @param rangeStart The first ICD code in the range.
     * @param rangeStop The last ICD code in the range.
     *
     */
	static List<String> icdCodeStringsFromRange(String rangeStart, String rangeStop) {
		def rangePrefixes = icdCodeStringPrefixesFromRange(rangeStart, rangeStop)
		return rangePrefixes.collect { "${it}.*" }
	}


	/**
	 * Construct ICD code prefixes from an ICD code range.
	 * For example:
	 *
     * icdCodeStringPrefixesFromRange('8', '10') == ['8', '9', '10']
     * icdCodeStringPrefixesFromRange('C00', 'C02') == ['C00', 'C01', 'C02']
     *
     * @param rangeStart The first ICD code in the range.
     * @param rangeStop The last ICD code in the range.
	 *
	 */
	static List<String> icdCodeStringPrefixesFromRange(String rangeStart, String rangeStop) {
		log.trace "icdCodeStringPrefixesFromRange $rangeStart $rangeStop"

		List<String> prefixes = new ArrayList<String>()
		
		if (rangeStart.isInteger() && rangeStop.isInteger()) {
			(rangeStart.toInteger()..rangeStop.toInteger()).each { prefixes << "${it}" }
			return prefixes
		}

		def rangeStartPrefix = rangeStart.take(1)
		def rangeStartPostfix = rangeStart.drop(1)
		def rangeStopPrefix = rangeStop.take(1)
		def rangeStopPostfix = rangeStop.drop(1)

		if (rangeStartPrefix == rangeStopPrefix && rangeStartPostfix.isInteger() && rangeStopPostfix.isInteger()) {
			def numDigits = Math.max(rangeStartPostfix.length(), rangeStopPostfix.length())
			//log.trace "numDigits: $numDigits"
			(rangeStartPostfix.toInteger()..rangeStopPostfix.toInteger()).each { 
				prefixes << "${rangeStartPrefix}" + "${it}".padLeft(numDigits, '0')
			}
			return prefixes
		}

		// if we've gotten this far, throw an exception
		throw new IllegalArgumentException("cannot return prefixes for range ${rangeStart} to ${rangeStop}")
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** optional name for the group */
	String name

	/** list of code refs */
	SortedSet<CodeRef> codeRefs = new TreeSet<CodeRef>()


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Return a new CodeRefGroup object that contains only the codes of this
	 * object that are associated with the given system.
	 *
	 * @param codeSystem The code system whose codes will populate the new
	 *                   CodeRefGroup.
	 *
	 */
	public CodeRefGroup subGroup(CodeSystem codeSystem) {
		def codesOfGivenSystem
		if (codeSystem == CodeSystem.ICD) {
			codesOfGivenSystem = codeRefs.findAll { 
				[CodeSystem.ICD, CodeSystem.ICD9, CodeSystem.ICD10].contains(it.system) 
			}
		} else {
			codesOfGivenSystem = codeRefs.findAll { it.system == codeSystem }
		}
				
		return new CodeRefGroup(
			name: this.name,
			codeRefs: codesOfGivenSystem
		)
	}


	/**
	 * Add the given code refs to this group using the given code system.
	 *
	 * @param system The code system to apply to all code refs.
	 * @param codes The collection of code refs.
	 *
	 * @exception IllegalArgumentException if no codes are provided.
	 * @exception Exception if any code fails validation.
	 *
	 */
	public void addCodeRefs(CodeSystem system, Collection<String> codes) {
		assert system
		assert codes != null
		assert codes.size() > 0, "no codes provided: $codes"

		// clean up the list
		def cc = codes.toList().unique()

		// add all the codes
		cc.each { addCodeRef(system, it) }
	}


	/**
	 * Add a new CodeRef with the given system and value to this CodeRefGroup.
	 *
	 * @param system The system of the new CodeRef.
	 * @param value The value of the new CodeRef.
	 *
	 * @exception Exception If the newly created CodeRef fails validation.
	 *
	 */
	public void addCodeRef(CodeSystem system, String value) {
		def validationError = CodeRef.validateSyntax(value)
		if (validationError) throw new Exception(validationError)
		codeRefs << CodeRef.create(system, value)	
	}


	/**
	 * Get all the code refs that are not wildcards.
	 *
	 */
	public Collection<CodeRef> getIndividualCodeRefs() {
		codeRefs.findAll({ !it.isWildcard() }).sort({it.toString()})
	}


	/**
	 * Get all the code refs that are wildcards.
	 *
	 */
	public Collection<CodeRef> getWildcardRefs() {
		codeRefs.findAll({ it.isWildcard() }).sort({it.toString()})
	}

}

