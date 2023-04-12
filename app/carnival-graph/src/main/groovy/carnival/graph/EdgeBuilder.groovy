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
 * Builder class used when creating edges from an EdgeDefinition object.
 */
@Slf4j
class EdgeBuilder extends PropertyValuesHolder<EdgeBuilder> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * The edge definition that will be used to create edges.
     */
    EdgeDefinition edgeDef

    /** The from or out vertex for created edges */
    Vertex fromVertex

    /** The to or in vertex for created edges */
    Vertex toVertex


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Constructor that takes the edge definition as an argument.
     * @param edgeDef The edge definition this builder will use.
     */
    public EdgeBuilder(EdgeDefinition edgeDef) {
        assert edgeDef
        this.edgeDef = edgeDef
    }


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return the element element definition of this builder.
     * @return The ElementDefinition
     */
    public ElementDefinition getElementDef() { edgeDef }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Convenience string representation of this object.
     * @return A string representation of this object
     */
    public String toString() {
        def str = "${edgeDef}"
        if (propertyValues.size() > 0) str += " ${propertyValues}"
        return str
    }

    /** 
     * Assert that the properties required by the edge definition have been
     * set.
     */
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

    /** 
     * Sets the from vertex for created edges.
     * @param v The from vertex
     * @return This object
     */
    public EdgeBuilder from(Vertex v) {
        assert v != null
        edgeDef.assertDomain(v)
        fromVertex = v
        this
    }


    /** 
     * Sets the to vertex for created edges.
     * @param v The to vertex
     * @return This object
     */
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
