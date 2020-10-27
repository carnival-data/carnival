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
 *
 *
 */
@Slf4j
trait VertexDefTrait extends WithPropertyDefsTrait {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public static final String GLOBAL = 'GlobalVertexDefinition'

    /** */
    public static final String CLASS_SUFFIX = '_class'

    /** */
    public static final String NAME_SEPARATOR = '_'



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * The singleton vertex, if applicable, for this vertex definition. 
     * Singleton vertices are valuable so they can be used in gremlin traversals.
     */
    Vertex vertex
    
    /** */
    boolean global = false

    /** */
    VertexDefTrait superClass

    /** */
    Boolean propertiesMustBeDefined = true


    ///////////////////////////////////////////////////////////////////////////
    // GETTERS / SETTERS
    ///////////////////////////////////////////////////////////////////////////

    /** Setter wrapper for propertyDefs */
    List<PropertyDefTrait> getVertexProperties() { this.propertyDefs }
    
    /** Getter wrapper for propertyDefs */
    void setVertexProperties(List<PropertyDefTrait> propertyDefs) {
        this.propertyDefs = propertyDefs
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
    public boolean isClass() {
        name().toLowerCase().endsWith(CLASS_SUFFIX)
    }


    /** */
    public boolean isGlobal() {
        return global
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
    public String getNameSpace() {
        if (this.global) return GLOBAL
        return getVertexDefinitionClass()
    }


    /** */
    public String getVertexDefinitionClass() {
        if (this instanceof Enum) return "${this.declaringClass.name}"
        return "${this.metaClass.theClass.name}"
    }


    /** */
    public ControlledInstance instance() {
        controlledInstance()
    }


    /** */
    public ControlledInstance controlledInstance() {
        def ci = new ControlledInstance(this)
        ci.propertiesMustBeDefined = this.propertiesMustBeDefined
        ci
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
        if (isGlobal()) v.property(Base.PX.VERTEX_DEFINITION_CLASS.label, getVertexDefinitionClass())

        //log.trace "createVertex ${v} ${v.label()}"
        return v
    }



    ///////////////////////////////////////////////////////////////////////////
    // SIMPLE VALIDATION
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public boolean isa(Vertex v) {
        assert v != null
        (v.label() == getLabel() && Base.PX.NAME_SPACE.valueOf(v) == getNameSpace())
    }


    ///////////////////////////////////////////////////////////////////////////
    // KNOWLEDGE GRAPH METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     *
     *
     */
    public void setSubclassOf(GraphTraversalSource g, VertexDefTrait superClassDef) {
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
    public void setRelationship(GraphTraversalSource g, EdgeDefTrait rel, VertexDefTrait targetClassDef) {
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


/** */
/*trait GlobalVertexDefTrait extends VertexDefTrait {

    public static final String GLOBAL = "GLOBAL"

    @Override
    public String getNameSpace() { return GLOBAL }

}*/






