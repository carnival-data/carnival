package carnival.core.graph



import groovy.util.logging.Slf4j
import groovy.transform.ToString

import org.apache.tinkerpop.gremlin.structure.Graph

import carnival.graph.VertexBuilder



@Slf4j
@ToString(includeNames=true)
class CarnivalNeo4jConfiguration {

    Graph graph
    GraphSchema graphSchema
    GraphValidator graphValidator    

    Set<VertexBuilder> vertexBuilders


    @ToString(includeNames=true)
    static class Gremlin {

        @ToString(includeNames=true)
        static class Neo4j {
            String directory

            @ToString(includeNames=true)
            static class Conf {

                @ToString(includeNames=true)
                static class Dbms {
                    String unmanaged_extension_classes

                    @ToString(includeNames=true)
                    static class Directories {
                        String plugins
                    }
                    Directories directories = new Directories()

                    @ToString(includeNames=true)
                    static class Security {
                        String auth_enabled

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
