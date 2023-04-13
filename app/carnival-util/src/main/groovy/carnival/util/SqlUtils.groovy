package carnival.util



import java.util.concurrent.atomic.AtomicInteger

import java.sql.Timestamp
import java.sql.Time
import java.text.SimpleDateFormat

import groovy.transform.Synchronized
import groovy.sql.*







/**
 * Convenience class that offers useful utilities for interacting with
 * relational data sources.
 *
 */
class SqlUtils {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** default simple date format string */
	static public final String DEFAULT_TIMESTAMP_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss"

	/** default simple date format object */
	static public final SimpleDateFormat DEFAULT_TIMESTAMP_FORMATER = new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT_STRING)


	///////////////////////////////////////////////////////////////////////////
	// SCALAR VALUE CONVERTERS
	///////////////////////////////////////////////////////////////////////////

	/**
	 * if timestamp is a java.sql.timestamp object, return the string formatted
	 * if timestamp is a String, just return
	 */
	@Synchronized
	public static String timestampAsString(Object timestamp) {
		if (timestamp instanceof String) return timestamp
		else if (timestamp instanceof Timestamp) return DEFAULT_TIMESTAMP_FORMATER.format(timestamp)
		else if (timestamp instanceof Date) return DEFAULT_TIMESTAMP_FORMATER.format(timestamp)
	    else throw RuntimeException("timestampAsString() can not parse class: '${timestamp.getClass()}'".toString())
	}


	/**
	 * if value can be parsed as a string, return the string value if it is a number
	 * otherwise return null
	 */
	@Synchronized
	public static String numberAsString(Object value) {
		def stringVal = (value instanceof String) ? value : String.valueOf(value)
		if (stringVal.isNumber()) return stringVal
		else return null
	}


	/**
	 * if timestamp is a java.sql.timestamp object, return the string formatted
	 * if timestamp is a String, just return
	 */
	@Synchronized
	public static Date valueAsTimestamp(Object timestamp, String format = "yyyy-MM-dd HH:mm:ss") {
		if (timestamp instanceof Timestamp) {
			return timestamp
		} 
		else if (timestamp instanceof Date) {
			return new java.sql.Timestamp(timestamp.getTime())
		} 
		else if (timestamp instanceof String) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format)
   			def parsedDate = dateFormat.parse(timestamp)
   			return new java.sql.Timestamp(parsedDate.getTime())
	    } 
	    else {
	    	throw RuntimeException("stringAsTimestamp() can not parse class: '${timestamp.getClass()}'")
	    }
	}


	/**
	 * if date is a java.util.date object, return the string formatted
	 * if date is a String, just return
	 */
	@Synchronized
	public static java.sql.Date valueAsSqlTime(Object date, String format = DEFAULT_TIMESTAMP_FORMAT_STRING) {
		if (date instanceof Timestamp) {
			//Date parsedDate = 
			//return new java.sql.Datestamp(date.getTime())
			assert false : "objects of type 'Timestamp' not yet supported"
		}
		else if (date instanceof Date) { 
			def parsedDate = new java.sql.Date(date.getTime())
			return parsedDate
		}
		else if (date instanceof String) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format)
   			def parsedDate = dateFormat.parse(date)
   			def parsedSqlDate = new java.sql.Date(parsedDate.getTime())
   			return parsedSqlDate
	    }
	    else {
	    	throw RuntimeException("valueAsSqlDate() can not parse class: '${date.getClass()}'")
	    }
	}




	///////////////////////////////////////////////////////////////////////////
	// WHERE CLAUSES
	///////////////////////////////////////////////////////////////////////////


	/**
	 * Accept a list of items and produce the where sub-clauses.
	 *
	 * returns something like:
	 * 	(code in ("1", "2", "3%") or code in ("4", "5", "6"))
	 *  (lab in ("lab1", "lab2", "lab3") or lab in ("labA", "labB", "labC"))
	 *
	 */
	static public String chunkedWhereClauseInts(String fieldName, Collection<Integer> allItems, int maxItemsPerClause = 1000) {
		def fieldIns = chunkedWhereClausesInts(fieldName, allItems, maxItemsPerClause)
		def ored = fieldIns.join("\nor ")
		def out = "($ored)"
		return out
	}

	/**
	 * Accept a list of items and produce a list of where sub-clauses.
	 *
	 * returns something like:
	 * 	[
	 *		'code in ("1", "2", "3%")', 
	 *		'code in ("4", "5", "6")'
	 *	]
	 *
	 */
	static public Collection<String> chunkedWhereClausesInts(String fieldName, Collection<Integer> allItems, int maxItemsPerClause = 1000) {
		def subClauses = inClauses(allItems, maxItemsPerClause, false)
		def fieldIns = subClauses.collect { "$fieldName in $it" }
		return fieldIns
	}


	/**
	 * Accept a list of items and produce the where sub-clauses.
	 *
	 * returns something like:
	 * 	(code in ("1", "2", "3%") or code in ("4", "5", "6"))
	 *  (lab in ("lab1", "lab2", "lab3") or lab in ("labA", "labB", "labC"))
	 *
	 */
	static public String chunkedWhereClauseCaseInsensitive(String fieldName, Collection<String> allItems, int maxItemsPerClause = 1000) {
		def fieldIns = chunkedWhereClausesCaseInsensitive(fieldName, allItems, maxItemsPerClause)
		def ored = fieldIns.join("\nor ")

		def out = "($ored)"

		return out
	}


	/**
	 * Return a collection of where clauses where the comparison is case insensitive.
	 *
	 * @see #inClauses(Collection<String> allItems, int maxItemsPerClause, boolean quoteItems = true)
	 *
	 */
	static public Collection<String> chunkedWhereClausesCaseInsensitive(String fieldName, Collection<String> allItems, int maxItemsPerClause = 1000) {
		def upperCasedItems = allItems.collect {it.toUpperCase()}
		def subClauses = inClauses(upperCasedItems, maxItemsPerClause)
		def fieldIns = subClauses.collect { "upper($fieldName) in $it" }
		return fieldIns
	}

	/**
	 * Accept a list of items and produce the where sub-clauses.
	 *
	 * returns something like:
	 * 	(code in ("1", "2", "3%") or code in ("4", "5", "6"))
	 *  (lab in ("lab1", "lab2", "lab3") or lab in ("labA", "labB", "labC"))
	 *
	 */
	static public String chunkedWhereClause(String fieldName, Collection<String> allItems, int maxItemsPerClause = 1000) {
		def fieldIns = chunkedWhereClauses(fieldName, allItems, maxItemsPerClause)
		def ored = fieldIns.join("\nor ")
		def out = "($ored)"
		return out
	}


	/**
	 * Return a collection of where clauses that use an 'in' construction to include all items in allItems.
	 *
	 */
	static public Collection<String> chunkedWhereClauses(String fieldName, Collection<String> allItems, int maxItemsPerClause = 1000) {
		def subClauses = inClauses(allItems, maxItemsPerClause)
		def fieldIns = subClauses.collect { "$fieldName in $it" }
		return fieldIns
	}


	/**
	 * Produces clauses like:
	 *
	 * (testField in ("1.1", "1,2") OR (testField like "411.%") OR (testField like "412.%"))
	 *
	 */
    static public String codeWhereClause(CodeRefGroup crg, String fieldName = 'code') {
    	assert crg

    	def codes = crg.individualCodeRefs*.baseCode
    	def wildcards = crg.wildcardRefs*.baseCode

    	def clauses = []

        if (codes) {
            clauses << chunkedWhereClause(fieldName, codes)
        }

        if (wildcards) {
        	//clauses.addAll(wildcards.collect{"$fieldName LIKE '" + it.replaceAll(/\*/, "%") + "'"})
        	clauses.addAll(wildcards.collect{"$fieldName LIKE '${it}%'"})
        }

        def q = "(\n"
        q += clauses.join(" OR ")
        q += ")"

        return q
    }


	/**
	 * Create a where in clause.  Add wildcard character to search strings.
	 *
	 * For wildcards:
	 *	("1", "2", "3")
	 *
	 * returns something like:
	 * 	(code like "1%" or code like "2%" or code like "3%")
	 *
	 */
	static public likeWildcardClause(Collection<String> wildcards, String fieldName, Boolean appendWildcardCharacter = false) {
		def clauses = appendWildcardCharacter ? wildcards.collect {"$fieldName like '${it}%'"} : wildcards.collect {"$fieldName like '${it}'"} 
        def str = clauses.join(" OR ")
		str = "($str)"

		// necessary to deal with special character issue
		return new String(str)
	}



	///////////////////////////////////////////////////////////////////////////
	// COMPARATOR WHERE CLAUSES
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Returns a SQL comparator clause for given fields and a FirstOrLastDateComparator comparator.
	 *
	 * @param field1 - event/order date
	 * @param field2 - recruitment date
	 *
	 */
	static public String comparatorWhereClause(String field1, String field2, FirstOrLastDateComparator comp) {
		return comparatorWhereClauseBuilder(field1, field2, comp.toString())
	}


	/**
	 * Returns a SQL comparator clause for given fields and a SummaryDateComparator comparator.
	 *
	 * @param field1 - event/order date
	 * @param field2 - recruitment date
	 *
	 */
	static public String comparatorWhereClause(String field1, String field2, SummaryDateComparator comp) {
		return comparatorWhereClauseBuilder(field1, field2, comp.toString())
	}


	/**
	 * Returns a SQL comparator clause for given fields and a SingleValueDateComparator comparator.
	 *
	 * @param field1 - event/order date
	 * @param field2 - recruitment date
	 *
	 */
	static public String comparatorWhereClause(String field1, String field2, SingleValueDateComparator comp) {
		return comparatorWhereClauseBuilder(field1, field2, comp.toString())
	}


	/**
	 * Returns a SQL comparator clause for given fields and a string comparator value.
	 *
	 * returns something like: 
	 * 	enc_date <= recruitment_date
	 *	order_date > recruitment_date
	 *
	 * @param field1 - event/order date
	 * @param field2 - recruitment date
	 *
	 */
	static private String comparatorWhereClauseBuilder(String field1, String field2, String compValue) {
		if (compValue == "ALL" 
			|| compValue == "ALL_MOSTRECENT"
			|| compValue == "ALL_CLOSEST") return "1 = 1"

		def compString = ""
		switch (compValue) {
			case "LT":
			case "LT_MOSTRECENT":
				compString = "<"
				break
			case "LTEQ":
			case "LTEQ_MOSTRECENT":
				compString = "<="
				break
			case "EQ":
			case "EQ_MOSTRECENT":
				compString = "="
				break
			case "GT":
			case "GT_MOSTRECENT":
				compString = ">"
				break
			case "GTEQ":
			case "GTEQ_MOSTRECENT":
				compString = ">="
				break
			default: 
				throw new RuntimeException("comparatorWhereClauseBuilder encountered unhandled DateComparator value: '$compValue'")
		}

		return "$field1 $compString $field2"
	}




	///////////////////////////////////////////////////////////////////////////
	// RANK ORDER COMPARATOR
	///////////////////////////////////////////////////////////////////////////

	/**
	* Returns the SQL chunk for a comparator rank order clause given a field name and a 
	* FirstOrLastDateComparator comparator.
	*
	* Returns statements like: 
	* 	ABS(enc_date - recruitment_date)
	*	order_date DESC
	*
	* @param field1 - event/order date
	* @param field2 - recruitment date
	*
	*/
	static public String comparatorRankOrder(String field1, String field2, SingleValueDateComparator comp) {
		if (comp == SingleValueDateComparator.ALL_CLOSEST) return " ABS($field1 - $field2) ASC "
		else if (comp in [SingleValueDateComparator.LT_MOSTRECENT, 
			SingleValueDateComparator.LTEQ_MOSTRECENT, 
			SingleValueDateComparator.EQ_MOSTRECENT,
			SingleValueDateComparator.GT_MOSTRECENT,
			SingleValueDateComparator.GTEQ_MOSTRECENT,
			SingleValueDateComparator.ALL_MOSTRECENT,]) return "$field1 DESC"
		return "$field1 ASC"
	}


	/**
	 * Returns the SQL chunk for a comparator rank order clause given a field name and a 
	 * FirstOrLastDateComparator comparator.
	 *
	 */
	static public String comparatorRankOrder(String field1, FirstOrLastDateComparator comp) {
		if (comp == SingleValueDateComparator.ALL_MOSTRECENT) return "$field1 DESC"
		else return "$field1 ASC"
	}



	///////////////////////////////////////////////////////////////////////////
	// IN CLAUSES
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Accept a list of items like [1, 2, 3, 4, 5] and produce a collection of in clauses
	 * like: ['(1, 2, 3)', '(4,5)']
	 *
	 */
	static public Collection<String> inClauses(Collection<String> allItems, int maxItemsPerClause, boolean quoteItems = true) {
		def itemChunks = allItems.collate(maxItemsPerClause)

		def inClauses = []
		itemChunks.each { chunk ->
			inClauses << inClause(chunk)
		}

		return inClauses
	}


	/**
	 * Generates the parenthesized list that comes after a SQL 'IN' word.
	 * Returns strings like: 
	 *   (1, 2, 3)
	 *   ('a', 'b', 'c')
	 *
	 */
	static public String inClause(Collection<String> allItems, boolean quoteItems = true) {
		def quoted = (quoteItems) ? allItems.collect { "\'" + it + "\'" } : allItems
		def str = quoted.join(", ")
		str = "($str)"

		// necessary to deal with special character issue
		return new String(str)
	}

}



