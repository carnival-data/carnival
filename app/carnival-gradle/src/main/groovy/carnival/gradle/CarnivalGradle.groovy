package carnival.gradle



import org.gradle.api.Plugin
import org.gradle.api.Project



class CarnivalGradlePlugin implements Plugin<Project> {

    void apply(Project project) {
        def pn = project.name
        def ev = pn.split('-').collect({it.toUpperCase()}).join('_') + "_HOME"
        println "[CarnivalGradle] environment variable: $ev"

        if (!System.env."${ev}") {
            println "[CarnivalGradle] WARNING: ${ev} is not set. Using default configurations."
            return
        }

        def appHomeDirectory = System.env."${ev}"
        File appHomeDirectoryDir = new File(appHomeDirectory)
        if (!appHomeDirectoryDir.exists()) {
            println "[CarnivalGradle] WARNING: ${appHomeDirectory} does not exist. Using default configurations."
            return
        }
        if (!appHomeDirectoryDir.isDirectory()) {
            println "[CarnivalGradle] WARNING: ${appHomeDirectory} is not a directory. Using default configurations."
            return
        }        
        println "[CarnivalGradle] application home directory: $appHomeDirectory"

        project.ext.set("appHomeDirectory", appHomeDirectory)

        // set the carnival.home property in both the run and test environments
        project.test.systemProperty('carnival.home', appHomeDirectory)
        project.run.systemProperty('carnival.home', appHomeDirectory)

        // set the logback configuation file in run and test
        def logbackConfig = "${appHomeDirectory}/config/logback.xml"
        File logbackConfigFile = new File(logbackConfig)
        if (!logbackConfigFile.exists()) {
            println "[CarnivalGradle] WARNING: ${logbackConfig} does not exist. Using default logging configurations."
            return
        }
        project.test.systemProperty('logback.configurationFile', logbackConfigFile)
        project.run.systemProperty('logback.configurationFile', logbackConfigFile)

        // set the location of the external configuration files for run and test
        def externalConfigYamlFile = new File(appHomeDirectoryDir, "config/application.yml")
        if (externalConfigYamlFile.exists()) {
            println "[CarnivalGradle] external configuration: $externalConfigYamlFile"
            def pathStr = externalConfigYamlFile.canonicalPath
            project.test.systemProperty('micronaut.config.files', externalConfigYamlFile)
            project.run.systemProperty('micronaut.config.files', externalConfigYamlFile)
        }

        // dependency versions
        Locale locale = new Locale("en", "US");
        ResourceBundle versions = ResourceBundle.getBundle("carnival.gradle.Version", locale);        
        def groovyVersion = versions.getString("groovyVersion")
        def gremlinVersion = versions.getString("gremlinVersion")
        def neo4jTinkerpopVersion = versions.getString("neo4jTinkerpopVersion")
        def neo4JavaDriverVersion = versions.getString("neo4JavaDriverVersion")
        def carnivalVersion = versions.getString("carnivalVersion")

        println "[CarnivalGradle] Java version: ${System.getProperty('java.version')}"
        println "[CarnivalGradle] Groovy version: ${groovyVersion}"
        println "[CarnivalGradle] Gremlin version: ${gremlinVersion}"
        println "[CarnivalGradle] Neo4j Tinkerpop version: ${neo4jTinkerpopVersion}"
        println "[CarnivalGradle] Neo4 Java Driver version: ${neo4JavaDriverVersion}"
        println "[CarnivalGradle] Carnival version: ${carnivalVersion}"

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
            implementation("edu.upenn.pmbb:carnival-util:${carnivalVersion}")
            implementation("edu.upenn.pmbb:carnival-graph:${carnivalVersion}")
            implementation("edu.upenn.pmbb:carnival-core:${carnivalVersion}")
        }
    }
}