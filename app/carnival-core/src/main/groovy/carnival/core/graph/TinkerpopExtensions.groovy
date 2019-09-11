package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.sql.GroovyRowResult
import groovy.transform.EqualsAndHashCode

import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversal
//import carnival.core.graph.DefaultCarnivalTraversal



/** */
class TinkerpopExtensions {

    /** */
    static Object nextOne(DefaultTraversal traversal) {
        def verts = traversal.toList()
        if (verts.size() > 1) throw new RuntimeException("nextOne: ${verts.size()}")
        if (verts.size() == 1) return verts.first()
        else return null
    }

    /** */
    /*static Object nextOne(DefaultCarnivalTraversal traversal) {
        def verts = traversal.toList()
        if (verts.size() > 1) throw new RuntimeException("nextOne: ${verts.size()}")
        if (verts.size() == 1) return verts.first()
        else return null
    }*/

}