package carnival.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Property

import carnival.util.StringUtils
import carnival.graph.Base



/** 
 * Inherited by traits that can have PropertyDefs: VertexDefinition and EdgedDefTrait
 *
 * @see carnival.graph.PropertyDefinition
 * @see carnival.graph.EdgeDefinition
 */
@Slf4j
trait WithPropertyDefsTrait {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** The set of property definitions */
    Set<PropertyDefinition> propertyDefs = new LinkedHashSet<PropertyDefinition>()



    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Add the provided property definition to the set of stored property 
     * definitions.
     * @param propertyDef The property definition to store
     * @return This object
     */
    public WithPropertyDefsTrait withPropertyDef(PropertyDefinition propertyDef) {
        propertyDefs.add(propertyDef)
        return this
    }




    ///////////////////////////////////////////////////////////////////////////
    // PROPERTY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Get the labels of the properties that should have unique values across
     * instantiated graph elements.
     * @return The set of string labels
     */
    public Set<String> getUniquePropertyLabels() {
        return uniqueProperties*.label
    }


    /** 
     * Get the labels of the properties that should be required across 
     * instantiated graph elements.
     * @return The set of string labels
     */
    public Set<String> getRequiredPropertyLabels() {
        return requiredProperties*.label
    }


    /** 
     * Get the labels of the properties that should be indexes in the graph
     * database engine.
     * @return The set of string labels
     */
    public Set<String> getIndexedPropertyLabels() {
        return indexedProperties*.label
    }


    /** 
     * Get the set of property definitions whose values should be unique
     * across the instantiated elements.
     * @return The set of property deinitions
     */
    public Set<PropertyDefinition> getUniqueProperties() {
        return propertyDefs.findAll {
            it.unique
        }
    }


    /** 
     * Get the set of property definitions that should be required to have 
     * values in instantiated elements.
     * @return The set of property definitions
     */
    public Set<PropertyDefinition> getRequiredProperties() {
        return propertyDefs.findAll {
            it.required
        }
    }


    /** 
     * Get the property definitions that should be indexed by the graph 
     * database engine.
     * @return The set of property definitions
     */
    public Set<PropertyDefinition> getIndexedProperties() {
        return propertyDefs.findAll {
            it.index
        }
    }


    /** 
     * Get the property definitions that have default values set.
     * @return The set of property definitions
     */
    public Set<PropertyDefinition> getDefaultProperties() {
        return propertyDefs.findAll {
            it.defaultValue != null
        }
    }

    /** 
     * Return the set of properties of the provided element that are defined
     * by this trait.
     * @param e The element to test
     * @return The set of properties.
     */
    public Set<Property> definedPropertiesOf(Element e) {
        Set<Property> eProps = new HashSet<Property>()
        propertyDefs.each { pDef ->
            def ep = e.property(pDef.label)
            if (Property.empty().equals(ep)) return
            if (!ep.isPresent()) return
            eProps.add(ep)
        }
        eProps
    }
}
