package carnival.graph



import groovy.transform.InheritConstructors



/** 
 * EdgeDomainException is expected to be thrown when the domain rules of an
 * edge definition have been violated.
 */
@InheritConstructors
class EdgeDomainException extends RuntimeException { }

/** 
 * EdgeRangeException is expected to be thrown when the range rules of an edge
 * definition have been violated.
 */
@InheritConstructors
class EdgeRangeException extends RuntimeException { }

/** 
 * RequiredPropertyException is expected to be thrown when the rules of a 
 * required property have been violated.
 */
@InheritConstructors
class RequiredPropertyException extends RuntimeException { }

