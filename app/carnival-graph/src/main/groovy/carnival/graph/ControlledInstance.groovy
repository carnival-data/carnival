package carnival.graph



import groovy.transform.ToString

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge



/** 
 *
 *
 */
class ControlledInstance extends PropertyValuesHolder<ControlledInstance> {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    VertexDefTrait vertexDef


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public ControlledInstance(VertexDefTrait vertexDef) {
        assert vertexDef
        this.vertexDef = vertexDef
    }


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public WithPropertyDefsTrait getElementDef() { vertexDef }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String toString() {
        def str = "${vertexDef}"
        if (propertyValues.size() > 0) str += " ${propertyValues}"
        return str
    }

    /*
    public ControlledInstance withProperty(PropertyDefTrait propDef, Object propValue) {
        boolean found = vertexDef.vertexProperties.find({it.label == propDef.label})
        if (!found) throw new IllegalArgumentException("${propDef} is not a property of ${vertexDef.label}: ${vertexDef.vertexProperties}")

        propertyValues.put(propDef, propValue)
        return this
    }


    public ControlledInstance withProperties(Object... args) {
        withProperties(args.toList())
    }


    public ControlledInstance withProperties(List args) {
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
            this.withProperty(vp, val)
        }
        return this
    }


    public Map<PropertyDefTrait,Object> allPropertyValues() {
        def pvs = [:]
        pvs.putAll(propertyValues)
        vertexDef.defaultProperties.each { PropertyDefTrait defVp ->
            def found = pvs.find({ vp, val -> vp.label == defVp.label})
            //log.debug "found: $found"
            
            if (!found) {
                pvs.put(defVp, defVp.defaultValue)                
            }
        }
        return pvs
    } 
    */


    /** */
    public Vertex vertex(Graph graph, GraphTraversalSource g) {
        assert graph
        assert g

        def traversal = traversal(graph, g)

        def pvs = allPropertyValues()
        pvs.each { PropertyDefTrait vp, Object val -> 
            traversal.has(vp.label, val) 
        }

        def vertex = traversal.tryNext().orElseGet {
            createVertex(graph, g)
        }

        return vertex
    }


    /** */
    public Traversal traversal(Graph graph, GraphTraversalSource g) {
        assert graph
        assert g

        assertRequiredProperties()

        boolean isClass = vertexDef.isClass()
        def lbl = vertexDef.getLabel()
        def ns = vertexDef.getNameSpace()
        assert ns != null
        
        def traversal = g.V().hasLabel(lbl)
        if (isClass) traversal.has(Base.PX.IS_CLASS.label, isClass)
        traversal.has(Base.PX.NAME_SPACE.label, ns)

        traversal
    }


    /** */
    public Vertex createVertex(Graph graph, GraphTraversalSource g) {
        assert graph
        assert g

        assertRequiredProperties()

        def lbl = vertexDef.getLabel()
        def ns = vertexDef.getNameSpace()
        assert ns != null

        def v = graph.addVertex(
            T.label, lbl,
            Base.PX.NAME_SPACE.label, ns
        )
        
        if (vertexDef.isClass()) v.property(Base.PX.IS_CLASS.label, vertexDef.isClass())
        if (vertexDef.isGlobal()) v.property(Base.PX.VERTEX_DEFINITION_CLASS.label, vertexDef.vertexDefinitionClass)

        setElementProperties(v)

        //log.debug "added vertex $v with label ${v.label()} and props $propertyValues"
        return v
    }


    /** */
    void assertRequiredProperties() {
        vertexDef.requiredProperties.each { requiredPropDef ->
            boolean found = allPropertyValues().find { k, v ->
                k.label == requiredPropDef.label
            }
            if (!found) throw new RuntimeException("required property ${requiredPropDef} of ${vertexDef} not found in ${propertyValues}")
        }
    }

}
