package carnival.core



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.util.AntBuilder
import groovy.transform.ToString
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils

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

import carnival.core.graph.DefaultGraphSchema
import carnival.core.graph.DefaultGraphValidator





/** 
 * A Carnival implementation with an underlying Neo4j graph.
 */
@InheritConstructors
@Slf4j
class CarnivalNeo4j extends Carnival {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** A log used to log Cypher statements */
    static Logger sqllog = LoggerFactory.getLogger('sql')


    /** 
	 * Create a CarnivalNeo4j object using the default configuration modified
	 * by any provided arguments.
	 * @param args A map of arguments
	 * @param args.vertexBuilders A collection of vertex builders that will be
	 * used to create the initial set of vertices in the graph.
	 * @return A started CarnivalNeo4j object
	 */
    public static CarnivalNeo4j create(Map args = [:]) {
		CarnivalNeo4jConfiguration defaultConfig = CarnivalNeo4jConfiguration.defaultConfiguration()
		create(defaultConfig, args)
	}


    /** 
	 * Create a CarnivalNeo4j object using the provided configuration and arguments.
	 * @param args A map of arguments
	 * @param args.vertexBuilders A collection of vertex builders that will be
	 * @param config A configuration object
	 * @return A started CarnivalNeo4j object
	 */
    public static CarnivalNeo4j create(CarnivalNeo4jConfiguration config, Map args = [:]) {
		assert config != null
		assert config instanceof CarnivalNeo4jConfiguration

		// initialize the directory structure
		initializeFiles(config)

		// open the neo4j gremlin graph
    	def graph = openGremlinGraph(config)

		// create a graph schema object
		def graphSchema
        if (args.vertexBuilders) graphSchema = new DefaultGraphSchema(args.vertexBuilders)
        else graphSchema = new DefaultGraphSchema()

		// create a graph validator object
        def graphValidator = new DefaultGraphValidator()
        
		// create the carnival object
		def carnival = new CarnivalNeo4j(graph, graphSchema, graphValidator)
		carnival.config = config

		// initialize the graph
    	def g = graph.traversal()
    	try {
	    	carnival.initialize(graph, g)
			carnival.neo4jConstraints(graph, g)
    	} finally {
    		if (g) g.close()
    	}

    	assert carnival
		return carnival
    }


	/** 
	 * Convert a CarnivalNeo4jConfiguration to a PropertiesConfiguration 
	 * object.
	 * @param config The source CarnivalNeo4jConfiguration
	 * @return A PropertiesConfiguration object with values based on the 
	 * provided CarnivalNeo4jConfiguration
	 */
	public static PropertiesConfiguration neo4jConfig(CarnivalNeo4jConfiguration config) {
   		def nconf = new PropertiesConfiguration()
   		nconf.setProperty('gremlin.neo4j.directory', config.gremlin.neo4j.directory)
		
		nconf.setProperty(
			'gremlin.neo4j.conf.dbms.security.auth_enabled', 
			config.gremlin.neo4j.conf.dbms.security.auth_enabled
		)
		nconf.setProperty(
			'gremlin.neo4j.conf.dbms.directories.plugins',
			config.gremlin.neo4j.conf.dbms.directories.plugins
		)
		nconf.setProperty(
			'gremlin.neo4j.conf.dbms.security.procedures.unrestricted',
			config.gremlin.neo4j.conf.dbms.security.procedures.unrestricted
		)
		nconf.setProperty(
			'gremlin.neo4j.conf.dbms.security.procedures.whitelist',
			config.gremlin.neo4j.conf.dbms.security.procedures.whitelist
		)
		nconf.setProperty(
			'gremlin.neo4j.conf.dbms.unmanaged_extension_classes',
			config.gremlin.neo4j.conf.dbms.unmanaged_extension_classes
		)

   		return nconf
	}


	/** 
	 * Open a Neo4jGraph using the provided configuration.
	 * @param config A configuration object
	 * @return An opened Neo4jGraph
	 */
	public static Graph openGremlinGraph(CarnivalNeo4jConfiguration config) {
		log.trace "openGremlinGraph() creating or loading graph in ${config}"

		// configure log4j
   		System.setProperty('log4j.configuration', 'log4j.properties')

   		// get the neo4j config
   		def nconf = neo4jConfig(config)

		// create gremlin Neo4jGraph
		log.info "opening Neo4j graph nconf:${nconf} ..."
		Graph graph = Neo4jGraph.open(nconf)
		assert graph

		return graph
	}


