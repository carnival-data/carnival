package carnival.core.graph



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.IgnoreIf

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

/*import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.configuration.connectors.BoltConnector
import org.neo4j.configuration.helpers.SocketAddress*/

import carnival.graph.*
import carnival.core.util.FilesUtil



/**
 * gradle test --tests "carnival.core.graph.CarnivalNeo4jSpec"
 *
 */
class CarnivalNeo4jSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    static enum VX implements VertexDefinition {
        CGS_SUITCASE
    }

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared carnival
    
    @Shared vertexBuilders = [
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "1"),
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "2"),
    ]


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() {
        CarnivalNeo4jConfiguration cnConf = CarnivalNeo4jConfiguration.defaultConfiguration()
        CarnivalNeo4j.clearGraph(cnConf)
        carnival = CarnivalNeo4j.create(cnConf, [vertexBuilders:vertexBuilders])
    } 

    def setup() { }

    def cleanup() {
        if (carnival) carnival.graph.tx().rollback()
    }

    def cleanupSpec() {
        if (carnival) carnival.graph.close()
    }




    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    /*def "test expose Bolt port"() {
        when:
		// expose Bolt port
		DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( carnival.graphPath() )
        	.setConfig( BoltConnector.enabled, true )
        	.setConfig( BoltConnector.listen_address, new SocketAddress( "localhost", 7687 ) )
        .build();

        then:
        managementService != null
    }*/


    def "test configuration graph directory"() {
        when:
        CarnivalNeo4jConfiguration conf2 = CarnivalNeo4jConfiguration.defaultConfiguration()
        conf2.gremlin.neo4j.directory += "2"
        assert conf2.gremlin.neo4j.directory.endsWith("2")
        Path graphDir = Paths.get(conf2.gremlin.neo4j.directory)

        then:
        !Files.exists(graphDir)

        when:
        def carnival2 = CarnivalNeo4j.create(conf2)

        then:
        Files.exists(graphDir)

        cleanup:
        carnival2.graph.close()
        FilesUtil.delete(graphDir)
    }


    
    @IgnoreIf({ !CarnivalNeo4jConfiguration.defaultConfiguration().gremlin.neo4j.conf.dbms.directories.plugins })
    def "test apoc"() {
        when: 
        def graph = carnival.graph
        def apocVersion
        try {
            apocVersion = graph.cypher('RETURN apoc.version()').toList().first()
            println "apocVersion: $apocVersion"
        } catch (org.neo4j.graphdb.QueryExecutionException e) {
            e.printStackTrace()
            def pluginDir = CarnivalNeo4jConfiguration.defaultConfiguration().gremlin.neo4j.conf.dbms.directories.plugins
            println "in order to run APOC, the APOC library must be present on the file system and configured in the application configuration."
            println "has carnival.home been set?  or a configuration otherwise provided?"
            println "is the following plugin directory valid? ${pluginDir}"
        }

        then:
        apocVersion != null
    }


    def "test initializeGraph for uniqueness constraint existence"() {
    	when:
    	def graph = carnival.graph
    	def constraints = graph.cypher("CALL db.constraints()").toList()

    	then:
    	//println constraints
    	constraints != null
    	constraints.size() >= 2
    }

}

