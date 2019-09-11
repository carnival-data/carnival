package carnival.util



import java.util.Objects

import groovy.transform.InheritConstructors
import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode



/**
 * The set of recognized coding systems.
 *
 */
enum CodeSystem {
	ICD('ICD 9 or 10'), 
	ICD9('ICD vesion 9'), 
	ICD10('ICD version 10'), 
	CPT('Current Procedural Terminology'), 
	PHECODE('Phecodes from phewascatalog.org'), 
	LAB_DESCRIPTION('Penn Data Store lab descriptions'), 
	SEARCH_TERM('Search term that will be transformed to %SERCH_TERM%')

    final public String description

    private CodeSystem(String description) {
        this.description = description
    }
}



/**
 * CodeRef is a reference to a code from a coding system such as ICD. CodeRef
 * is not the code itself, but rather a reference to a code or set of codes in
 * the case of wildcard references.
 *
 * Wildcards are acceptable only as a single trailing * which has the same
 * meaning as .* in regular expression syntax.
 *
 */
//@ToString
class CodeRef implements Comparable<CodeRef> {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Convenience method to create a CodeRef object.
	 *
	 */
	static public CodeRef create(CodeSystem system, String value) {
		assert system
		assert value
		return new CodeRef(system:system, value:value)
	}


	/**
	 * Validates the syntax of the code reference.
	 *
	 * @return An error string iff the vaildation fails.
	 *
	 */
	static public String validateSyntax(String value) {
		def percents = value.findAll(/\%/)
		if (percents.size() > 0) return "percent not allowed; asterisc is only acceptable wildcard expression: '$value'"

		def asteriscs = value.findAll(/\*/)
		if (asteriscs.size() > 1) return "only one asterisc is allowed: '$value'"
		if (asteriscs.size() == 1 && !value.endsWith('*')) return "asterisc is only acceptable as final character of wildcard expression: '$value'"
		return null
	}


	/**
	 * Returns true iff the code reference is a wildcard.  For example, 140.*
	 * is a wildcard that would map to 140.% in SQL.
	 *
	 * @return A boolean indication of whether this code reference represents
	 *         a wildcard.
	 *
	 */
	static public boolean isWildcard(String value) {
		def asteriscs = value.findAll(/\*/)
		return (asteriscs.size() == 1 && value.endsWith('*'))
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** the coding system */
	CodeSystem system

	/** the code ref */
	String value



	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////


	/**
	 * Implementation of the hashCode method that builds a has based on the
	 * properties of this object, system and value.  Two CodeRef objects that
	 * have the same property values are considered the same object.
	 *
	 */
	@Override
	public int hashCode() {
		Objects.hash(system, value)
	}


	/**
	 * Implementation of the compareTo method that compares the string
	 * representations of the system and value, ie 'ICD9-140.1'.
	 *
	 */
	@Override
	public int compareTo(CodeRef cr) {
		String a = "${this.system}-${this.value}".toString()
		String b = "${cr.system}-${cr.value}".toString()
		a.compareTo(b)
	}


	/**
	 * Implementation of equals that compares the property values to determine
	 * equality.  Two CodeRef objects with the same property values are
	 * considered equal.
	 *
	 */
	@Override
	public boolean equals(Object cr) {
		if (this == cr) return true
		if (!(cr instanceof CodeRef)) return false
		
		this.system == cr.system && this.value == cr.value
	}


	/**
	 * Validates the syntax of the code reference.
	 *
	 * @return An error string iff the vaildation fails.
	 *
	 */
	public String validateSyntax() {
		return validateSyntax(value)
	}


	/**
	 * Returns true iff the code ref is a wildcard ref.
	 *
	 */
	public boolean isWildcard() {
		return isWildcard(value)
	}


	/**
	 * Returns the base code, ie the code with the trailing * stripped, if any.
	 *
	 */
	public String getBaseCode() {
		if (isWildcard()) return value.substring(0, value.size()-1)
		else return value
	}


	/**
	 * Return a string representation of this object.
	 *
	 */
	public String toString() {
		return "${system}:${value}"
	}


}

