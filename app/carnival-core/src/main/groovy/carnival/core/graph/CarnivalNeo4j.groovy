package carnival.core.graph



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
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





/** */
@InheritConstructors
@Slf4j
class CarnivalNeo4j extends Carnival {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** */
    public static CarnivalNeo4j create(Map args = [:]) {
		CarnivalNeo4jConfiguration defaultConfig = CarnivalNeo4jConfiguration.defaultConfiguration()
		create(defaultConfig, args)
	}


    /** */
    public static CarnivalNeo4j create(CarnivalNeo4jConfiguration config, Map args = [:]) {
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
	    	carnival.initializeGremlinGraph(graph, g)
			carnival.neo4jConstraints(graph, g)
    	} finally {
    		if (g) g.close()
    	}

    	assert carnival
		return carnival
    }


	/** */
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


	/** */
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
	 * Initialize directory structure.
	 *
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

	CarnivalNeo4jConfiguration config



	///////////////////////////////////////////////////////////////////////////
	// NEO4J
	///////////////////////////////////////////////////////////////////////////

	/** */
	public void initializeFiles() {
		assert this.config
		initializeFiles(this.config)
	}

	/**
	 * Set property uniqueness constraints.
	 *
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
	 * Set indexes.
	 *
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


	/** */
	public String graphPath() {
		assert this.config
		Paths.get(config.gremlin.neo4j.directory)
	}


	/** */
	public String graphPathString() {
		Path dirPath = graphPath()
		String dirPathString = dirPath.toAbsolutePath().toString()
		return dirPathString
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
	public static File graphDir(CarnivalNeo4jConfiguration config) {
		def graphPath = Paths.get(config.gremlin.neo4j.directory)
		File graphDir = graphPath.toFile()
		graphDir
	}


	/** */
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


	/** */
 	public static clearGraph(CarnivalNeo4jConfiguration config) {
		log.info "clearGraph"
        def ant = new AntBuilder()
        def graphDir = graphDir(config)
        if (graphDir.exists()) ant.delete(dir:graphDir)
    }


	/** */
	public static resetGraphFrom(CarnivalNeo4jConfiguration config, String sourceDirName) {
		log.info "resetGraphFrom sourceDirName:${sourceDirName}"

		File sourceDir = sisterDir(config, sourceDirName)
		assert sourceDir.exists()

		resetGraphFrom(config, sourceDir)
	}


	/** */
	public resetGraphFromIfExists(CarnivalNeo4jConfiguration config, String sourceDirName) {
		log.info "resetGraphFromIfExists sourceDirName:${sourceDirName}"

		File sourceDir = sisterDir(config, sourceDirName)
		if (sourceDir.exists()) resetGraphFrom(config, sourceDir)
	}


	/** */
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


	/** */
	public copyGraph(CarnivalNeo4jConfiguration config, String dirName) {
		log.info "copyGraph dirName: $dirName"
		assert dirName

		File publishDir = sisterDir(config, dirName)
		copyGraph(config, publishDir)
	}


	/** */
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
	
}
