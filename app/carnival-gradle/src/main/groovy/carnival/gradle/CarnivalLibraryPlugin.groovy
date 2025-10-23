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
        def carnivalVersion = versions.getString("carnivalVersion")
        def janusGraphVersion = versions.getString("janusGraphVersion")

        println "[Carnival] Java version: ${System.getProperty('java.version')}"
        println "[Carnival] Groovy version: ${groovyVersion}"
        println "[Carnival] Gremlin version: ${gremlinVersion}"
        println "[Carnival] Carnival version: ${carnivalVersion}"

        List<String> deps = [
            // Tinkerpop
            "org.apache.tinkerpop:gremlin-core:${gremlinVersion}",
            "org.apache.tinkerpop:gremlin-groovy:${gremlinVersion}",
            "org.apache.tinkerpop:tinkergraph-gremlin:${gremlinVersion}",

            // JanusGraph
            "org.janusgraph:janusgraph-core:${janusGraphVersion}",
            "org.janusgraph:janusgraph-berkeleyje:${janusGraphVersion}",

            // Carnival
            "io.github.carnival-data:carnival-util:${carnivalVersion}",
            "io.github.carnival-data:carnival-graph:${carnivalVersion}",
            "io.github.carnival-data:carnival-core:${carnivalVersion}",
            "io.github.carnival-data:carnival-vine:${carnivalVersion}",
        ]

        // apply dependencies
        project.dependencies.add('implementation', "org.apache.groovy:groovy-all:${groovyVersion}")
        deps.each { project.dependencies.add('implementation', it) }

        // apply java-test-fixtures dependencies
        project.pluginManager.withPlugin('java-test-fixtures') {
            deps.each { project.dependencies.add('testFixturesImplementation', it) }
        }
    }
}