	/**
	 * Initialize directory structure required to open a Neo4jgraph using the
	 * provided configuration.  The graph path is verified.  If the config
	 * directoryCreateIfNotPresent is set to true, the graph path will be
	 * created if not already present.
	 * @param config The configuration to use
	 */
	public static void initializeFiles(CarnivalNeo4jConfiguration config) {
        Path graphPath = Paths.get(config.gremlin.neo4j.directory)
		if (graphPath == null) throw new RuntimeException("graphPath is null")
        
		//String currentRelativePathString = currentRelativePath.toAbsolutePath().toString()
        //Path graphPath = currentRelativePath.resolve(CARNIVAL_HOME_NAME)
        //String graphPathString = graphPath.toAbsolutePath().toString()

		def assertDirectoryAttributes = { Path dirPath ->
			String dirPathString = dirPath.toAbsolutePath().toString()
			if (!Files.exists(dirPath)) {
                throw new RuntimeException("${dirPathString} does not exist")
			}
            if (!Files.isDirectory(dirPath)) {
                throw new RuntimeException("${dirPathString} is not a directory")
            }
            if (!Files.isWritable(dirPath)) {
                throw new RuntimeException("${dirPathString} is not writable")
            }
            if (!Files.isReadable(dirPath)) {
                throw new RuntimeException("${dirPathString} is not readable")
            }
		}

        if (!Files.exists(graphPath) && config.gremlin.neo4j.directoryCreateIfNotPresent) {
			log.trace "Files.createDirectories ${graphPath}"
			Files.createDirectories(graphPath)
		}

		assertDirectoryAttributes(graphPath)
    }



	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** The configuration of this object */
	CarnivalNeo4jConfiguration config



	///////////////////////////////////////////////////////////////////////////
	// NEO4J
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Initialise the directory structure of this Neo4j Carnival using the
	 * the configuration of this object.
	 * @see #initializeFiles(CarnivalNeo4jConfiguration)
	 */
	public void initializeFiles() {
		assert this.config
		initializeFiles(this.config)
	}


	/**
	 * Apply the property uniqueness constraints contained in the graph schema 
	 * using the provided graph and graph traversal source.
	 * @param graph The target gremlin graph
	 * @param g The graph traversal source to use
	 */
	public void neo4jConstraints(Graph graph, GraphTraversalSource g) {
		log.trace "CarnivalNeo4j neo4jConstraints"

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
	}


