# Carnival Gradle Plugin

The Carnival Gradle Plugin facilites the creation of applications build using Gradle, such as Micronaut services. It does the following:

-   Sets the `carnival.home` property of the test and run tasks.
-   Sets the logback configuration property of the test and run tasks to: `${home}/config/logback.xml`.

Carnival will automatically look for an application configuration file in `${home}/config/application.pml`.

The plugin looks for an environment variable based on the `rootProject.name` of your project by transforming it from kebab-case to SNAKE_CASE with \_HOME appended. For example, if your project name is `my-project`, the plugin will look for an environment variable named `MY_PROJECT_HOME` which must contain the full path of the home directory to use.

The plugin can be built from source and deployed to your local maven repository by:

```
./gradlew :carnival-gradle:publishToMavenLocal
```

To use the plugin, add something like the following to `build.gradle`:

```
buildscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath group:'edu.upenn.pmbb', name:'carnival-gradle', version:'0.2.7'
    }
}
apply plugin: 'edu.upenn.pmbb.carnival'
```
