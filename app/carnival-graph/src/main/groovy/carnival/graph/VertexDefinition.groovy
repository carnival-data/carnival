package carnival.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.VertexProperty

import carnival.util.StringUtils
import carnival.graph.Base




/** 
 * Defines allowed verticies in a graph model, automatically inherited by 
 * enums with the `@VertexModel` annotation.
 * 
 * @see carnival.graph.VertexModel
 */
@Slf4j
trait VertexDefinition extends ElementDefinition {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** The default suffix for vertex labels representing classes */
    public static final String CLASS_SUFFIX = '_class'

    /** The default separator for components of a name */
    public static final String NAME_SEPARATOR = '_'



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * The singleton vertex, if applicable, for this vertex definition. 
     * Singleton vertices are valuable so they can be used in gremlin traversals.
     */
    Vertex vertex
    
    /** 
     * optional, defines what the superclass of this class is.
     */
    VertexDefinition superClass

    /** 
     * optional, defines what class these verticies are instances of.
     */
    VertexDefinition instanceOf

    /** 
     * Explicitly designate this definition as a class. A singleton vertex will
     * automatically be created in the graph.
     */
    Boolean isClass = null


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS / SETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** Setter wrapper for propertyDefs */
    Set<PropertyDefinition> getVertexProperties() { this.propertyDefs }
    
    /** Getter wrapper for propertyDefs */
    void setVertexProperties(Set<PropertyDefinition> propertyDefs) {
        this.propertyDefs = propertyDefs
    }

    /** */
    VertexDefinition getSuperClass() { this.superClass }

    /** */
    void setSuperClass(VertexDefinition vDef) {
        assert vDef != null
        if (!isClass()) throw new RuntimeException("cannot set superClass when isClass() is false")
        
        this.superClass = vDef
    }

    /** */
    VertexDefinition getInstanceOf() { this.instanceOf }

    /** */
    void setInstanceOf(VertexDefinition vDef) {
        assert vDef != null
        if (isClass()) throw new RuntimeException("cannot set instanceOf when isClass() is true")
        
        this.instanceOf = vDef
    }



    ///////////////////////////////////////////////////////////////////////////
    // PROPERTY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String getLabel() {
        def n = name()
        def chunks = n.split(NAME_SEPARATOR)
        def str = ""
        chunks.each { str += it.toLowerCase().capitalize() }
        return str
    }


    /** */
    public boolean hasLabel(Vertex v) {
        assert v != null
        return v.label() == getLabel()
    }


    /** */
    boolean getIsClass() {
        isClass()        
    }


    /** */
    public boolean isClass() {
        if (this.isClass != null) return this.isClass
        name().toLowerCase().endsWith(CLASS_SUFFIX)
    }


    /** */
    public Set<VertexProperty> definedPropertiesOf(Vertex v) {
        Set<VertexProperty> vProps = new HashSet<VertexProperty>()
        propertyDefs.each { pDef ->
            def vp = v.property(pDef.label)
            if (VertexProperty.empty().equals(vp)) return
            if (!vp.isPresent()) return
            vProps.add(vp)
        }
        vProps
    }



    ///////////////////////////////////////////////////////////////////////////
    // SINGLETON VERTEX METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public VertexBuilder instance() {
        def vb = new VertexBuilder(this)
        vb.propertiesMustBeDefined = this.propertiesMustBeDefined
        vb
    }


    /** */
    public Vertex createVertex(Graph graph) {
        assert graph
        if (isClass()) throw new RuntimeException("cannot create instance vertex of class ${this}")
        def lbl = getLabel()
        def ns = getNameSpace()
        def v = graph.addVertex(
            T.label, lbl,
            Base.PX.NAME_SPACE.label, ns
        )
        if (isGlobal()) v.property(Base.PX.VERTEX_DEFINITION_CLASS.label, getVertexModelClass())
        
        //if (instanceOf != null) Base.EX.IS_INSTANCE_OF.instance().from(v).to(instanceOf).create()

        //log.trace "createVertex ${v} ${v.label()}"
        return v
    }



    ///////////////////////////////////////////////////////////////////////////
    // TYPE CHECKING
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public boolean isa(Edge e) {
        assert e != null
        return false
    }

    /** */
    public boolean isa(Vertex v) {
        assert v != null
        (v.label() == getLabel() && Base.PX.NAME_SPACE._valueOf(v) == getNameSpace())
    }


    ///////////////////////////////////////////////////////////////////////////
    // KNOWLEDGE GRAPH METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public void applyTo(Graph graph, GraphTraversalSource g) {
        if (this.isClass() && this.requiredProperties.size() == 0) {
            this.vertex = this.instance().ensure(graph, g)
        }

		if (this.superClass) {
			assert this.isClass()
			assert this.superClass.isClass()
			assert this.vertex
			assert this.superClass.vertex
			this.setSubclassOf(g, this.superClass)
		}
    }


    /** 
     *
     *
     */
    public void setSubclassOf(GraphTraversalSource g, VertexDefinition superClassDef) {
        assert g
        assert superClassDef
        if (superClassDef.vertex == null) throw new IllegalArgumentException("superClassDef.vertex is null: $superClassDef")
        if (!this.vertex) throw new RuntimeException("vertex is null: $this")
        g.V(this.vertex)
            .out(Base.EX.IS_SUBCLASS_OF.label)
            .is(superClassDef.vertex)
        .tryNext().orElseGet {
            Base.EX.IS_SUBCLASS_OF.relate(g, vertex, superClassDef.vertex)
        }
    }


    /** 
     *
     *
     */
    public void setSuperclassOf(GraphTraversalSource g, Vertex subclassV) {
        assert g
        assert subclassV
        if (!this.vertex) throw new RuntimeException("vertex is null: $this")
        g.V(this.vertex)
            .in(Base.EX.IS_SUBCLASS_OF.label)
            .is(subclassV)
            .tryNext().orElseGet {
                Base.EX.IS_SUBCLASS_OF.relate(g, subclassV, this.vertex)
        }
    }


    /** 
     *
     *
     */
    public void setRelationship(GraphTraversalSource g, EdgeDefinition rel, VertexDefinition targetClassDef) {
        //log.debug "setRelationship rel:$rel"
        g.V(vertex)
            .outE(rel.label).as('r')
            .inV()
            .is(targetClassDef.vertex)
            .select('r')
        .tryNext().orElseGet {
            vertex.addEdge(
                rel.label, targetClassDef.vertex, 
                Base.PX.NAME_SPACE.label, rel.nameSpace
            )
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    public String toString() {
        def str = "${name()} ${nameSpace}"
        if (vertexProperties) str += " vertexProperties:${vertexProperties}"
        return str
    }

}