	/**
	 * Create the graph indexes specified in the graph schema using the
	 * provided graph and graph traversal source.
	 * @param graph The target gremlin graph
	 * @param g The graph traversal source to use
	 */
	public void createIndexes(Graph graph, GraphTraversalSource g) {
		log.trace "CarnivalNeo4j createIndexes"

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


	/** 
	 * Return the configured graph path as a Path using Paths.get.
	 * @return A Path representation of the configured graph path.
	 */
	public Path graphPath() {
		assert this.config
		Paths.get(config.gremlin.neo4j.directory)
	}


    ///////////////////////////////////////////////////////////////////////////
    // LIFE-CYCLE
    ///////////////////////////////////////////////////////////////////////////

    /** 
	 * Convenience method to close the underlying Neo4j graph.
	 */
    public void close() {
        graph.close()
    }


	///////////////////////////////////////////////////////////////////////////
	// GRAPH DIRECTORY OPERATIONS
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Return the graph directory from the provided configuration object as a 
	 * File object using Paths.get.
	 * @param @config The source configuration
	 * @return The graph directory as a File object
	 */
	public static File graphDir(CarnivalNeo4jConfiguration config) {
		def graphPath = Paths.get(config.gremlin.neo4j.directory)
		File graphDir = graphPath.toFile()
		graphDir
	}


	/** 
	 * Search for a directory named as the provided dirName that resides in the
	 * same parent directory as the graph directory as per the provided
	 * configuration.  The sister directory is asserted to exist and be a 
	 * directory.
	 * @param dirName The name of the directory to find
	 * @param config. The Carnival configuration
	 * @return The sister directory as a File object
	 */
	public static File sisterDir(CarnivalNeo4jConfiguration config, String dirName) {
		assert dirName
		File graphDir = graphDir(config)
		assert graphDir.exists()
		assert graphDir.isDirectory()
		File parentDir = graphDir.getParentFile()
		assert parentDir.exists()
		assert parentDir.isDirectory()

		new File(parentDir, dirName)
	}


	/** 
	 * Clear the graph directory of the provided configuration.
	 * @param config The configuration from which to get the graph directory
	 */
 	public static clearGraph(CarnivalNeo4jConfiguration config) {
		log.info "clearGraph"
        File graphDir = graphDir(config)
		if (graphDir.exists()) {
			FileUtils.deleteDirectory(graphDir)
			graphDir.delete()
		}
    }


	/** 
	 * Resets this Carnival object to use a Neo4j graph found in a sister
	 * directory of the configured graph directory that is named per the
	 * provided directory name.
	 * @param config The configuration with the default graph directory
	 * @param sourceDirName The name of the sister directory to use
	 * @see #sisterDir(CarnivalNeo4jConfiguration, String)
	 * @see #resetGraphFrom(CarnivalNeo4jConfiguration, File)
	 */
	public static resetGraphFrom(CarnivalNeo4jConfiguration config, String sourceDirName) {
		log.info "resetGraphFrom sourceDirName:${sourceDirName}"

		File sourceDir = sisterDir(config, sourceDirName)
		assert sourceDir.exists()

		resetGraphFrom(config, sourceDir)
	}


	/** 
	 * Reset this Carnival to the new graph directory only if it exists.
	 * @param config The configuration with the default graph directory
	 * @param sourceDirName The name of the sister directory to use
	 * @see #resetGraphFrom(CarnivalNeo4jConfiguration, String)
	 */
	public resetGraphFromIfExists(CarnivalNeo4jConfiguration config, String sourceDirName) {
		log.info "resetGraphFromIfExists sourceDirName:${sourceDirName}"

		File sourceDir = sisterDir(config, sourceDirName)
		if (sourceDir.exists()) resetGraphFrom(config, sourceDir)
	}


	/** 
	 * Copy the Neo4j graph from the specified source directory to the graph
	 * directory of the provided configuration.
	 * @param sourceDir The source Neo4j graph directory
	 * @param config The configuration containing the target graph path.
	 */
	public resetGraphFrom(CarnivalNeo4jConfiguration config, File sourceDir) {
		log.info "resetGraphFrom sourceDir:${sourceDir}"

		assert sourceDir
		assert sourceDir.exists()
		assert sourceDir.isDirectory()

        def ant = new AntBuilder()
		File graphDir = graphDir(config)
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


	/** 
	 * Copy the graph directory of the provided configuration to the provided
	 * target directory.
	 * @param dirName The String path to the target directory 
	 * @param config The configuration
	 */
	public copyGraph(CarnivalNeo4jConfiguration config, String dirName) {
		log.info "copyGraph dirName: $dirName"
		assert dirName

		File publishDir = sisterDir(config, dirName)
		copyGraph(config, publishDir)
	}


	/** 
	 * Copy the graph directory of the provided configuration to the provided
	 * target directory.
	 * @param publishDir The target directory
	 * @param config The configuration
	 */
	public copyGraph(CarnivalNeo4jConfiguration config, File publishDir) {
		log.info "copyGraph $publishDir"
		assert publishDir

		File graphDir = graphDir(config)
		assert graphDir.exists()
		assert graphDir.isDirectory()

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

	///////////////////////////////////////////////////////////////////////////
	// CYPHER
	///////////////////////////////////////////////////////////////////////////

    /** 
	 * Run the Cypher query specified by the provided String.
	 * @param q The Cypher query
	 * @return The result of running the Cypher query
	 */
    public Object cypher(String q) {
        sqllog.info("CarnivalNeo4j.cypher:\n$q")
        assert graph
        return graph.cypher(q)
    }


    /** 
	 * Run the Cypher query specified by the provided String using the
	 * provided arguments.
	 * @param q The Cypher query
	 * @param args The map of arguments passed to graph.cypher()
	 */
    public Object cypher(String q, Map args) {
        sqllog.info("CarnivalNeo4j.cypher:\n$q\n$args")
        assert graph
        return graph.cypher(q, args)
    }



	
}
