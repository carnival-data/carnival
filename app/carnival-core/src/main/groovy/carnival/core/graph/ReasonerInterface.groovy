package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex

import carnival.core.vine.Vine



/** */
public interface ReasonerInterface {
	
    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Graph getGraph()

    /** */
    public GraphTraversalSource traversal()

    /** */
    public Object cypher(String q)

    /** */
    public Object cypher(String q, Map args)

    /** 
     * Calls the given closure with two parameters, the Gremlin graph
     * and an active GraphTraversalSource, eg. cl(graph, g).
     *
     */
    public Object withGremlin(Closure cl)

    /** */
    public Map validate(Map args)

    /** */
    public Map reason(Map args)

}