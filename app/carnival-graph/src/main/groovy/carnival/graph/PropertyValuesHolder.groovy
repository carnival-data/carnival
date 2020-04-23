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
 *
 *
 */
@Slf4j
class PropertyValuesHolder<T> {

    /** */
    Map<PropertyDefTrait,Object> propertyValues = new HashMap<PropertyDefTrait,Object>()


    /** */
    public T withProperty(PropertyDefTrait propDef, Enum propValue) {
        assert propDef != null
        assert propValue != null
        withProperty(propDef, propValue.name())
    }


    /** */
    public T withProperty(PropertyDefTrait propDef, Object propValue) {
        assert this.respondsTo("getElementDef")
        WithPropertyDefsTrait eDef = getElementDef()

        boolean found = eDef.propertyDefs.find({it.label == propDef.label})
        if (!found) throw new IllegalArgumentException("${propDef} is not a property of ${eDef.label}: ${eDef.propertyDefs}")

        propertyValues.put(propDef, propValue)
        return this
    }


    /** */
    public T withProperties(Object... args) {
        withProperties(args.toList())
    }


    /** */
    public T withProperties(List args) {
        def numArgs = args.size()
        assert numArgs >= 2
        assert numArgs % 2 == 0

        Map<PropertyDefTrait,Object> props = new HashMap<PropertyDefTrait,Object>()
        def i = 0
        while (i < numArgs) {
            def propDef = args[i++]
            def propVal = args[i++]
            props.put(propDef, propVal)
        }    
        //log.trace "withProperties props: $props"

        props.each { PropertyDefTrait vp, Object val ->
            withProperty(vp, val)
        }
        return this
    }


    /** */
    public Map<PropertyDefTrait,Object> allPropertyValues() {
        assert this.respondsTo("getElementDef")
        WithPropertyDefsTrait eDef = getElementDef()

        def pvs = [:]
        pvs.putAll(propertyValues)

        eDef.defaultProperties.each { PropertyDefTrait defVp ->
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
        pvs.each { PropertyDefTrait vp, Object val ->
            el.property(vp.label, val) 
        }
        el
    }

}
