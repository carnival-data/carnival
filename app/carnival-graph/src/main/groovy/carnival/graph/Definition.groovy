package carnival.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.util.StringUtils
import carnival.graph.Base


/**
 * A collection of static methods relevent to element definitions.
 */
@Slf4j
class Definition {

    /** 
     * Look up the element definition of the provided element.  If the element
     * is a vertex, a VertexDefinition will be returned; if the element is an
     * edge, and EdgeDefinition will be returned.
     * @param v The source element
     * @return The ElementDefinition that applies to the element.
     */
    static public ElementDefinition lookupElementDefinition(Element v) {
        assert v != null
        assert (v instanceof Edge || v instanceof Vertex)

        String label = v.label()
        //log.trace "label: ${label}"

        def defName
        if (v instanceof Vertex) defName = StringUtils.toScreamingSnakeCase(label)
        else if (v instanceof Edge) defName = label.toUpperCase()
        //log.trace "defName: $defName"

        def defClassName
        if (Base.PX.VERTEX_DEFINITION_CLASS._of(v).isPresent()) {
            defClassName = Base.PX.VERTEX_DEFINITION_CLASS._valueOf(v)
        } else {
            defClassName = Base.PX.NAME_SPACE._valueOf(v)
        }
        //log.trace "defClassName: $defClassName"

        def defClass = Class.forName(defClassName)
        //log.trace "defClass: $defClass"

        def isEnum = Enum.isAssignableFrom(defClass)
        //log.trace "is enum ${isEnum}"

        def defInstance
        if (isEnum) {
            defInstance = Enum.valueOf(defClass, defName) 
        } else {
            defInstance = defClass.newInstance()
            defInstance.name = defName
        }
        //log.trace "defInstance: $defInstance"

        return defInstance
    }

    
    /** 
     * Look up the vertex definition of the provided vertex.
     * @param v The source vertex
     * @return The VertexDefinition that applies to the vertex
     * @see #lookupElementDefinition(Element)
     */
    static public VertexDefinition lookup(Vertex v) {
        assert v != null
        lookupElementDefinition(v)
    }    


    /** 
     * Look up the edge definition of the provided edge.
     * @param v The source edge
     * @return The EdgeDefinition that applies to the edge
     * @see #lookupElementDefinition(Element)
     */
    static public EdgeDefinition lookup(Edge e) {
        assert e != null
        lookupElementDefinition(e)
    }
}





