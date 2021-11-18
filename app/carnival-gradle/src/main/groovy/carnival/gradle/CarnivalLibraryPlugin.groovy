package carnival.gradle



import org.gradle.api.Plugin
import org.gradle.api.Project



class CarnivalLibraryPlugin implements Plugin<Project> {

    void apply(Project project) {
        // dependency versions
        Locale locale = new Locale("en", "US");
        ResourceBundle versions = ResourceBundle.getBundle("carnival.gradle.Version", locale);        
        def groovyVersion = versions.getString("groovyVersion")
        def gremlinVersion = versions.getString("gremlinVersion")
        def neo4jTinkerpopVersion = versions.getString("neo4jTinkerpopVersion")
        def neo4JavaDriverVersion = versions.getString("neo4JavaDriverVersion")
        def carnivalVersion = versions.getString("carnivalVersion")

        println "[CarnivalLibrary] Java version: ${System.getProperty('java.version')}"
        println "[CarnivalLibrary] Groovy version: ${groovyVersion}"
        println "[CarnivalLibrary] Gremlin version: ${gremlinVersion}"
        println "[CarnivalLibrary] Neo4j Tinkerpop version: ${neo4jTinkerpopVersion}"
        println "[CarnivalLibrary] Neo4 Java Driver version: ${neo4JavaDriverVersion}"
        println "[CarnivalLibrary] Carnival version: ${carnivalVersion}"

        // apply dependencies
        project.dependencies {
            // Groovy
            implementation "org.codehaus.groovy:groovy-all:${groovyVersion}"

            // Tinkerpop
            implementation "org.apache.tinkerpop:gremlin-core:${gremlinVersion}"
            implementation "org.apache.tinkerpop:gremlin-groovy:${gremlinVersion}"
            implementation "org.apache.tinkerpop:tinkergraph-gremlin:${gremlinVersion}"

            // Neo4J
            implementation "org.apache.tinkerpop:neo4j-gremlin:${gremlinVersion}"
            implementation "org.neo4j:neo4j-tinkerpop-api-impl:${neo4jTinkerpopVersion}"
            implementation "org.neo4j.driver:neo4j-java-driver:${neo4JavaDriverVersion}"

            // Carnival
            implementation("io.github.carnival-data:carnival-util:${carnivalVersion}")
            implementation("io.github.carnival-data:carnival-graph:${carnivalVersion}")
            implementation("io.github.carnival-data:carnival-core:${carnivalVersion}")
        }
    }
}