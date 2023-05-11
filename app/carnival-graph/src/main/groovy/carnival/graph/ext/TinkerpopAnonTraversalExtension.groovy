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

    /** @see TinkerpopTraversalExtension#out(DefaultTraversal, EdgeDefinition) */
    static GraphTraversal out(__ traversal, EdgeDefinition edef) {
        __.outE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).inV()
    }

    /** @see TinkerpopTraversalExtension#both(DefaultTraversal, EdgeDefinition) */
    static GraphTraversal both(__ traversal, EdgeDefinition edef) {
        __.bothE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).otherV()
    }

    /** @see TinkerpopTraversalExtension#in(DefaultTraversal, EdgeDefinition) */
    static GraphTraversal "in"(__ traversal, EdgeDefinition edef) {
        __.inE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).outV()
    }

    /** @see TinkerpopTraversalExtension#isa(DefaultTraversal, VertexDefinition) */
    static GraphTraversal isa(__ traversal, VertexDefinition vdef) {
        __.hasLabel(vdef.label).has(Base.PX.NAME_SPACE.label, vdef.nameSpace)
    }

    /** @see TinkerpopTraversalExtension#isa(DefaultTraversal, EdgeDefinition) */
    static GraphTraversal isa(__ traversal, EdgeDefinition edef) {
        __.hasLabel(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }

    /** @see TinkerpopTraversalExtension#has(DefaultTraversal, PropertyDefinition) */
    static GraphTraversal has(__ traversal, PropertyDefinition pdef) {
        traversal.has(pdef.label)
    }

    /** @see TinkerpopTraversalExtension#has(DefaultTraversal, PropertyDefinition, Enum) */
    static GraphTraversal has(__ traversal, PropertyDefinition pdef, Enum value) {
        traversal.has(pdef.label, value.name())
    }

    /** @see TinkerpopTraversalExtension#has(DefaultTraversal, PropertyDefinition, Object) */
    static GraphTraversal has(__ traversal, PropertyDefinition pdef, Object value) {
        traversal.has(pdef.label, value)
    }

}