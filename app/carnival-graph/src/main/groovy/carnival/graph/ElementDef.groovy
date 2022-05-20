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



@Slf4j
class ElementDef {

    /** */
    static public ElementDefTrait lookup(Element v) {
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

}





