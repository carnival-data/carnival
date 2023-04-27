# Create a Gradle Groovy Application

Create a Groovy application that used the Carnival library using Gradle as the build tool.

## Directions


### Use Gradle Init to create an empty application

Follow the [Gradle instructions](https://docs.gradle.org/current/samples/sample_building_groovy_applications.html) to create a Groovy application project.

Prompt | Selection
--- | ---
type of project | application
implementation language | Groovy
build script DSL | Groovy 

*You will be required to give your application a name and default package.  The rest of these instructions assume the name carnival-demo-application.*

### Edit the Gradle Build
Apply the following edits to `app/build.gradle`.


#### Apply the Carnival plugin

In addition the groovy plugin and any other plugins already included from `gradle init`, the following plugins are required.  

```Gradle
plugins {
    id "io.github.carnival-data.carnival" version "3.0.0"
}
```

On a Windows computer, the following plugin may be necessary to build with Gradle:

```Gradle
plugins {
    id 'com.github.ManifestClasspath' version '0.1.0-RELEASE'
}
```

### Test the build
Run a `clean` to test the build configuration.

```Shell
./gradlew clean
```

If the Carnival plugin is properly applied, you should see version information that looks something like the following.

```
[Carnival] Java version: 11.0.15
[Carnival] Groovy version: 3.0.9
[Carnival] Gremlin version: 3.4.10
[Carnival] Neo4j Tinkerpop version: 0.9-3.4.0
[Carnival] Neo4 Java Driver version: 4.1.1
[Carnival] Carnival version: 3.0.0
```

### Test the app

You should now be ready to run the full lifecycle of Gradle commands.  

```
./gradlew compileGroovy
./gradlew test
```

### Fun With Carnival

Add the following to **build.gradle**:

```Gradle
test {
    testLogging {
        outputs.upToDateWhen {false}
        showStandardStreams = true
    }
}
```

Add the following to **App.groovy**:

```Groovy
void funWithCarnival() {
    Carnival carnival = CarnivalTinker.create()
    carnival.withTraversal { graph, g ->
        g.V().each { v -> println "vertex: ${v} ${v.label}"}
        g.E().each { e -> println "edge: ${e} ${e.label}"}
    }
}
```

Add the following to **AppTest.groovy**:

```Groovy
def "fun with carnival"() {
    setup:
    def app = new App()

    when:
    app.funWithCarnival()

    then:
    noExceptionThrown()
}
```

Re-run the tests:

```Shell
./gradlew test
```

The initial set of vertices should be printed to the console.  To learn what can be done with Carnival, please see the [Reference Documentation](index.md#reference-docs)
