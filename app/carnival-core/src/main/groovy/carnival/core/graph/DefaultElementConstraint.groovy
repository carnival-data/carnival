package carnival.core.graph


import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/** 
 * A default implementation of ElementConstraint, which is used by the 
 * default graph validator.
 * @see ElementConstraint
 */
@ToString
@EqualsAndHashCode(allProperties=true)
class DefaultElementConstraint implements ElementConstraint {
	
	/** element label */
	String label
	
	/** element name space */
	String nameSpace
	
	/** whether the element constraing is global */
	boolean global

	/** return the global field */
	boolean isGlobal() { return global }
}
