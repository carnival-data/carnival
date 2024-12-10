package carnival.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Element



/** 
 * Inhertied by builder classes that create elements in the graph that can have properties.
 */
@Slf4j
class PropertyValuesHolder<T> {

    /** A map of property definitions to property values */
    Map<PropertyDefinition,Object> propertyValues = new HashMap<PropertyDefinition,Object>()

    /** If true, then properties must be defined to have a value */
    Boolean propertiesMustBeDefined = true


    /** 
     * Set the provided property to the provided enum value.
     * @param propDef The property to set
     * @param propValue The enum value to use
     * @return This object
     */
    public T withProperty(PropertyDefinition propDef, Enum propValue) {
        assert propDef != null
        assert propValue != null
        withProperty(propDef, propValue.name())
    }


    /** 
     * Set the provided property to the provided value.
     * @param propDef The property to set
     * @param prooValue The value to use
     * @return This object
     */
    public T withProperty(PropertyDefinition propDef, Object propValue) {
        assert this.respondsTo("getElementDef")
        ElementDefinition eDef = getElementDef()

        if (propertiesMustBeDefined) {
            boolean found = eDef.propertyDefs.find({it.label == propDef.label})
            if (!found) throw new IllegalArgumentException("${propDef} is not a property of ${eDef.label}: ${eDef.propertyDefs}")
        }

        propertyValues.put(propDef, propValue)
        return this
    }


    /** 
     * Set the provided property pairs of properties and values, ex.
     * .setProperty(p1, v1, p2, v2).
     * @param args Pairs of property definitions and values
     * @return This object 
     */
    public T withProperties(Object... args) {
        if (args.size() == 1) {
            assert args[0] instanceof Map
            withProperties(args[0])
        }
        withProperties(args.toList())
    }


    /** 
     * Set all the properties specified in the provided map of property 
     * definitions to values ex. .setProperty([p1:v1, p2:v2]).
     * @param args Map of property definitions to property values
     * @return This object
     */
    public T withProperties(Map args) {
        assert this.respondsTo("getElementDef")
        ElementDefinition eDef = getElementDef()

        args.each { k, v ->
            def propertyDef = eDef.propertyDefs.find { it.name() == k }
            assert propertyDef : "Could not find property definition for ${k}."
            withProperty(propertyDef, v)
        }
        return this
    }


    /** 
     * Set all the properties in the provided list where the list elements
     * alternate between property definitions and property values ex.
     * .setProperty([p1, v1, p2, v2]).
     * @paran args A list of property definitions and values
     * @return This object
     */
    public T withProperties(List args) {
        def numArgs = args.size()
        assert numArgs >= 2
        assert numArgs % 2 == 0

        def pairs = args.collate(2)
        holdPropertyPairs(pairs)

        return this
    }


    /** 
     * Set the pairs of property definitions and property values when the value
     * is not null ex. .withNonNullProperties(p1, v1, p2, v2)
     * @param args Pairs of property definitions and values
     * @return This object 
     */
    public T withNonNullProperties(Object... args) {
        withNonNullProperties(args.toList())
    }


    /** 
     * Set the pairs of property definitions and property values when the value
     * is not null ex. .withNonNullProperties([p1, v1, p2, v2]).
     * @paran args A list of property definitions and values
     * @return This object
     */
    public T withNonNullProperties(List args) {
        def numArgs = args.size()
        assert numArgs >= 2
        assert numArgs % 2 == 0

        def pairs = args.collate(2).findAll({ p -> p[1] != null })
        holdPropertyPairs(pairs)

        return this
    }


    /** 
     * Matches a map of data against the properties of this element by name and
     * assignes the property value on match.
     * @param args A map of property definitions to values
     * @return This object
     */
    public T withMatchingProperties(Map args) {
        assert args != null
        assert this.respondsTo("getElementDef")
        ElementDefinition eDef = getElementDef()

        args.each { k, v ->
            def propertyDef = eDef.propertyDefs.find { it.name() == k }
            if (propertyDef) withProperty(propertyDef, v)
        }
        return this
    }


    /** 
     * Matches a map of data against the properties of this element by name and
     * assignes the property value on match ignoring data records where the
     * value is null.
     * @param args A map of property definitions to values
     * @return This object
     */
    public T withNonNullMatchingProperties(Map args) {
        assert args != null
        def nunNullArgs = args.findAll { k, v -> v != null }
        withMatchingProperties(nunNullArgs)
    }


    /** 
     * Utility method to split a list of pairs into property definition and
     * value and then call withProperty on each pair.
     * @param pairs A list of two element lists where each two element list
     * is a pair of property definition and property value.
     */
    private void holdPropertyPairs(List<List> pairs) {
        Map<PropertyDefinition,Object> props = new HashMap<PropertyDefinition,Object>()
        pairs.each { p ->
            def propDef = p[0]
            def propVal = p[1]
            props.put(propDef, propVal)
        }
        props.each { PropertyDefinition vp, Object val ->
            withProperty(vp, val)
        }
    }


    /** 
     * Returns a map of all property definitions and their value.
     * @return A map of property definition to value
     */
    public Map<PropertyDefinition,Object> allPropertyValues() {
        assert this.respondsTo("getElementDef")
        ElementDefinition eDef = getElementDef()

        def pvs = [:]
        pvs.putAll(propertyValues)

        eDef.defaultProperties.each { PropertyDefinition defVp ->
            def found = pvs.find({ vp, val -> vp.label == defVp.label})
            //log.debug "found: $found"
            
            if (!found) {
                pvs.put(defVp, defVp.defaultValue)                
            }
        }
        return pvs
    } 


    /** 
     * Set the properties of the provided graph element to the property values 
     * held by this object.
     * @param el The target graph element 
     */
    public Element setElementProperties(Element el) {
        def pvs = allPropertyValues()
        pvs.each { PropertyDefinition vp, Object val ->
            if (val instanceof org.codehaus.groovy.runtime.GStringImpl) val = val.toString()
            try {
                el.property(vp.label, val) 
            } catch (Exception e) {
                throw new RuntimeException(
                    "Could not set property ${vp} with value ${val} of element ${el} with definition " + Definition.lookup(el), 
                    e
                )
            }
        }
        el
    }

}
