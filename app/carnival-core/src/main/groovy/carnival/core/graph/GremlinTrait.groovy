package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex



/** */
class GremlinTraitUtilities {

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')

    /**
     * cl(graph, g)
     *
     */
    static public Object withGremlin(Graph graph, GraphTraversalSource g, Closure cl) {
        assert graph != null
        assert g != null
        assert cl != null

        def res
		def transactionsAreSupported = graph.features().graph().supportsTransactions()
        
        try {
            res = cl(graph, g)
        } catch (Exception e) {
            if (transactionsAreSupported) {
                try {
                    graph.tx().rollback()
                } catch (Exception e2) {
                    log.error("could not rollback", e2)
                }
            }
            throw e
        } finally {
            if (transactionsAreSupported) {
                try {
                    graph.tx().commit()
                } catch (Exception e3) {
                    log.error("could not commit", e3)
                }
            }
        }

        return res
    }

}



/** */
trait GremlinTrait  {
    
    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////
    static Logger sqllog = LoggerFactory.getLogger('sql')
    static Logger elog = LoggerFactory.getLogger('db-entity-report')
    static Logger log = LoggerFactory.getLogger('carnival')


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    Graph graph


    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Graph getGraph() { this.graph }


    /** */
    public GraphTraversalSource traversal() { 
        def g = this.graph.traversal() 
        return g
    }


    /** */
    public Object cypher(String q) {
        sqllog.info("GrelinTrait.cypher:\n$q")
        return graph.cypher(q)
    }


    /** */
    public Object cypher(String q, Map args) {
        sqllog.info("GremlinTrait.cypher:\n$q\n$args")
        return graph.cypher(q, args)
    }


    /**
     * cl(g)
     *
     */
    public Object withTraversal(Closure cl) {
        def maxClosureParams = cl.getMaximumNumberOfParameters()
        assert maxClosureParams > 0 : "closure must accept at least one parameter"

        GraphTraversalSource g = traversal()
        assert g

        Graph graph = graph
        assert graph

        try {
            if (maxClosureParams == 1) {
                cl(g)
            } else {
                cl(graph, g)
            }
        } finally {
            if (g != null) g.close()
        }
    }


    /**
     * cl(graph, g)
     *
     */
    public Object withGremlin(Closure cl) {
        GraphTraversalSource g = traversal()
        assert g

        Graph graph = graph
        assert graph

        try {
            GremlinTraitUtilities.withGremlin(graph, g, cl)    
        } finally {
            g.close()
        }        
    }

}