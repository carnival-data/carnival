package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of




/** */
class CoreGraphUtils {


    ///////////////////////////////////////////////////////////////////////////
    // DEBUG PRINTING
    ///////////////////////////////////////////////////////////////////////////
    static void printVerts(Traversal trv, String prefixLine = "") {
        printVerts(trv.toList())
    }

    static void printVerts(Collection<Vertex> vertices, String prefixLine = "") {
        if (prefixLine) println prefixLine
        vertices.each { v ->
            println "${v.id()}:${v.label()} " + v.properties().collect { "${it.key()}:${it.value()}" }
        }
    }

    static String str(String prefix, int num) {
        return String.valueOf("${prefix}${num}")
    }

    static void printGraph(GraphTraversalSource g) {
        def vs = g.V().valueMap(true)
        vs.each { v -> println "$v" }

        def es = g.E()
        es.each { e -> println "$e" }
    }
}