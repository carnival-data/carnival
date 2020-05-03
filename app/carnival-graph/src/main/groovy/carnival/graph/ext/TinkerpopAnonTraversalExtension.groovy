package carnival.graph.ext



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.sql.GroovyRowResult
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
    static GraphTraversal isa(__ traversal, VertexDefTrait vdef) {
        __.hasLabel(vdef.label).has(Base.PX.NAME_SPACE.label, vdef.nameSpace)
    }

}