# Create a Library

## Directions

### Publish Carnival to your local maven repository
- Download the code for Carnival
- Run publishToMavenLocal() to publish the libraries to your local maven repository

### Use Gradle Init to create an empty library

Follow the [Gradle instructions](https://docs.gradle.org/current/samples/sample_building_groovy_applications.html) to create a Groovy library project.

Prompt | Selection
--- | ---
type of project | library
implementation language | Groovy
build script DSL | Groovy 

### Add Carnival Gradle Plugin to Buildscript
Add the Carnival Gradle plugin to the buildscript.

```groovy
buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath group:'org.carnival', name:'carnival-gradle', version:'2.1.1-SNAPSHOT'
    }    
}
```

### Apply necessary plugins
In addition the groovy plugin and any other plugins already included from `gradle init`, the following plugins are required.  The Carnival gradle plugin currently requires the `apply` keyword. 

```groovy
plugins {
    id 'com.github.ManifestClasspath' version '0.1.0-RELEASE'
}
apply plugin: 'carnival.library'
```

### Add mavenLocal() to repositories

```
repositories {
    mavenLocal()
}
```


### Test the build
Run a `clean` to test the build configuration.

```
./gradlew clean
```

If the Carnival plugin is properly applied, you should see version information that looks something like the following.

```
[CarnivalLibrary] Java version: 11.0.11
[CarnivalLibrary] Groovy version: 3.0.7
[CarnivalLibrary] Gremlin version: 3.4.10
[CarnivalLibrary] Neo4j Tinkerpop version: 0.9-3.4.0
[CarnivalLibrary] Neo4 Java Driver version: 4.1.1
[CarnivalLibrary] Carnival version: 2.1.0-SNAPSHOT
```
