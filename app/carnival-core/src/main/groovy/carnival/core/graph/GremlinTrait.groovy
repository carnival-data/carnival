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

    static Logger log = LoggerFactory.getLogger(GremlinTraitUtilities)


    /**
     * Call the supplied closure passing in the graph and traversal source if
     * they are present.
     *
     */
    static public Object withTraversal(Graph graph, GraphTraversalSource g, Closure cl) {
        def maxClosureParams = cl.getMaximumNumberOfParameters()
        assert maxClosureParams > 0 : "closure must accept at least one parameter"

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
    static Logger log = LoggerFactory.getLogger(GremlinTrait)


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
    public void setGraph(Graph theGraph) { this.graph = theGraph }


    /** */
    public GraphTraversalSource traversal() { 
        Graph theGraph = getGraph()
        assert theGraph
        def g = theGraph.traversal() 
        return g
    }


    /** */
    public Object cypher(String q) {
        sqllog.info("GrelinTrait.cypher:\n$q")
        assert graph
        return graph.cypher(q)
    }


    /** */
    public Object cypher(String q, Map args) {
        sqllog.info("GremlinTrait.cypher:\n$q\n$args")
        assert graph
        return graph.cypher(q, args)
    }


    /**
     * Given a closure that can accept one or two parameters, call the closure
     * passing the gremlin graph and optionally a graph traversal source as
     * parameters.  Note that in Groovy, if the final argument of a method is a
     * closure, the method can optionally be invoked by passing in the closure
     * argument outside the parenthesis containing the other arguments.  If
     * there are no arguments, the method can be called as follows.
     *
     * Example.
     *  carnival.coreGraph.withTraversal { graph, g ->
     *      totalVertices = g.V().count().next()
     *  }
     *
     * @parameter cl A Closure that accepts one or two parameters. {graph -> }
     * or { graph, g -> }.
     *
     */
    public Object withTraversal(Closure cl) {
        assert cl != null
        
        GraphTraversalSource g = traversal()
        assert g

        Graph graph = graph
        assert graph

        GremlinTraitUtilities.withTraversal(graph, g, cl)
    }


    /**
     * Given a closure that accepts two parameters, call the closure passing
     * the graph as the first parameter and a traversal source as the second.
     * When the closure terminates, if transactions are supported by the
     * underlying graph, commit the transaction.  If an exception is thrown,
     * then there will be an attempt to roll back the transaction.
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