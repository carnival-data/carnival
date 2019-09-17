# Carnival Core
carnival-core contains all the carnival code except for the Gremlin DSL java code which can be found in [carnival-dsl](../carnival-dsl/README.md).  carnival-core is probably not the best name for this module as it contains code specific to the Penn Medicine Biobank.  It seems likely that there will be a refactor in the future.

## Contents
1. [Gradle](#gradle)
1. [Testing](#testing)
1. [Gremlin Console](#gremlin-console)
1. [Vines](#vines)
1. [Grapes](#grapes)


<a name="gradle"></a>
## Gradle 
```
gradle clean
gradle compileGroovy
```

```
gradle runExample -Pdatasource=/Volumes/phi/queries/issue23_cad_dna-regeneron/pmbb-pds-dbconfig.yaml -Puuids=data/example-packet-uuids.csv
```

```
gradle whereClauseChunker -Pfieldname=PACKET_UUID -Pfile=data/example-packet-uuids.csv -Pchunksize=10
```


<a name="testing"></a>
## Testing

### Quick Commands

#### All Tests

```
gradle -Dtest.http=true test
```

#### All Tests -- No external resources required

```
gradle test
```

### Single Tests
By default, gradle will run all tests.  

To run a single test, the `test.single` flag can be used.

```
gradle -Dtest.single=MappedDataTableSpec test
```

`--tests` can work in a similar way and can also be used to run all tests at a package level.

```
gradle test --tests "carnival.core.util.CodeRefGroupSpec"
```

```
gradle test --tests "carnival.core.util.*"
```


### HTTP
Some of the tests require external HTTP resources.  To run these tests:

```
gradle -Dtest.http=true ...
```


### Neo4j Graph
Some tests create and use a Neo4j graph.  By default convention, these tests will run in a transation and call `graph.tx().rollback()` to roll back the transaction at the end of testing.  To disable this functionality and instead call `graph.tx().commit()`, set the `test.graph.rollback` flag to false:

```
gradle -Dtest.graph.rollback=false ...
```


<a name="gremlin-console"></a>
## Gremlin Console
Gremlin Console must currently be built from source ([instructions](https://github.com/pennbiobank/carnival/issues/34)).

```
graph = Neo4jGraph.open('/Users/augustearth/dev/carnival/carnival-core/data/graph/neo4j')
g = graph.traversal()
```

To use the CarnivalTraversal, build carnival-dsl.jar and copy it to the gremlin-console lib/ directory.

```
graph = Neo4jGraph.open('/Users/augustearth/dev/carnival/carnival-core/data/graph/neo4j')
g = graph.traversal(carnival.pmbb.graph.CarnivalTraversalSource.class)
```

<a name="vines"></a>
## Vines
### Relational database configuration

```
url: jdbc:oracle:thin:@uphsvlndc069.uphs.upenn.edu:1521/pdsprd.uphs.upenn.edu
user: BIOBANK
password: ...
driver: oracle.jdbc.pool.OracleDataSource
```

<a name="connectivity"></a>
## Connectivity

### SSH Tunnels

*PMACS VPN is required*

Resource | Tunnel
--- | ---
SPS | `ssh -l <username> -L 7791:sps-prd-db.pmacs.upenn.edu:3306 willow.pmacs.upenn.edu`
Pumpkin | `ssh -l <username> -L 7792:loam.pmacs.upenn.edu:3306 willow.pmacs.upenn.edu`
PDS | `ssh -l <username> -L 7799:170.166.29.121:1521 willow.pmacs.upenn.edu`
ORQID | `ssh -l <username> -L 7798:170.166.29.122:1433 willow.pmacs.upenn.edu`


## Command Line Distribution


### CARNIVAL_HOME
CARNIVAL_HOME is an environment variable that is expected to be the directory where Carnival is installed. The directory structure is:

```
carnival-cli
  bin
  config (optional)
  data
  libs
```

Directory | Contents
--------- | ---------
carnival-cli/bin | command line executables
carnival-cli/config | optional global configuration
carnival-cli/data | global data directories
carnival-cli/libs | java libraries required for execution

### CARNIVAL_LOCAL
CARNIVAL_LOCAL is the user's local directory where user-specific configuration, output files, and databases files are stored.

```
carnival
    config
    data
    log
    target
```

Directory | Contents
--------- | ---------
carnival/config | local configuration including data source credentials
carnival/data | local data including database files
carnival/target | output files


### Data
The data directory, which can appear at a global and local level, has the following sub-directories:

```
data
  h2
  reference
  source
  cache
  graph
```


<a name="grapes"></a>
## Grapes (deprecated)
### run with grapes logging turned up

```
groovy -Dgroovy.grape.report.downloads=true -Divy.message.logger.level=4 -cp ./ojdbc6.jar pmbb-pds.groovy /Volumes/phi/queries/issue23_cad_dna-regeneron/pmbb-pds-dbconfig.txt
```

### hard coded classpath with oracle jar

```
groovy -cp ./ojdbc6.jar pmbb-pds-example.groovy /Volumes/phi/queries/issue23_cad_dna-regeneron/pmbb-pds-dbconfig.yaml
```

