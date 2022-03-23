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
 * Inherited by traits that can have PropertyDefs: VertexDefTrait and EdgedDefTrait
 *
 * @see carnival.graph.PropertyDefTrait
 * @see carnival.graph.EdgeDefTrait
 */
@Slf4j
trait WithPropertyDefsTrait {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    Set<PropertyDefTrait> propertyDefs = new LinkedHashSet<PropertyDefTrait>()



    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public WithPropertyDefsTrait withPropertyDef(PropertyDefTrait propertyDef) {
        propertyDefs.add(propertyDef)
        return this
    }




    ///////////////////////////////////////////////////////////////////////////
    // PROPERTY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Set<String> getUniquePropertyLabels() {
        return uniqueProperties*.label
    }


    /** */
    public Set<String> getRequiredPropertyLabels() {
        return requiredProperties*.label
    }


    /** */
    public Set<String> getIndexedPropertyLabels() {
        return indexedProperties*.label
    }


    /** */
    public Set<PropertyDefTrait> getUniqueProperties() {
        return propertyDefs.findAll {
            it.unique
        }
    }


    /** */
    public Set<PropertyDefTrait> getRequiredProperties() {
        return propertyDefs.findAll {
            it.required
        }
    }


    /** */
    public Set<PropertyDefTrait> getIndexedProperties() {
        return propertyDefs.findAll {
            it.index
        }
    }


    /** */
    public Set<PropertyDefTrait> getDefaultProperties() {
        return propertyDefs.findAll {
            it.defaultValue != null
        }
    }

    /** */
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
