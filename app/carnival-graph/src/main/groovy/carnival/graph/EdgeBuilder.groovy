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
 * Builder class used when creating edges from an EdgeDefTrait object.
 */
@Slf4j
class EdgeBuilder extends PropertyValuesHolder<EdgeBuilder> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Object that owns this builder.
     * */
    EdgeDefTrait edgeDef

    /** */
    Vertex fromVertex

    /** */
    Vertex toVertex


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public EdgeBuilder(EdgeDefTrait edgeDef) {
        assert edgeDef
        this.edgeDef = edgeDef
    }


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public ElementDefTrait getElementDef() { edgeDef }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String toString() {
        def str = "${edgeDef}"
        if (propertyValues.size() > 0) str += " ${propertyValues}"
        return str
    }

    /** */
    void assertRequiredProperties() {
        edgeDef.requiredProperties.each { requiredPropDef ->
            boolean found = allPropertyValues().find { k, v ->
                k.label == requiredPropDef.label
            }
            if (!found) throw new RequiredPropertyException("required property ${requiredPropDef} of ${edgeDef} not found in ${propertyValues}")
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public EdgeBuilder from(Vertex v) {
        assert v != null
        edgeDef.assertDomain(v)
        fromVertex = v
        this
    }


    /** */
    public EdgeBuilder to(Vertex v) {
        edgeDef.assertRange(v)
        toVertex = v
        this
    }


    /** 
     * Create new edge using previously specified 'from' and 'to' Vertex objects.
     * Usage example, assuming EX.IS_FIRENDS_WITH is an enum decorated with '@EdgeModel':
     * <p>
     * {@code Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).create() }
     * */
    public Edge create() {
        assert fromVertex
        assert toVertex
        assertRequiredProperties()
        def e = edgeDef.addEdge(fromVertex, toVertex)
        setElementProperties(e)
    }


    /** 
     * If the edge with the specified 'from', 'to', and properties exists return it, otherwise create it.
     * Usage example, assuming EX.IS_FIRENDS_WITH is an enum decorated with '@EdgeModel':
     * <p>
     * {@code Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).ensure() }
     * */
    public Edge ensure(GraphTraversalSource g) {
        assert g
        assert fromVertex
        assert toVertex
        assertRequiredProperties()
        def e = edgeDef.relate(g, fromVertex, toVertex)
        setElementProperties(e)
    }

}
