package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex



/** 
 * A trait to add methods helpful for interacting with a gremlin graph.
 */
trait GremlinTrait  {
    
    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** A general log */
    static Logger log = LoggerFactory.getLogger(GremlinTrait)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** A gremlin graph that will be added with this trait */
    Graph graph


    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Getter for the gremlin graph.
     * @return The gremlin graph
     */
    public Graph getGraph() { this.graph }

    /** 
     * Setter for the gremlin graph.
     * @param theGraph A gremlin graph
     */
    public void setGraph(Graph theGraph) { this.graph = theGraph }


    /** 
     * Create and return a graph traversal source
     * @return A new graph traversal source
     */
    public GraphTraversalSource traversal() { 
        Graph theGraph = getGraph()
        assert theGraph
        def g = theGraph.traversal() 
        return g
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

        // withTraversal closes g
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
            if (g) g.close()
        }        
    }

}