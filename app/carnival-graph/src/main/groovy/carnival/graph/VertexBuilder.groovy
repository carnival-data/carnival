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
 * Builder class used when creating verticies from a VertexDefinition object.
 * 
 * @see carnival.graph.VertexDefinition
 */
@Slf4j
class VertexBuilder extends PropertyValuesHolder<VertexBuilder> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Object that owns this builder.
     * */
    VertexDefinition vertexDef


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public VertexBuilder(VertexDefinition vertexDef) {
        assert vertexDef
        this.vertexDef = vertexDef
    }


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public ElementDefinition getElementDef() { vertexDef }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String toString() {
        def str = "${vertexDef}"
        if (propertyValues.size() > 0) str += " ${propertyValues}"
        return str
    }


     /** 
     * If the vertex with the specified properties exists return it, otherwise create it.
     * Usage example, assuming VX.SHIBA_INU is an enum decorated with '@VertexModel':
     * <p>
     * {@code Vertex rover = VX.SHIBA_INU.instance().ensure(graph) }
     * 
     * @synonym carnival.graph.VertexBuilder.vertex(Graph graph, GraphTraversalSource g)
     * */
    public Vertex ensure(Graph graph, GraphTraversalSource g) {
        vertex(graph, g)
    }


     /** 
     * If the vertex with the specified properties exists return it, otherwise create it. 
     * Usage example, assuming VX.SHIBA_INU is an enum decorated with '@VertexModel':
     * <p>
     * {@code Vertex rover = VX.SHIBA_INU.instance().vertex(graph) }
     *
     * @synonym carnival.graph.VertexBuilder.ensure(Graph graph, GraphTraversalSource g)
     * */
    public Vertex vertex(Graph graph, GraphTraversalSource g) {
        assert graph
        assert g

        def traversal = traversal(graph, g)

        def pvs = allPropertyValues()
        pvs.each { PropertyDefinition vp, Object val -> 
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


    /** 
     * Create a vertex with the specified properties.
     * Usage example, assuming VX.SHIBA_INU is an enum decorated with '@VertexModel':
     * <p>
     * {@code Vertex rover = VX.SHIBA_INU.instance().create(graph) }
     * 
     * @synonym carnival.graph.VertexBuilder.createVertex(Graph graph)
     * */
    public Vertex create(Graph graph) {
        createVertex(graph)
    }



    /** 
     * Create a vertex with the specified properties.
     * Usage example, assuming VX.SHIBA_INU is an enum decorated with '@VertexModel':
     * <p>
     * {@code Vertex rover = VX.SHIBA_INU.instance().create(graph) }
     * 
     * @synonym carnival.graph.VertexBuilder.create(Graph graph) 
     * */
    public Vertex createVertex(Graph graph) {
        log.trace "VertexBuilder.createVertex graph:${graph}"
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
        if (vertexDef.isGlobal()) v.property(Base.PX.VERTEX_DEFINITION_CLASS.label, vertexDef.elementDefinitionClass)
        if (vertexDef.instanceOf != null) {
            assert vertexDef.instanceOf.vertex
            Base.EX.IS_INSTANCE_OF.instance().from(v).to(vertexDef.instanceOf.vertex).create()
        }

        setElementProperties(v)

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
