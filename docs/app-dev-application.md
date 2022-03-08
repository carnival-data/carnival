# Create an application

## Directions

### Publish Carnival to your local maven repository
- Download the code for Carnival
- Run publishToMavenLocal() to publish the libraries to your local maven repository

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

#### Add Carnival Gradle Plugin to Buildscript
Add the Carnival Gradle plugin to the buildscript section.

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

#### Apply necessary plugins
In addition the groovy plugin and any other plugins already included from `gradle init`, the following plugins are required.  The Carnival gradle plugin currently requires the `apply` keyword. 

```groovy
plugins {
    id 'com.github.ManifestClasspath' version '0.1.0-RELEASE'
}
apply plugin: 'carnival.application'
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
[CarnivalApplication] root project name: carnival-demo-application
[CarnivalApplication] environment variable: CARNIVAL_DEMO_APPLICATION_HOME
[CarnivalApplication] WARNING: CARNIVAL_DEMO_APPLICATION_HOME is not set. Using default configurations.
[CarnivalLibrary] Java version: 11.0.11
[CarnivalLibrary] Groovy version: 3.0.7
[CarnivalLibrary] Gremlin version: 3.4.10
[CarnivalLibrary] Neo4j Tinkerpop version: 0.9-3.4.0
[CarnivalLibrary] Neo4 Java Driver version: 4.1.1
[CarnivalLibrary] Carnival version: 2.1.0-SNAPSHOT
```

Notice the warning that `CARNIVAL_DEMO_APPLICATION_HOME` is not set.  Carnival applications require a home directory in order to run.  The home directory contains configuration and working directories.

### Create a home directory
Create an empty directory outside of your gradle project directory.  Create the following directory structure and files inside the empty directory.  The recommended name for the directory is `carnival_demo_application_home`, but it can be named whatever you wish.

```
config/
  application.yml
  logback.xml
```

#### applictaion.yml

```
carnival-demo-application:
  graph:
    runtime: "neo4j"
    test: "tinker"
carnival:
  cache-mode: OPTIONAL
```


#### logback.xml

```
<configuration>

    <property name="LOG_DIR" value="${CARNIVAL_DEMO_APPLICATION_HOME}/log"/>

    <!-- appenders -->

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <withJansi>true</withJansi>
        <!-- encoders are assigned the type
             ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
        <encoder>
            <pattern>%cyan(%d{HH:mm:ss.SSS}) %green([%thread]) %highlight(%-5level) %magenta(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="CARNIVAL" class="ch.qos.logback.core.FileAppender">
        <file>${LOG_DIR}/carnival.log</file>
      <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
      </encoder>
    </appender> 

    <!-- loggers -->

    <!-- java -->
    <logger name="javax.management" level="INFO"/>
    <logger name="org.reflections" level="WARN"/>    

    <!-- groovy -->
    <logger name="groovy.util" level="INFO"/>

    <!-- netty -->
    <logger name="org.jboss.netty" level="INFO"/>
    <logger name="com.ning.http.client" level="INFO"/>
    <logger name="io.netty" level="INFO"/>

    <!-- micronaut -->
    <logger name="io.micronaut" level="INFO"/>

    <!-- reactor -->
    <logger name="reactor" level="INFO"/>

    <!-- hibernate -->
    <logger name="org.hibernate" level="INFO"/>

    <!-- neo4j -->
    <logger name="org.neo4j" level="INFO"/>

    <!-- carnival -->
    <logger name="console" level="TRACE">
      <appender-ref ref="STDOUT" />
    </logger>
    <logger name="carnival" level="TRACE">
      <appender-ref ref="CARNIVAL" />
    </logger>
    <logger name="carnival.util.Defaults" level="INFO"/>
    <logger name="carnival.core.graph.CoreGraph" level="INFO"/>

    <!-- with no appender-ref, log statements will flow to the root logger. the
         level set here will be respected.  so, if the level here is trace, but
         the root logger level is info, a trace message will still flow to the
         root logger appender. -->
    <logger name="example.carnival.micronaut" level="TRACE"/>

    <root level="TRACE">
        <appender-ref ref="STDOUT" />
    </root>
    
</configuration>

```

### Set the application environment variable

Set an environment variable to point the application to the home directory.

#### .nix
```
export CARNIVAL_DEMO_APPLICATION_HOME=/path/to/carnival_demo_application_home
```
 
### Test the build again
Run a `clean` to test the build configuration.

```
./gradlew clean
```

If the environment variable is set property, you should see that the warning is no longer present and the application home directory is listed.

```
[CarnivalApplication] root project name: carnival-demo-application
[CarnivalApplication] environment variable: CARNIVAL_DEMO_APPLICATION_HOME
[CarnivalApplication] application home directory: /Users/birtwell/data/carnival_demo_application_home
[CarnivalApplication] external configuration: /Users/birtwell/data/carnival_demo_application_home/config/application.yml
[CarnivalApplication] Java version: 11.0.11
[CarnivalApplication] Groovy version: 3.0.7
[CarnivalApplication] Gremlin version: 3.4.10
[CarnivalApplication] Neo4j Tinkerpop version: 0.9-3.4.0
[CarnivalApplication] Neo4 Java Driver version: 4.1.1
[CarnivalApplication] Carnival version: 2.1.1-SNAPSHOT
```

#### Test compile

You should now be ready to run the full lifecycle of Gradle commands.  

```
./gradlew compileGroovy
```
