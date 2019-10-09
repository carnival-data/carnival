package carnival.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge



/** */
trait EdgeDefTrait {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    List<VertexDefTrait> domain = new ArrayList<VertexDefTrait>()

    /** */
    List<VertexDefTrait> range = new ArrayList<VertexDefTrait>()

    /** additional constraints, just represent as a string for now */
    String constraint

    /** */
    boolean global = false


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String getLabel() {
        name().toLowerCase()
    }


    /** */
    public boolean isGlobal() {
        return global
    }


    /** */
    public String getNameSpace() {
        //log.debug "\n\ngetNameSpace ${this} ${this.metaClass} ${this.metaClass.theClass} ${this.metaClass.theClass.name} \n\n\n"

        if (this instanceof Enum) return "${this.declaringClass.name}"
        else return "${this.metaClass.theClass.name}"
    }


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
        if (this.domain.size() > 0) {
            if (this.domain.contains(fromDef)) return
            if (this.domain.find({ it.label == fromDef.label && fromDef.isGlobal() })) return
            throw new RuntimeException("The 'from' vertex is not found in the domain of this relation -- relation:${this} relationDomain:${this.domain} fromVertexDef:${fromDef}")
        }
    }


    /** */
    public void assertDomain(Vertex from) {
        def fromDef = VertexDef.lookup(from)
        assertDomain(fromDef)
    }


    /** */
    public void assertRange(VertexDefTrait toDef) {
        if (this.range.size() > 0) {
            if (this.range.contains(toDef)) return
            if (this.range.find({ it.label == toDef.label && toDef.isGlobal() })) return
            throw new RuntimeException("The 'to' vertex is not found in the range of this relation -- relation:${this} relationDomain:${this.range} toVertexDef:${toDef}")
        }
    }


    /** */
    public void assertRange(Vertex to) {
        def toDef = VertexDef.lookup(to)
        assertRange(toDef)        
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
        def lbl = getLabel()
        def ns = getNameSpace()
        from.addEdge(
            lbl, to,
            Base.PX.NAME_SPACE.label, ns
        )
    }


}