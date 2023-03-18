package carnival.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * A simple Gradle plugin to add a consistent set of dependencies to code 
 * that relies on Carnival.
 *
 */
class CarnivalLibraryPlugin implements Plugin<Project> {

    /**
     * Apply a consistent set of Carnival dependencies to the Gradle project.
     */
    void apply(Project project) {
        // dependency versions
        Locale locale = new Locale("en", "US");
        ResourceBundle versions = ResourceBundle.getBundle("carnival.gradle.Version", locale);        
        def groovyVersion = versions.getString("groovyVersion")
        def gremlinVersion = versions.getString("gremlinVersion")
        def neo4jTinkerpopVersion = versions.getString("neo4jTinkerpopVersion")
        def neo4JavaDriverVersion = versions.getString("neo4JavaDriverVersion")
        def carnivalVersion = versions.getString("carnivalVersion")

        println "[Carnival] Java version: ${System.getProperty('java.version')}"
        println "[Carnival] Groovy version: ${groovyVersion}"
        println "[Carnival] Gremlin version: ${gremlinVersion}"
        println "[Carnival] Neo4j Tinkerpop version: ${neo4jTinkerpopVersion}"
        println "[Carnival] Neo4 Java Driver version: ${neo4JavaDriverVersion}"
        println "[Carnival] Carnival version: ${carnivalVersion}"

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
            implementation("io.github.carnival-data:carnival-vine:${carnivalVersion}")
        }
    }
}