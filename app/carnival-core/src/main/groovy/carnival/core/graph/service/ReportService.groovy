package carnival.core.graph.service



import java.text.SimpleDateFormat

import groovy.transform.Synchronized

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.step.*
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import static org.apache.tinkerpop.gremlin.process.traversal.step.TraversalOptionParent.Pick.*
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import org.apache.tinkerpop.gremlin.neo4j.structure.*

import carnival.core.*
import carnival.core.graph.*
import carnival.util.KeyType
import carnival.util.DataTable
import carnival.util.GenericDataTable
import carnival.util.TabularReport



/**
 *
 *
 */
abstract class ReportService {

    ///////////////////////////////////////////////////////////////////////////
    // LOGGING
    ///////////////////////////////////////////////////////////////////////////

    static final Logger sqllog = LoggerFactory.getLogger('sql')
    static final Logger elog = LoggerFactory.getLogger('db-entity-report')
    static final Logger log = LoggerFactory.getLogger(ReportService)


    ///////////////////////////////////////////////////////////////////////////
    // COMMON FORMATS
    ///////////////////////////////////////////////////////////////////////////

    static protected SimpleDateFormat DATE_FORMAT_COMPACT = new SimpleDateFormat("yyyyMMdd_HHmmss")


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    @Synchronized
    static String currentTimeAsString() {
        def d = new Date()
        return DATE_FORMAT_COMPACT.format(d)
    }


    /**
     * Convenenience method to log a Cypher statment and then run it.
     *
     */
    static public Object cypher(Graph graph, String q, Map args = [:]) {
        sqllog.info("ReportService.cypher:\n$q")
        def res = (args) ? graph.cypher(q, args) : graph.cypher(q)
        return res
    }



}