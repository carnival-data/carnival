package carnival.core.graph



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
import carnival.core.config.Defaults



/**
 * gradle test --tests "carnival.core.graph.CoreGraphNeo4jSpec"
 *
 */
class CoreGraphNeo4jSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    static enum VX implements VertexDefTrait {
        CGS_SUITCASE
    }

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared coreGraph
    
    @Shared vertexBuilders = [
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "1"),
        Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, "2"),
    ]


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setup() {
    }

    def setupSpec() {
        CoreGraphNeo4j.clearGraph()
        coreGraph = CoreGraphNeo4j.create(vertexBuilders:vertexBuilders)
    } 


    def cleanupSpec() {
        if (coreGraph) coreGraph.graph.close()
    }


    def cleanup() {
        if (coreGraph) coreGraph.graph.tx().rollback()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    /*def "test expose Bolt port"() {
        when:
		// expose Bolt port
		DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( Defaults.getDataGraphDirectoryPath() )
        	.setConfig( BoltConnector.enabled, true )
        	.setConfig( BoltConnector.listen_address, new SocketAddress( "localhost", 7687 ) )
        .build();

        then:
        managementService != null
    }*/


    
    @IgnoreIf({ !Defaults.getConfigValue('carnival.gremlin.conf.dbms.directories.plugins') })
    def "test apoc"() {
        when: 
        def graph = coreGraph.graph
        def apocVersion
        try {
            apocVersion = graph.cypher('RETURN apoc.version()').toList().first()
            println "apocVersion: $apocVersion"
        } catch (org.neo4j.graphdb.QueryExecutionException e) {
            e.printStackTrace()
            def pluginDir = Defaults.getConfigValue('carnival.gremlin.conf.dbms.directories.plugins')
            println "in order to run APOC, the APOC library must be present on the file system and configured in the application configuration."
            println "has carnival.home been set?  or a configuration otherwise provided?"
            println "is the following plugin directory valid? ${pluginDir}"
        }

        then:
        apocVersion != null
    }


    def "test initializeGraph for uniqueness constraint existence"() {
    	when:
    	def graph = coreGraph.graph
    	def constraints = graph.cypher("CALL db.constraints()").toList()

    	then:
    	//println constraints
    	constraints != null
    	constraints.size() >= 2
    }

}

