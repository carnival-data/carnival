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
trait EdgeDefinition extends ElementDefinition {

    //////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    static enum Multiplicity {
        MULTI, SIMPLE, MANY2ONE, ONE2MANY, ONE2ONE
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** The set of permitted vertex definitions for from vertices */
    List<VertexDefinition> domain = new ArrayList<VertexDefinition>()

    /** The set of permitted vertex definitions for to vertices */
    List<VertexDefinition> range = new ArrayList<VertexDefinition>()

    /** */
    Multiplicity multiplicity = Multiplicity.MULTI


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS / SETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Getter wrapper for propertyDefs 
     * @return A set of property definitions
     */
    Set<PropertyDefinition> getEdgeProperties() { this.propertyDefs }
    
    /** 
     * Setter wrapper for propertyDefs 
     * @param A set of proeprty definitions
     */
    void setEdgeProperties(Set<PropertyDefinition> propertyDefs) {
        assert propertyDefs != null
        this.propertyDefs = propertyDefs
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return the label to use for instantiated edges.
     * @return The label as a string
     */
    /*public String getLabel() {
        name().toLowerCase()
    }*/


    ///////////////////////////////////////////////////////////////////////////
    // TYPE CHECKING
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Returns true if this definition applies to the provided edge.
     * @param e The edge object to test
     */
    public boolean isa(Edge e) {
        assert e != null
        (e.label() == getLabel() && Base.PX.NAME_SPACE._valueOf(e) == getNameSpace())   
    }

    /** 
     * Implementation of isa(Vertex) that will always return false since this
     * object defines edges, not vertices.
     * @param v The vertex to test
     */
    public boolean isa(Vertex v) {
        assert v != null
        return false
    }


    ///////////////////////////////////////////////////////////////////////////
    // DOMAIN / RANGE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return the list of labels of the domain vertex definitions.
     * @return The list of labels as a String
     */
    public List<String> getDomainLabels() {
        if (domain == null) return null
        else return domain*.label
    }


    /** 
     * Return the list of labels of the range vertex definitions.
     * @return The list of labels as a String
     */
    public List<String> getRangeLabels() {
        if (range == null) return null
        else return range*.label
    }


    /** 
     * Assert that the provided vertex definition is included in the set of 
     * permissible domain vertex definitions of this edge definition.
     * @param fromDef The vertex definition to test
     */
    public void assertDomain(VertexDefinition fromDef) {
        assert fromDef != null
        if (this.domain.size() > 0) {
            if (this.domain.contains(fromDef)) return
            if (fromDef.isGlobal() && this.domain.find({ it.label == fromDef.label })) return
            if (this.domain.find({ it.label == fromDef.label && it.isGlobal() })) return
            throw new EdgeDomainException("The 'from' vertex is not found in the domain of this relation -- relation:${this} relationDomain:${this.domain} fromVertexDef:${fromDef}")
        }
    }


    /** 
     * Assert that the definition of the provided vertex is included in the 
     * domain of this edge definition.
     * @param from The vertex to test
     */
    public void assertDomain(Vertex from) {
        assert from != null
        def fromDef = Definition.lookup(from)
        assertDomain(fromDef)
    }


    /** 
     * Assert that the provided vertex definition is included in the range of
     * this edge definition.
     * @param toDef The vertex definition to test
     */
    public void assertRange(VertexDefinition toDef) {
        assert toDef != null

        /*println "--- toDef: ${toDef} ${toDef.label}"
        println "--- range: ${this.range}"
        range.each {
            println "${it}: ${it.label}"
        }*/

        if (this.range.size() > 0) {
            if (this.range.contains(toDef)) return
            if (toDef.isGlobal() && this.range.find({ it.label == toDef.label })) return
            if (this.range.find({ it.label == toDef.label && it.isGlobal() })) return
            throw new EdgeRangeException("The 'to' vertex is not found in the range of this relation -- relation:${this} relationDomain:${this.range} toVertexDef:${toDef}")
        }
    }


    /** 
     * Assert that the definition that applies to the provided vertex is
     * included in the range of this edge definition.
     * @param to The vertex to test
     */
    public void assertRange(Vertex to) {
        assert to != null
        def toDef = Definition.lookup(to)
        assertRange(toDef)        
    }


    ///////////////////////////////////////////////////////////////////////////
    // Builders
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return an edge builder that can be used to instantiate an edge using
     * this edge definition.
     * @return An edge builder
     */
    public EdgeBuilder instance() {
        def ci = new EdgeBuilder(this)
        ci.propertiesMustBeDefined = this.propertiesMustBeDefined
        return ci
    }


    ///////////////////////////////////////////////////////////////////////////
    // VertexDef -> VertexDef
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Synonym for setRelationship().
     * @see #setRelationship(GraphTraversalSource, VertexDefinition, VertexDefinition)
     */
    public Edge relate(GraphTraversalSource g, VertexDefinition from, VertexDefinition to) {
        setRelationship(g, from, to)
    }


    /** 
     * Create an edge based on this edge definition between the singleton
     * vertices of the provided from and to vertex definitions if the edge is
     * not already present. The from and to vertex definitions must have 
     * singleton vertices defined, which is the case for all vertices that
     * represent classes of things.
     * @param g A graph traversal source to use
     * @param from The from vertex definition
     * @param to The to vertex definition
     * @return The new or existing edge
     */
    public Edge setRelationship(GraphTraversalSource g, VertexDefinition from, VertexDefinition to) {
        assert g != null
        assert from != null
        assert to != null

        assertDomain(from)
        assertRange(to)
        assert from.vertex
        assert to.vertex

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

    /** 
     * Synonym for setRelationship().
     * @see #setRelationship(GraphTraversalSource, Vertex, VertexDefinition)
     */
    public Edge relate(GraphTraversalSource g, Vertex from, VertexDefinition to) {
        setRelationship(g, from, to)
    }


    /** 
     * Create an edge between the provided from vertex and the singleton vertex
     * of the provided to vertex definition if the edge is not already created.
     * @param g A graph traversal source to use
     * @param from The from vertex
     * @param to The to vertex definition that must have a singleton vertex
     * @return The new or existing edge
     */
    public Edge setRelationship(GraphTraversalSource g, Vertex from, VertexDefinition to) {
        assert g != null
        assert from != null
        assert to != null

        assertDomain(from)
        assertRange(to)
        assert to.vertex

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

    /** 
     * Synonym for setRelationship().
     * @see #setRelationship(GraphTraversalSource, Vertex, Vertex)
     * @return The new or existing edge
     */
    public Edge relate(GraphTraversalSource g, Vertex from, Vertex to) {
        setRelationship(g, from, to)
    }


    /** 
     * Create an edge between the from and to vertices if the edge is not
     * already present.
     * @param to The to vertex
     * @param from The from vertex
     * @return The new or existing edge
     */
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


    /** 
     * Add an edge between the from and to vertices.
     * @param from The from vertex
     * @param to The to vertex
     */
    public Edge addEdge(Vertex from, Vertex to) {
        assert from != null
        assert to != null

        assertDomain(from)
        assertRange(to)

        def lbl = getLabel()
        def ns = getNameSpace()
        from.addEdge(
            lbl, to,
            Base.PX.NAME_SPACE.label, ns
        )
    }

}