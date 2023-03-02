package carnival.core



import groovy.transform.ToString
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.core.graph.DefaultGraphSchema
import carnival.core.graph.DefaultGraphValidator





/** 
 * A Carnival with an underlying Tinkergraph implementation.
 */
@InheritConstructors
@Slf4j
class CarnivalTinker extends Carnival {

	///////////////////////////////////////////////////////////////////////////
	// FACTORY 
	///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a ready-to-use CarnivalTinker object.
     * @return A CarnivalTinker object.
     */
    public static CarnivalTinker create(Map args = [:]) {
		log.info "CarnivalTinker create args:$args"

    	def graph = TinkerGraph.open()

		def graphSchema
        if (args.vertexBuilders) graphSchema = new DefaultGraphSchema(args.vertexBuilders)
        else graphSchema = new DefaultGraphSchema()

        def graphValidator = new DefaultGraphValidator()
        def carnival = new CarnivalTinker(graph, graphSchema, graphValidator)

    	def g = graph.traversal()

    	try {
	    	carnival.initialize(graph, g)
    	} finally {
    		if (g) g.close()
    	}

    	assert carnival
		return carnival
    }




    ///////////////////////////////////////////////////////////////////////////
    // LIFE-CYCLE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Close this Carnival.
     */
    public void close() {
        graph.close()
    }

}
