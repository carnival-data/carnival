package carnival.graph



import groovy.util.logging.Slf4j
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.structure.Property



/** */
@Slf4j
trait PropertyDefTrait {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    Object defaultValue = null


	///////////////////////////////////////////////////////////////////////////
	// FACTORY METHODS
	///////////////////////////////////////////////////////////////////////////

    /*
    public ConstrainedPropertyDefTrait index() {
        println "index() ${this instanceof ConstrainedPropertyDefTrait} $this"
        def cpd = (this instanceof ConstrainedPropertyDefTrait) ? this : this as ConstrainedPropertyDefTrait
        cpd.index = true
        cpd
    }

    public ConstrainedPropertyDefTrait require() {
        println "require() ${this instanceof ConstrainedPropertyDefTrait} $this"
        def cpd = (this instanceof ConstrainedPropertyDefTrait) ? this : this as ConstrainedPropertyDefTrait
        cpd.required = true
        cpd
    }
    */

	/** */
	public ConstrainedPropertyDefTrait withConstraints(Map m) {
        def cpd = this as ConstrainedPropertyDefTrait

		if (m.unique) cpd.unique = m.unique
		if (m.required) cpd.required = m.required
		if (m.index) cpd.index = m.index

		return cpd
	}


    /** */
    public PropertyDefTrait defaultValue(Object o) {
        //log.debug "PropertyDefTrait.defaultValue o:${o.class.name}"
        //log.debug "this: ${this.class.name} ${this}"
        //log.debug "this instanceof ConstrainedPropertyDefTrait ${this instanceof ConstrainedPropertyDefTrait}"

        this.defaultValue = o
        return this
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
        el.value(getLabel())
    }


    /** */
    public PropertyDefTrait set(Element el, Object value) {
        assert el
        assert value != null
        el.property(getLabel(), value)
        this
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
    public String getLabel() {
        def chunks = name().split('_')
        if (chunks.size() == 1) return chunks[0].toLowerCase()

        def str = chunks[0].toLowerCase()
        chunks.drop(1).each { str += it.toLowerCase().capitalize() }
        return str
    }


    /** */
    public String toString() {
    	return "${name()} ${label}"
    }
    
}



/** */
@Slf4j
trait ConstrainedPropertyDefTrait {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    //PropertyDefTrait parent

    /**
     * If true, then the values of this property must be unique across all
     * elements.
     */
    Boolean unique = false

    /**
     * If true, then this property must be present in all elements.
     */
    Boolean required = false

    /**
     * If true, then this property should be indexed by the underlying database
     * system.
     */
    Boolean index = false


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////


    /** */
    public String toString() {
        def str = super.toString()
        if (unique) str += " unique:${unique}"
        if (required) str += " required:${required}"
        if (index) str += " index:${index}"
        return str
    }

}
