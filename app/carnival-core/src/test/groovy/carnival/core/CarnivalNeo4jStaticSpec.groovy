package carnival.core



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

import carnival.graph.*
import carnival.core.graph.*



/**
 * gradle test --tests "carnival.core.CarnivalNeo4jStaticSpec"
 *
 */
class CarnivalNeo4jStaticSpec extends Specification {



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "clearGraph deletes the graph directory"() {
        when:
            CarnivalNeo4jConfiguration cnConf = CarnivalNeo4jConfiguration.defaultConfiguration()
            CarnivalNeo4j.initializeFiles(cnConf)
            Path graphPath = Paths.get(cnConf.gremlin.neo4j.directory)
            File graphDir = graphPath.toFile()
        
        then:
            graphDir.exists()
            graphDir.isDirectory()
        
        when:
            CarnivalNeo4j.clearGraph(cnConf)
        
        then:
            !graphDir.exists()
    }

}

