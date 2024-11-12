package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex



/** 
 * Utilities useful for GremlinTrait.
 */
class GremlinTraitUtilities {

    /** A log object used in a static context */
    static Logger log = LoggerFactory.getLogger(GremlinTraitUtilities)


    /**
     * Call the supplied closure passing in the graph and traversal source if
     * they are present.
     * <p>
     * If the closure accepts a single argument, it will be called with  the
     * graph traversal source as the single argument.
     * <p>
     * If the closure accepts more than one argument, it will be called with
     * the gremlin graph as the first argument and the graph traversal source
     * as the second.
     * @param graph A gremlin graph
     * @param g A graph traversal source to use
     * @param cl The closure to execute
     * @return The result of the closure
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
     * Call withGremlin with default optional args.
     * args.commitExisting: false
     * args.useTransactionIfSupported: true
     * @see withGremlin(Map, Graph, GraphTraversalSource, Closure)
     */
    static public Object withGremlin(Graph graph, GraphTraversalSource g, Closure cl) {
        withGremlin(
            [
                useTransactionIfSupported: true,
                commitExisting: false
            ], 
            graph, 
            g, 
            cl
        )
    }


    /**
     * Call the provided closure passing the gremlin graph and graph traversal 
     * source.  If the gremlin graph supports transactions, the transaction
     * will be rolled back if the closure throws an exception and committed
     * otherwise.
     * @param args Optional arguments
     * @param args.commitExisting If true, commit the existing transaction
     * @param graph A gremlin graph
     * @param g A graph traversal source
     * @param cl The closure to execute
     * @return The result of the closure
     */
    static public Object withGremlin(Map args, Graph graph, GraphTraversalSource g, Closure cl) {
        assert args != null
        assert graph != null
        assert g != null
        assert cl != null

        // check if transactions are supported
		def transactionsAreSupported = graph.features().graph().supportsTransactions()
        
        // get and assert count of closure parameters
        def maxClosureParams = cl.getMaximumNumberOfParameters()
        assert maxClosureParams > 0 : "closure must accept at least one parameter"

        // decide if transactions will be used
        boolean useTx = transactionsAreSupported && args.useTransactionIfSupported

		// deal with existing transaction
        if (useTx) {
            // optionally commit existing transaction
            if (args.commitExisting) {
                try {
                    graph.tx().commit()
                } catch (Exception e) {
                    log.error("could not commit existing transaction", e)
                }
            }

            // close existing transaction
            if (graph.tx().isOpen()) graph.tx().close()
            
            // open a new transaction
            graph.tx().open()
        }

        // try to run the closure
        def res
        try {
            if (maxClosureParams == 1) {
                res = cl(g)
            } else {
                res = cl(graph, g)
            }
        } catch (Exception e) {
            if (useTx) {
                try {
                    graph.tx().rollback()
                } catch (Exception e2) {
                    log.error("could not rollback", e2)
                }
            }
            throw e
        } finally {
            if (useTx) {
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