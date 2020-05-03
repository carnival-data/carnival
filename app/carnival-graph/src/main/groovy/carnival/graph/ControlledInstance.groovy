package carnival.graph



import groovy.transform.ToString

import groovy.util.logging.Slf4j

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
@Slf4j
class ControlledInstance extends PropertyValuesHolder<ControlledInstance> {

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


    /** */
    public Vertex ensure(Graph graph, GraphTraversalSource g) {
        vertex(graph, g)
    }


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
            createVertex(graph)
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
    public Vertex create(Graph graph) {
        createVertex(graph)
    }



    /** */
    public Vertex createVertex(Graph graph) {
        assert graph

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
