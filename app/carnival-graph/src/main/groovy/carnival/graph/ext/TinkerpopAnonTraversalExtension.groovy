package carnival.graph.ext



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.transform.EqualsAndHashCode

import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.EdgeDefinition
import carnival.graph.VertexDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.Base



/** 
 * Extensions to the Tinkerpop Gremlin graph traversal language that enable the use of 
 * Carnival objects in anonymous Gremlin treversals.  See TinkerpopTraversalExtension
 * for descriptions of the step logic.
 *
 * @see TinkerpopTraversalExtension
 *
 */
class TinkerpopAnonTraversalExtension {

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal out(__ traversal, EdgeDefinition edef) {
        __.outE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).inV()
    }

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal both(__ traversal, EdgeDefinition edef) {
        __.bothE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).otherV()
    }

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal "in"(__ traversal, EdgeDefinition edef) {
        __.inE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).outV()
    }

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal isa(__ traversal, VertexDefinition vdef) {
        __.hasLabel(vdef.label).has(Base.PX.NAME_SPACE.label, vdef.nameSpace)
    }

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal isa(__ traversal, EdgeDefinition edef) {
        __.hasLabel(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal has(__ traversal, PropertyDefinition pdef) {
        traversal.has(pdef.label)
    }

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal has(__ traversal, PropertyDefinition pdef, Enum value) {
        traversal.has(pdef.label, value.name())
    }

    /** @see TinkerpopTraversalExtension */
    static GraphTraversal has(__ traversal, PropertyDefinition pdef, Object value) {
        traversal.has(pdef.label, value)
    }

}