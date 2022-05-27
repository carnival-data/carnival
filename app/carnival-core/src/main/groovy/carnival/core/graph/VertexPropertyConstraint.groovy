package carnival.core.graph



import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Defines the rules governing the given property.
 *   - Unique asserts that the property values must be unique
 *   across all vertices with a given label that have that
 *   property.
 *   - Required asserts that the property must be present
 *   and non-null(?)
 *   - Index instructs Carnival to use the underlying graph
 *   database services to index the given property for 
 *   efficient lookup.
 * 
 */
@ToString
class VertexPropertyConstraint {
	String name
	Boolean unique = false
	Boolean required = false
	Boolean index = false
}

