package carnival.graph



import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge



/** 
 * Defines allowed edges in a graph model, automatically inherited by 
 * enums with the `@EdgeModel` annotation.
 * 
 * 
 * @see carnival.graph.EdgeModel
 * @see carnival.graph.EdgeBuilder
 */
@Slf4j
trait EdgeDefTrait extends ElementDefTrait {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    List<VertexDefTrait> domain = new ArrayList<VertexDefTrait>()

    /** */
    List<VertexDefTrait> range = new ArrayList<VertexDefTrait>()


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS / SETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** Setter wrapper for propertyDefs */
    List<PropertyDefTrait> getEdgeProperties() { propertyDefs }
    
    /** Getter wrapper for propertyDefs */
    void setEdgeProperties(ArrayList<PropertyDefTrait> propertyDefs) {
        assert propertyDefs != null
        propertyDefs = propertyDefs
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String getLabel() {
        name().toLowerCase()
    }


    ///////////////////////////////////////////////////////////////////////////
    // TYPE CHECKING
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public boolean isa(Edge e) {
        assert e != null
        (e.label() == getLabel() && Base.PX.NAME_SPACE._valueOf(e) == getNameSpace())   
    }

    /** */
    public boolean isa(Vertex v) {
        assert v != null
        return false
    }


    ///////////////////////////////////////////////////////////////////////////
    // DOMAIN / RANGE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public List<String> getDomainLabels() {
        if (domain == null) return null
        else return domain*.label
    }


    /** */
    public List<String> getRangeLabels() {
        if (range == null) return null
        else return range*.label
    }


    /** */
    public void assertDomain(VertexDefTrait fromDef) {
        assert fromDef != null
        if (this.domain.size() > 0) {
            if (this.domain.contains(fromDef)) return
            if (fromDef.isGlobal() && this.domain.find({ it.label == fromDef.label })) return
            if (this.domain.find({ it.label == fromDef.label && it.isGlobal() })) return
            throw new EdgeDomainException("The 'from' vertex is not found in the domain of this relation -- relation:${this} relationDomain:${this.domain} fromVertexDef:${fromDef}")
        }
    }


    /** */
    public void assertDomain(Vertex from) {
        assert from != null
        def fromDef = ElementDefinition.lookup(from)
        assertDomain(fromDef)
    }


    /** */
    public void assertRange(VertexDefTrait toDef) {
        assert toDef != null
        if (this.range.size() > 0) {
            if (this.range.contains(toDef)) return
            if (toDef.isGlobal() && this.range.find({ it.label == toDef.label })) return
            if (this.range.find({ it.label == toDef.label && it.isGlobal() })) return
            throw new EdgeRangeException("The 'to' vertex is not found in the range of this relation -- relation:${this} relationDomain:${this.range} toVertexDef:${toDef}")
        }
    }


    /** */
    public void assertRange(Vertex to) {
        assert to != null
        def toDef = ElementDefinition.lookup(to)
        assertRange(toDef)        
    }


    ///////////////////////////////////////////////////////////////////////////
    // Builders
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public EdgeBuilder instance() {
        def ci = new EdgeBuilder(this)
        ci.propertiesMustBeDefined = this.propertiesMustBeDefined
        return ci
    }


    ///////////////////////////////////////////////////////////////////////////
    // VertexDef -> VertexDef
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Edge relate(GraphTraversalSource g, VertexDefTrait from, VertexDefTrait to) {
        setRelationship(g, from, to)
    }


    /** */
    public Edge setRelationship(GraphTraversalSource g, VertexDefTrait from, VertexDefTrait to) {
        assert g != null
        assert from != null
        assert to != null

        assertDomain(from)
        assertRange(to)

        def lbl = getLabel()
        def ns = getNameSpace()
        g.V(from.vertex)
            .outE(lbl).as('r')
            .has(Base.PX.NAME_SPACE.label, ns)
            .inV().is(to.vertex)
            .select('r')
        .tryNext().orElseGet {
            from.vertex.addEdge(
                lbl, to.vertex,
                Base.PX.NAME_SPACE.label, ns
            )
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Vertex -> VertexDef
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Edge relate(GraphTraversalSource g, Vertex from, VertexDefTrait to) {
        setRelationship(g, from, to)
    }


    /** */
    public Edge setRelationship(GraphTraversalSource g, Vertex from, VertexDefTrait to) {
        assert g != null
        assert from != null
        assert to != null

        assertDomain(from)
        assertRange(to)

        def lbl = getLabel()
        def ns = getNameSpace()
        g.V(from)
            .outE(lbl).as('r')
            .has(Base.PX.NAME_SPACE.label, ns)
            .inV().is(to.vertex)
            .select('r')
        .tryNext().orElseGet {
            from.addEdge(
                lbl, to.vertex,
                Base.PX.NAME_SPACE.label, ns
            )
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Vertex -> Vertex
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Edge relate(GraphTraversalSource g, Vertex from, Vertex to) {
        setRelationship(g, from, to)
    }


    /** */
    public Edge setRelationship(GraphTraversalSource g, Vertex from, Vertex to) {
        assert g != null
        assert from != null
        assert to != null

        assertDomain(from)
        assertRange(to)

        def lbl = getLabel()
        def ns = getNameSpace()
        g.V(from)
            .outE(lbl).as('r')
            .has(Base.PX.NAME_SPACE.label, ns)
            .inV().is(to)
            .select('r')
        .tryNext().orElseGet {
            from.addEdge(
                lbl, to,
                Base.PX.NAME_SPACE.label, ns
            )
        }
    }


    /** */
    public Edge addEdge(Vertex from, Vertex to) {
        assert from != null
        assert to != null

        def lbl = getLabel()
        def ns = getNameSpace()
        from.addEdge(
            lbl, to,
            Base.PX.NAME_SPACE.label, ns
        )
    }


}