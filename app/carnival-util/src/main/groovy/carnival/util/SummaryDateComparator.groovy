package carnival.util

/**
 * Summary of data with respect to a given date
 *
 * For example, Average BMI for all encounter measurements that take place before a biobank encounter
 */
enum SummaryDateComparator {
	/** less than */
	LT, 
	
	/** less than or equal to */
	LTEQ, 

	/** equal */
	EQ, 
	
	/** greater than */
	GT, 
	
	/** greater than or equal to */
	GTEQ, 
	
	/** all */
	ALL, 
}