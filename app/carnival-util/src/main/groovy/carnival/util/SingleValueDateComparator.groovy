package carnival.util


/**
 * Specific value with respect to a given date
 *
 * For example, the most recent bmi that occured before the biobank recruitment date
 * 	or the lab value that occured closest to recruitment date
 *
 * if not %_MOSTRECENT or %_CLOSEST, return first occurance
 * if %_MOSTRECENT, return the latest occurence
 * if ALL_CLOSEST, return the closest occurance
 *
 */
enum SingleValueDateComparator {
	/** the oldest */
	ALL (desc:'the oldest'),

	/** the most recent */
	ALL_MOSTRECENT (desc:'the most recent'), 

	/** the closest to the date before or after */
	ALL_CLOSEST (desc:'the closest to the date before or after'),

	/** the oldest strictly before */
	LT (desc:'the oldest strictly before'), 

	/** the oldest before or same time as */
	LTEQ (desc:'the oldest before or same time as'), 

	/** same time as */
	EQ (desc:'same time as'), 

	/** the oldest after or the same time as */
	GTEQ (desc:'the oldest after or the same time as'),

	/** the oldest strictly after */
	GT (desc:'the oldest strictly after'), 

	/** the most recent before */
	LT_MOSTRECENT (desc:'the most recent before'), 

	/** the most recent before or same time as */
	LTEQ_MOSTRECENT (desc:'the most recent before or same time as'), 

	/** the most recent same time as */
	EQ_MOSTRECENT (desc:'the most recent same time as'), 

	/** the most recent after or the same time */
	GTEQ_MOSTRECENT (desc:'the most recent after or the same time'),

	/** the most recent strictly after */
	GT_MOSTRECENT (desc:'the most recent strictly after')

    final String description

    private SingleValueDateComparator(Map args) { 
    	this.description = args.desc
    }
}


