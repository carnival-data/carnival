package carnival.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.util.StringUtils
import carnival.graph.Base



/**
 *
 *
 */
@Slf4j
class VertexDef {

    /** */
    static public VertexDefTrait lookup(Vertex v) {
        assert v != null

        def defName = StringUtils.toScreamingSnakeCase(v.label())
        log.trace "defName: $defName"

        def defClassName
        if (Base.PX.VERTEX_DEFINITION_CLASS.of(v).isPresent()) {
            defClassName = Base.PX.VERTEX_DEFINITION_CLASS.valueOf(v)
        } else {
            defClassName = Base.PX.NAME_SPACE.valueOf(v)
        }
        log.trace "defClassName: $defClassName"

        def defClass = Class.forName(defClassName)
        log.trace "defClass: $defClass"

        def isEnum = Enum.isAssignableFrom(defClass)
        log.trace "is enum ${isEnum}"

        def defInstance
        if (isEnum) {
            defInstance = Enum.valueOf(defClass, defName) 
        } else {
            defInstance = defClass.newInstance()
            defInstance.name = defName
        }
        log.trace "defInstance: $defInstance"

        return defInstance
    }

}





