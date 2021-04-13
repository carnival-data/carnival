package carnival.graph.ext



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.transform.EqualsAndHashCode

import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.EdgeDefTrait
import carnival.graph.VertexDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.Base



/** */
class TinkerpopAnonTraversalExtension {

    /** */
    static GraphTraversal out(__ traversal, EdgeDefTrait edef) {
        __.outE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).inV()
    }

    /** */
    static GraphTraversal "in"(__ traversal, EdgeDefTrait edef) {
        __.inE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).outV()
    }

    /** */
    static GraphTraversal isa(__ traversal, VertexDefTrait vdef) {
        __.hasLabel(vdef.label).has(Base.PX.NAME_SPACE.label, vdef.nameSpace)
    }

    /** */
    static GraphTraversal has(__ traversal, PropertyDefTrait pdef) {
        traversal.has(pdef.label)
    }

    /** */
    static GraphTraversal has(__ traversal, PropertyDefTrait pdef, Enum value) {
        traversal.has(pdef.label, value.name())
    }

    /** */
    static GraphTraversal has(__ traversal, PropertyDefTrait pdef, Object value) {
        traversal.has(pdef.label, value)
    }

}