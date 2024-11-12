package carnival.core.graph



import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.PropertyDefinition
import carnival.graph.PropertyDefinition.Cardinality



/** 
 * Defined constraints on a property.
 *
 */
@ToString
class PropertyConstraint {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Create an property constraint from a property definition.
	 * @param propertyDef The property definition
	 * @return The property constraint
	 */
	static public PropertyConstraint create(PropertyDefinition propertyDef) {
		assert propertyDef

		PropertyConstraint rd = new PropertyConstraint(
			propertyDef:propertyDef,
			label:propertyDef.label,
			dataType:propertyDef.dataType,
			cardinality: propertyDef.cardinality
		)
		return rd
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** The property definition that defines the constraint parameters */ 
	PropertyDefinition propertyDef

	/** The property key to which this constraint applies */
	String label

	/** Data type */
	Class dataType

	/** The cardinality of the property */
	Cardinality cardinality

}


