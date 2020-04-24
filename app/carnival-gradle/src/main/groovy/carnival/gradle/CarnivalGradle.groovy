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

        project.test.systemProperty('carnival.home', appHomeDirectory)
        project.run.systemProperty('carnival.home', appHomeDirectory)

        def logbackConfig = "${appHomeDirectory}/config/logback.xml"
        File logbackConfigFile = new File(logbackConfig)
        if (!logbackConfigFile.exists()) {
            println "[CarnivalGradle] WARNING: ${logbackConfig} does not exist. Using default logging configurations."
            return
        }
        project.test.systemProperty('logback.configurationFile', logbackConfigFile)
        project.run.systemProperty('logback.configurationFile', logbackConfigFile)
    }
}