package carnival.graph



import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.structure.Property



/** 
 * Defines allowed properties in a graph model, automatically inherited by 
 * enums with the `@PropertyDefinition` annotation.
 * 
 * @see carnival.graph.PropertyDefinition
 */
@Slf4j
trait PropertyDefTrait {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Set by #withDefault()
    */
    Object defaultValue = null

    /**
     * If true, then the values of this property must be unique across all
     * elements. Set by #withConstraints()
     */
    Boolean unique = false

    /**
     * If true, then this property must be present in all elements. Set 
     * by #withConstraints()
     */
    Boolean required = false

    /**
     * If true, then this property should be indexed by the underlying database
     * system. Set by #withConstraints()
     */
    Boolean index = false


	///////////////////////////////////////////////////////////////////////////
	// FACTORY METHODS
	///////////////////////////////////////////////////////////////////////////

	/** 
     * Used to set constraints for properties when they are added to vertex or edge definitions.
     * 
     * @param m 
     * @param m["unique"] <Boolean>   if true, then the values of this property must be unique across all elements.
     * @param m["required"] <Boolean> if true, then this property must be present in all elements.
     * @param m["index"] <Boolean>    if true, then this property should be indexed by the underlying database
     * system.
     * 
     * 
     * */
	public PropertyDefTrait withConstraints(Map m) {
        def newObj = new PropertyDefTraitHolder(this)

		if (m.unique) newObj.unique = m.unique
		if (m.required) newObj.required = m.required
		if (m.index) newObj.index = m.index

		return newObj
	}


	/** */
	public PropertyDefTrait constraints(Map m) {
        withConstraints(m)
    }


    /** */
    public PropertyDefTrait defaultValue(Object o) {
        def newObj = new PropertyDefTraitHolder(this)
        newObj.defaultValue = o
        return newObj
    }


	///////////////////////////////////////////////////////////////////////////
	// PROPERTY METHODS
	///////////////////////////////////////////////////////////////////////////

    /** */
    public Property of(Element el) {
        assert el
        el.property(getLabel())
    }


    /** */
    public Object valueOf(Element el) {
        assert el
        el.property(getLabel()).isPresent() ? el.value(getLabel()) : null
    }


    /** */
    public PropertyDefTrait set(Element el, Object value) {
        assert el
        assert value != null

        VertexDefTrait vdt = VertexDef.lookup(el)
        if (vdt != null && !(vdt instanceof DynamicVertexDef)) {
            if (vdt.propertiesMustBeDefined) {
                boolean found = vdt.propertyDefs.find({it.label == getLabel()})
                if (!found) {
                    throw new IllegalArgumentException("${this} is not a property of ${vdt.label}: ${vdt.propertyDefs}")
                }
            }
        }

        el.property(getLabel(), value)
        this
    }


    /** */
    public PropertyDefTrait set(Element el, Closure value) {
        assert el
        assert value != null
        set(el, value())
    }


    /** */
    public PropertyDefTrait setIf(Element el, Object value, Closure cl) {
        assert el
        assert value != null
        assert cl != null
        if (cl(value)) set(el, value)
        this
    }


    /** */
    public PropertyDefTrait setIf(Element el, Closure cl) {
        assert el
        assert cl != null
        def val = cl()
        if (val != null) set(el, val)
        this
    }


	/** */
    public String getLabel() {
        def chunks = name().split('_')
        if (chunks.size() == 1) return chunks[0].toLowerCase()

        def str = chunks[0].toLowerCase()
        chunks.drop(1).each { str += it.toLowerCase().capitalize() }
        return str
    }


    /** */
    public String toString() {
        def str = "${name()} ${label}"
        if (unique) str += " unique:${unique}"
        if (required) str += " required:${required}"
        if (index) str += " index:${index}"
        return str

    }
    
}

/** */
@Slf4j
class PropertyDefTraitHolder implements PropertyDefTrait {
    PropertyDefTrait source

    public PropertyDefTraitHolder(PropertyDefTrait source) {
        assert source != null
        this.source = source
        this.defaultValue = source.defaultValue
        this.unique = source.unique
        this.required = source.required
        this.index = source.index
    }

    def methodMissing(String name, def args) {
        assert this.source != null
        source.invokeMethod(name, args)
    }
}