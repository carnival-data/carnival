package carnival.core.graph



import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Defines the rules governing the given property.
 * <p>
 * - Unique asserts that the property values must be unique
 * across all vertices with a given label that have that
 * property.
 * <p>
 * - Required asserts that the property must be present
 * and non-null(?)
 * <p>
 * - Index instructs Carnival to use the underlying graph
 * database services to index the given property for 
 * efficient lookup.
 */
@ToString
class VertexPropertyConstraint {

	/** The name of the property */
	String name

	/** Whether the values of this property must be unique */
	Boolean unique = false

	/** Whether this property is required to exist */
	Boolean required = false

	/** Whether this property should be indexed by the database */
	Boolean index = false
}

