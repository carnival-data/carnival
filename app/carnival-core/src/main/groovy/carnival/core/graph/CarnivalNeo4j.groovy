package carnival.core.graph



import groovy.util.AntBuilder
import groovy.transform.ToString
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import org.reflections.Reflections

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph

import carnival.core.config.Defaults





/** */
@InheritConstructors
@Slf4j
class CarnivalNeo4j extends Carnival {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** File path to the graph directory */
	static String GRAPH_PATH = "${Defaults.dataGraphDirectory}"



	///////////////////////////////////////////////////////////////////////////
	// FACTORY 
	///////////////////////////////////////////////////////////////////////////

    /** */
    public static CarnivalNeo4j create(Map args = [:]) {
    	def graph = openGremlinGraph()

		def graphSchema
        if (args.vertexBuilders) graphSchema = new DefaultGraphSchema(args.vertexBuilders)
        else graphSchema = new DefaultGraphSchema()

        def graphValidator = new DefaultGraphValidator()
        def coreGraph = new CarnivalNeo4j(graph, graphSchema, graphValidator)

    	def g = graph.traversal()

    	try {
	    	coreGraph.initializeGremlinGraph(graph, g)
			coreGraph.initNeo4j(graph, g)
    	} finally {
    		if (g) g.close()
    	}

    	assert coreGraph
		return coreGraph
    }





	///////////////////////////////////////////////////////////////////////////
	// INITIALIZATION 
	///////////////////////////////////////////////////////////////////////////

	/** */
	public static PropertiesConfiguration neo4jConfig() {
   		def config = new PropertiesConfiguration()
   		config.setProperty('gremlin.neo4j.directory', Defaults.dataGraphDirectoryPath)

   		[
   			"carnival.gremlin.neo4j.conf.dbms.security.auth_enabled",
   			"carnival.gremlin.neo4j.conf.dbms.directories.plugins",
   			"carnival.gremlin.neo4j.conf.dbms.security.procedures.unrestricted",
   			"carnival.gremlin.neo4j.conf.dbms.security.procedures.whitelist",
   			"carnival.gremlin.neo4j.conf.dbms.unmanaged_extension_classes"
   		].each { p ->
		    def k = p.split('\\.').drop(1).join('.')
		    def v = Defaults.getConfigValue(p)
	   		config.setProperty(k, v)
   		}
   		
   		return config
	}


	/** */
	public static Graph openGremlinGraph(Map args = [:]) {
		log.trace "openGremlinGraph() creating or loading graph in ${GRAPH_PATH}"

		// configure log4j
   		System.setProperty('log4j.configuration', 'log4j.properties')

   		// get the neo4j config
   		def config = neo4jConfig()

   		// adjust neo4j config per params
   		if (args.graphDirectory) config.setProperty('gremlin.neo4j.directory', args.graphDirectory)

		// create gremlin Neo4jGraph
		log.info "opening Neo4j graph config:${config} ..."
		Graph graph = Neo4jGraph.open(config)
		assert graph

		return graph
	}


	/**
	 * Set property uniqueness constraints.
	 *
	 */
	public initNeo4j(Graph graph, GraphTraversalSource g) {
		log.trace "CarnivalNeo4j initNeo4j"

		// create uniqueness constraints
		withTransaction(graph) {
	    	graphSchema.vertexConstraints.each { labelDef ->
					labelDef.uniquePropertyKeys.each { property ->
						log.trace "attempting to create constraint: ${labelDef.label}.${property}"
	    			graph.cypher("CREATE CONSTRAINT ON (class:${labelDef.label}) ASSERT class.${property} IS UNIQUE")
	    			log.trace "graph initilization: created uniqueness constraint on ${labelDef.label}.${property}"
	    		}
	    	}
		}

        // create indexes
        withTransaction(graph) {
	    	graphSchema.vertexConstraints.each { labelDef ->
					labelDef.indexedPropertyKeys.each { property ->
	    			graph.cypher("CREATE INDEX ON :${labelDef.label}(${property})")
	    			log.trace "graph initilization: created index on :${labelDef.label}(${property}"
	    		}
	    	}
        }
	}


    ///////////////////////////////////////////////////////////////////////////
    // LIFE-CYCLE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public void close() {
        graph.close()
    }


	///////////////////////////////////////////////////////////////////////////
	// GRAPH DIRECTORY OPERATIONS
	///////////////////////////////////////////////////////////////////////////

	/** */
	public static File graphDir() {
		File graphDir = new File(GRAPH_PATH)
		assert graphDir.exists()
		assert graphDir.isDirectory()
		graphDir
	}


	/** */
	public static File sisterDir(String dirName) {
		assert dirName
		File graphDir = graphDir()
		File parentDir = graphDir.getParentFile()
		assert parentDir.exists()
		assert parentDir.isDirectory()

		new File(parentDir, dirName)
	}


	/** */
 	public static clearGraph() {
		log.info "clearGraph"
        def ant = new AntBuilder()
        def graphDir = graphDir()
        if (graphDir.exists()) ant.delete(dir:graphDir)
    }


	/** */
	public static resetGraphFrom(String sourceDirName) {
		log.info "resetGraphFrom sourceDirName:${sourceDirName}"

		File sourceDir = sisterDir(sourceDirName)
		assert sourceDir.exists()

		resetGraphFrom(sourceDir)
	}


	/** */
	public static resetGraphFromIfExists(String sourceDirName) {
		log.info "resetGraphFromIfExists sourceDirName:${sourceDirName}"

		File sourceDir = sisterDir(sourceDirName)
		if (sourceDir.exists()) resetGraphFrom(sourceDir)
	}


	/** */
	public static resetGraphFrom(File sourceDir) {
		log.info "resetGraphFrom sourceDir:${sourceDir}"

		assert sourceDir
		assert sourceDir.exists()

        def ant = new AntBuilder()
		File graphDir = graphDir()
        if (graphDir.exists()) ant.delete(dir:graphDir)
        ant.mkdir(dir:graphDir)
        ant.sequential {
			echo("copy graph ${sourceDir} to ${graphDir}")
		    copy(todir: graphDir) {
		        fileset(dir: sourceDir)
		    }
		    echo("done")
		}
	}


	/** */
	public copyGraph(String dirName) {
		log.info "copyGraph dirName: $dirName"
		assert dirName

		File publishDir = sisterDir(dirName)
		copyGraph(publishDir)
	}


	/** */
	public copyGraph(File publishDir) {
		log.info "copyGraph $publishDir"
		assert publishDir

		File graphDir = graphDir()

		// set up publish directory
        def ant = new AntBuilder()
        if (publishDir.exists()) ant.delete(dir:publishDir)
        ant.mkdir(dir:publishDir)

        // copy
        ant.sequential {
			echo("copy graph ${graphDir} to ${publishDir}")
		    copy(todir: publishDir) {
		        fileset(dir: graphDir)
		    }
		    echo("done")
		}
	}
	
}
