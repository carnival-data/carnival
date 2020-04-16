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
class EdgeBuilder extends PropertyValuesHolder<EdgeBuilder> {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
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
    public WithPropertyDefsTrait getElementDef() { edgeDef }


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


    /**
    .from()
    .to()
    .create(g)
    */

    /** */
    public EdgeBuilder from(Vertex v) {
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


    /** */
    public Edge create() {
        assert fromVertex
        assert toVertex
        assertRequiredProperties()
        def e = edgeDef.addEdge(fromVertex, toVertex)
        setElementProperties(e)
    }


    /** */
    public Edge ensure(GraphTraversalSource g) {
        assert g
        assert fromVertex
        assert toVertex
        assertRequiredProperties()
        def e = edgeDef.relate(g, fromVertex, toVertex)
        setElementProperties(e)
    }

}
