package carnival.core.graph



import groovy.transform.ToString
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import org.reflections.Reflections

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.PropertiesConfiguration

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

import carnival.core.config.Defaults





/** */
@InheritConstructors
@Slf4j
class CoreGraphTinker extends CoreGraph {

	///////////////////////////////////////////////////////////////////////////
	// FACTORY 
	///////////////////////////////////////////////////////////////////////////

    /** */
    public static CoreGraphTinker create(Map args = [:]) {
		log.info "CoreGraphTinker create args:$args"

    	def graph = TinkerGraph.open()

		def graphSchema
        if (args.vertexBuilders) graphSchema = new DefaultGraphSchema(args.vertexBuilders)
        else graphSchema = new DefaultGraphSchema()

        def graphValidator = new DefaultGraphValidator()
        def coreGraph = new CoreGraphTinker(graph, graphSchema, graphValidator)

    	def g = graph.traversal()

    	try {
	    	coreGraph.initializeGremlinGraph(graph, g)
    	} finally {
    		if (g) g.close()
    	}

    	assert coreGraph
		return coreGraph
    }




    ///////////////////////////////////////////////////////////////////////////
    // LIFE-CYCLE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public void close() {
        graph.close()
    }

}
