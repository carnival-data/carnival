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
 * enums with the `@PropertyModel` annotation.
 * 
 * @see carnival.graph.PropertyModel
 */
@Slf4j
trait PropertyDefinition {


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
     * @return This object
     * 
     * */
	public PropertyDefinition withConstraints(Map m) {
        def newObj = new PropertyDefinitionHolder(this)

		if (m.unique) newObj.unique = m.unique
		if (m.required) newObj.required = m.required
		if (m.index) newObj.index = m.index

		return newObj
	}


	/** 
     * Synonym of withConstraints().
     * @see #withConstraints(Map)
     */
	public PropertyDefinition constraints(Map m) {
        withConstraints(m)
    }


    /** 
     * Set the default value for this property.
     * @param o The default value to use
     * @return This object
     */
    public PropertyDefinition defaultValue(Object o) {
        def newObj = new PropertyDefinitionHolder(this)
        newObj.defaultValue = o
        return newObj
    }


	///////////////////////////////////////////////////////////////////////////
	// PROPERTY METHODS
	///////////////////////////////////////////////////////////////////////////

    /** 
     * Assert that the property is defined in the element and then return the 
     * property of the provided element.
     * @param el The source element
     * @return The property object
     */
    public Property of(Element el) {
        assert el
        assertPropertyIsDefined(el)

        _of(el)
    }


    /** 
     * Return property of the provided element.
     * @param el The source element
     * @return The property object
     */
    Property _of(Element el) {
        assert el
        el.property(getLabel())
    }


    /** 
     * Assert the property is defined and return the property value of the 
     * source element.
     * @param el The source element
     * @return The value of the property.
     */
    public Object valueOf(Element el) {
        assert el
        assertPropertyIsDefined(el)

        _valueOf(el)
    }


    /** 
     * Return the property value of the source element.
     * @param el The source element
     * @return The value of the property.
     */
    Object _valueOf(Element el) {
        assert el
        el.property(getLabel()).isPresent() ? el.value(getLabel()) : null
    }


    /** 
     * Set the property value of the provided element to the provided value.
     * Asserts that the property is defined.
     * @param value The property value
     * @param el The target element
     * @return This object
     */
    public PropertyDefinition set(Element el, Object value) {
        assert el
        assert value != null
        assertPropertyIsDefined(el)

        el.property(getLabel(), value)
        this
    }


    /** 
     * Assert that the property is defined in the target element.
     * @param el The target element
     */
    void assertPropertyIsDefined(Element el) {
        assert el

        EnumSet allBaseDefs = EnumSet.allOf(Base.PX)
        if (allBaseDefs.contains(this)) return

        boolean isDefined = false
        
        ElementDefinition edt = Definition.lookup(el)
        if (edt != null && !(edt instanceof DynamicVertexDef)) {
            if (edt.propertiesMustBeDefined) {
                isDefined = edt.propertyDefs.find({it.label == getLabel()})
            }
        }
        
        if (!isDefined) {
            throw new IllegalArgumentException("${this} is not a property of ${edt.label}: ${edt.propertyDefs}")

        }
    }


    /** 
     * Returns true if the property is defined in the provided target element.
     * @param el The target element
     * @return True if the property is defined
     */
    boolean propertyIsDefined(Element el) {
        boolean isDefined = false
        ElementDefinition edt = Definition.lookup(el)
        if (edt != null && !(edt instanceof DynamicVertexDef)) {
            if (edt.propertiesMustBeDefined) {
                isDefined = edt.propertyDefs.find({it.label == getLabel()})
            }
        }
        isDefined
    }


    /** 
     * Set the property of the target element to the result of running the
     * provided closure.
     * @param value The value closure
     * @param el The target element
     * @return This object
     */
    public PropertyDefinition set(Element el, Closure value) {
        assert el
        assert value != null
        set(el, value())
    }


    /** 
     * Set the property of the provided target element to the value if the
     * provided closure returns true when passed the value as the only
     * argument.
     * @param cl A closure whose result will be evaluated as a boolean
     * @param value The value to pass to the closure and set to the property
     * value
     * @param el The target element
     * @return This object
     */
    public PropertyDefinition setIf(Element el, Object value, Closure cl) {
        assert el
        assert value != null
        assert cl != null
        if (cl(value)) set(el, value)
        this
    }


    /** 
     * Run the provided closure; if the result is not null, set the property
     * of the target element to it.
     * @param cl The closure to run
     * @param el The target element
     * @return This object
     */
    public PropertyDefinition setIf(Element el, Closure cl) {
        assert el
        assert cl != null
        def val = cl()
        if (val != null) set(el, val)
        this
    }


	/** 
     * Returns the property label to use for this property definition.
     * @return The property label as a string
     */
    public String getLabel() {
        def chunks = name().split('_')
        if (chunks.size() == 1) return chunks[0].toLowerCase()

        def str = chunks[0].toLowerCase()
        chunks.drop(1).each { str += it.toLowerCase().capitalize() }
        return str
    }


    /** 
     * Returns a string representation of this object
     * @return A string
     */
    public String toString() {
        def str = "${name()} ${label}"
        if (unique) str += " unique:${unique}"
        if (required) str += " required:${required}"
        if (index) str += " index:${index}"
        return str

    }
    
}

/** 
 * PropertyDefinitionHolder extends PropertyDefinition to add a methodMissing()
 * passes unkonwn method calls to the wrapped property.
 */
@Slf4j
class PropertyDefinitionHolder implements PropertyDefinition {
    PropertyDefinition source

    public PropertyDefinitionHolder(PropertyDefinition source) {
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