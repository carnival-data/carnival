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

    /** */
    Map<PropertyDefinition,Object> propertyValues = new HashMap<PropertyDefinition,Object>()

    /** */
    Boolean propertiesMustBeDefined = true


    /** */
    public T withProperty(PropertyDefinition propDef, Enum propValue) {
        assert propDef != null
        assert propValue != null
        withProperty(propDef, propValue.name())
    }


    /** */
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


    /** */
    public T withProperties(Object... args) {
        if (args.size() == 1) {
            assert args[0] instanceof Map
            withProperties(args[0])
        }
        withProperties(args.toList())
    }


    /** */
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


    /** */
    public T withProperties(List args) {
        def numArgs = args.size()
        assert numArgs >= 2
        assert numArgs % 2 == 0

        def pairs = args.collate(2)
        holdPropertyPairs(pairs)

        return this
    }


    /** */
    public T withNonNullProperties(Object... args) {
        withNonNullProperties(args.toList())
    }


    /** */
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
     *
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
     *
     */
    public T withNonNullMatchingProperties(Map args) {
        assert args != null
        def nunNullArgs = args.findAll { k, v -> v != null }
        withMatchingProperties(nunNullArgs)
    }


    /** */
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


    /** */
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


    /** */
    public Element setElementProperties(Element el) {
        def pvs = allPropertyValues()
        pvs.each { PropertyDefinition vp, Object val ->
            if (val instanceof org.codehaus.groovy.runtime.GStringImpl) val = val.toString()
            try {
                el.property(vp.label, val) 
            } catch (Exception e) {
                throw new RuntimeException(
                    "Could not set property ${vp} with value ${val} of element ${el} with definition " + ElementDef.lookup(el), 
                    e
                )
            }
        }
        el
    }

}
