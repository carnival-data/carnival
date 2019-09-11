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
        def res
        
        try {
            res = cl(graph, g)
        } catch (Exception e) {
            try {
                graph.tx().rollback()
            } catch (Exception e2) {
                log.error("could not rollback", e2)
            }
            throw e
        } finally {
            try {
                graph.tx().commit()
            } catch (Exception e3) {
                log.error("could not commit", e3)
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
     * cl(graph, g)
     *
     */
    public Object withTraversal(Closure cl) {
        def g = traversal()
        try {
            cl(g)
        } finally {
            g.close()
        }
    }


    /**
     * cl(graph, g)
     *
     */
    public Object withGremlin(Closure cl) {
        def g = traversal()
        try {
            GremlinTraitUtilities.withGremlin(graph, g, cl)    
        } finally {
            g.close()
        }        
    }

}