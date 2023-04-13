package carnival.core



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.util.logging.Slf4j
import groovy.transform.ToString

import org.apache.tinkerpop.gremlin.structure.Graph

import carnival.graph.VertexBuilder



/**
 * A configuration object for a CarnivalNeo4j.
 */
@Slf4j
@ToString(includeNames=true)
class CarnivalNeo4jConfiguration {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** The default relative file path for the neo4j graph */
    final static String GRAPH_PATH_DEFAULT = "carnival-home/neo4j/graph"

    /** 
     * Returns a default configuration object.
     * @return A default CarnivalNeo4j configuration.
     */
    static public CarnivalNeo4jConfiguration defaultConfiguration() {
        Path currentRelativePath = Paths.get("")
        Path carnivalHomePath = currentRelativePath.resolve(GRAPH_PATH_DEFAULT)
        String carnivalHomePathString = carnivalHomePath.toAbsolutePath().toString()

        CarnivalNeo4jConfiguration config = new CarnivalNeo4jConfiguration()
        config.gremlin.neo4j.directory = carnivalHomePathString

        return config
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONFIG FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * A data holder for CarnivalNeo4j configuration elements.  See the Neo4j
     * documentation for a description of each configuration element.
     */
    @ToString(includeNames=true)
    static class Gremlin {

        /** @see <a href="https://neo4j.com/docs/operations-manual/current/configuration/neo4j-conf/">Neo4j Configuration</a> */
        @ToString(includeNames=true)
        static class Neo4j {

            /** The directory Neo4j will use for the database files */
            String directory

            /** 
             * If true, an empty database directory will be created if it does
             * not already exist; Neo4j requires that the directy be present
             * when the database starts up. 
             */
            Boolean directoryCreateIfNotPresent = true

            /** @see <a href="https://neo4j.com/docs/operations-manual/current/configuration/neo4j-conf/">Neo4j Configuration</a> */
            @ToString(includeNames=true)
            static class Conf {

                /** @see <a href="https://neo4j.com/docs/operations-manual/current/configuration/neo4j-conf/">Neo4j Configuration</a> */
                @ToString(includeNames=true)
                static class Dbms {
                    String unmanaged_extension_classes

                    /** @see <a href="https://neo4j.com/docs/operations-manual/current/configuration/neo4j-conf/">Neo4j Configuration</a> */
                    @ToString(includeNames=true)
                    static class Directories {
                        String plugins
                    }
                    Directories directories = new Directories()

                    /** @see <a href="https://neo4j.com/docs/operations-manual/current/configuration/neo4j-conf/">Neo4j Configuration</a> */
                    @ToString(includeNames=true)
                    static class Security {
                        String auth_enabled

                        /** @see <a href="https://neo4j.com/docs/operations-manual/current/configuration/neo4j-conf/">Neo4j Configuration</a> */
                        @ToString(includeNames=true)
                        static class Procedures {
                            String unrestricted
                            String whitelist
                        }
                        Procedures procedures = new Procedures()

                    }
                    Security security = new Security()

                }
                Dbms dbms = new Dbms()

            }
            Conf conf = new Conf()

        }
        Neo4j neo4j = new Neo4j()

    }
    Gremlin gremlin = new Gremlin()


}
