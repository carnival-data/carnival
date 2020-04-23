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

import carnival.util.StringUtils
import carnival.graph.Base



/** 
 *
 *
 */
@Slf4j
trait WithPropertyDefsTrait {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    List<PropertyDefTrait> propertyDefs = new ArrayList<PropertyDefTrait>()



    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public WithPropertyDefsTrait withPropertyDef(PropertyDefTrait propertyDef) {
        propertyDefs << propertyDef
        return this
    }




    ///////////////////////////////////////////////////////////////////////////
    // PROPERTY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public List<String> getUniquePropertyLabels() {
        return uniqueProperties*.label
    }


    /** */
    public List<String> getRequiredPropertyLabels() {
        return requiredProperties*.label
    }


    /** */
    public List<String> getIndexedPropertyLabels() {
        return indexedProperties*.label
    }


    /** */
    public List<PropertyDefTrait> getUniqueProperties() {
        return propertyDefs.findAll {
            it instanceof ConstrainedPropertyDefTrait &&
            it.unique
        }
    }


    /** */
    public List<PropertyDefTrait> getRequiredProperties() {
        return propertyDefs.findAll {
            it instanceof ConstrainedPropertyDefTrait &&
            it.required
        }
    }


    /** */
    public List<PropertyDefTrait> getIndexedProperties() {
        return propertyDefs.findAll {
            it instanceof ConstrainedPropertyDefTrait &&
            it.index
        }
    }


    /** */
    public List<PropertyDefTrait> getDefaultProperties() {
        return propertyDefs.findAll {
            it.defaultValue != null
        }
    }

}
