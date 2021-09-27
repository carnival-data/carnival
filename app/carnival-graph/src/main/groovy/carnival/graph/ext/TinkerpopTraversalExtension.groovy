package carnival.graph.ext



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.transform.EqualsAndHashCode

import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex

import carnival.graph.EdgeDefTrait
import carnival.graph.VertexDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.Base



/** */
class TinkerpopTraversalExtension {

    /** */
    static GraphTraversal properties(DefaultTraversal traversal, PropertyDefTrait pdef) {
        traversal.properties(pdef.label)
    }

    /** */
    static GraphTraversal isa(DefaultTraversal traversal, VertexDefTrait vdef) {
        traversal.hasLabel(vdef.label).has(Base.PX.NAME_SPACE.label, vdef.nameSpace)
    }

    /** */
    static GraphTraversal isa(DefaultTraversal traversal, EdgeDefTrait edef) {
        traversal.hasLabel(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }

    /** */
    static GraphTraversal both(DefaultTraversal traversal, EdgeDefTrait edef) {
        traversal.bothE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).otherV()
    }

    /** */
    static GraphTraversal bothE(DefaultTraversal traversal, EdgeDefTrait edef) {
        traversal.bothE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }

    /** */
    static GraphTraversal out(DefaultTraversal traversal, EdgeDefTrait edef) {
        traversal.outE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).inV()
    }

    /** */
    static GraphTraversal outE(DefaultTraversal traversal, EdgeDefTrait edef) {
        traversal.outE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }

    /** */
    static GraphTraversal "in"(DefaultTraversal traversal, EdgeDefTrait edef) {
        traversal.inE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).outV()
    }

    /** */
    static GraphTraversal inE(DefaultTraversal traversal, EdgeDefTrait edef) {
        traversal.inE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }

    /** */
    static GraphTraversal has(DefaultTraversal traversal, PropertyDefTrait pdef) {
        traversal.has(pdef.label)
    }

    /** */
    static GraphTraversal hasNot(DefaultTraversal traversal, PropertyDefTrait pdef) {
        traversal.hasNot(pdef.label)
    }

    /** */
    static GraphTraversal has(DefaultTraversal traversal, PropertyDefTrait pdef, Enum value) {
        traversal.has(pdef.label, value.name())
    }

    /** */
    static GraphTraversal has(DefaultTraversal traversal, PropertyDefTrait pdef, Object value) {
        traversal.has(pdef.label, value)
    }

    /** */
    static GraphTraversal matchesOn(DefaultTraversal traversal, PropertyDefTrait pdef, Vertex vertex) {
        if (pdef.of(vertex).isPresent()) {
            traversal.has(pdef.label, pdef.valueOf(vertex))
        } else {
            traversal.hasNot(pdef.label)
        }
        traversal        
    }

    /** */
    static GraphTraversal matchesOn(
        DefaultTraversal traversal, 
        PropertyDefTrait traversalPdef, 
        Vertex vertex, 
        PropertyDefTrait vertexPdef
    ) {
        if (vertexPdef.of(vertex).isPresent()) {
            traversal.has(traversalPdef.label, vertexPdef.valueOf(vertex))
        } else {
            traversal.hasNot(traversalPdef.label)
        }
        traversal        
    }

    /** */
    static Object nextOne(DefaultTraversal traversal) {
        def verts = traversal.toList()
        if (verts.size() > 1) throw new RuntimeException("nextOne: ${verts.size()}")
        if (verts.size() == 1) return verts.first()
        else return null
    }


}