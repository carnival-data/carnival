# Carnival

## Overview

**Carnival** is a data unification technology that aggregates disparate data into a unified property
graph resource. Inspired by Open Biological and Biomedical Ontology (OBO) Foundry ontologies, the
**Carnival** data model supports the execution of common investigatory tasks including patient cohort
identification, case-control matching, and the production of data sets for scientific analysis.

### Key Facts

-   Powered by Groovy
-   Vines extract data
-   Reapers add data to the graph
-   Reasoners apply logical rules
-   Algorithms compute and stratify
-   Sowers export data

## Developer Setup
Carnival is a Groovy multi-project that uses Gradle as the build engine. The main project is in the app directory. Folders within app contain the sub-projects (eg. app/public/carnival-core, app/public/carnival-gremlin-dsl). Every sub-project has a build.gradle configuration file that defines its dependencies and the gradle tasks that it can execute. The build.gradle file in the app directory defines project-wide configuration. Gradle task commands run from the root project filter down to the sub-projects. Gradle commands referenced in this documentation are assumed to be called from the root project directory carnival/app unless otherwise noted. This project also includes a Docker image configuration that can be built to run Carnival or the test suite.

### Installation

#### Build Requirements
- Gradle V5+
- Installation with a package manager
- SDKMan
- Windows: Cygwin + SDKMan
- Note - Cygwin needs curl installed for sdkman to work successfully. The windows 10 version of curl may cause an error when trying to install gradle. See stack overflow.
- Windows: Scoop
- Install Manually
  - Java V1.8+
  - Neo4j V3.5+ (Optional) - Helpful to browse the property graph. Desktop Community Server.

#### Environment Setup
1. Clone the repository using:
```
git clone git@github.com:pennbiobank/carnival-public.git
```

2. Create a carnival home directory. This directory will contain your configuration files and will be the location where carnival reads and writes graphs and files.
```
mkdir /Users/myuser/dev/carnival/carnival_home
```

3. Make carnival aware of your carnival home directory. There are a couple of ways you can do this.
  - Use the environment variable CARNIVAL_HOME. For example:
```
export CARNIVAL_HOME=/Users/myuser/dev/carnival/carnival_home
```
  - Pass your carnival home directory to future gradle commands using the -D syntax:
```
-Dcarnival.home=/Users/myuser/dev/carnival/carnival_home
```
  - **Note** - If you use this method, then you will need to edit logback.xml, which by default references CARNIVAL_HOME.
  
4. Setup the configuration files in the `${CARNIVAL_HOME}/config` directory:
  - Copy the config template files from `carnival/config` to `${CARNIVAL_HOME}/config`.
  - Rename the files named `*.*-template` to `*.*` (remove -template from the name).
  - **Note** - In the config files windows paths should be specified using double forward-slashes (i.e. `C://Users//myuser//somedirectory`).
  - File descriptions:
  - application.yaml - Contains data source information (i.e. credentals to relational dbs, RDF dbs, REDCap, etc.), the default vine cache-mode, local directory configuration and the gremlin configuration.
  - logback.xml - Can be modified to change the log levels.

5. Install APOC neo4j plugin:
Download the APOC V3.5.0.7 and save it to a directory on your local file system.
Add the path to that directory in gremlin section of the application.yaml config file in the entry gremlin:neo4j:conf:dbms:directories:plugins:

```yaml
# gremlin
gremlin:
 neo4j:
   conf:
     dbms:
       directories:
         plugins: /Users/myuser/Documents/Neo4j/default.graphdb/plugins
       security:
         auth_enabled: "false"      
         procedures:
           unrestricted: apoc.*
           whitelist: apoc.*
```

## Data Tables

Carnival contains a DataTable class with two sub-classes, MappedDataTable and GenericDataTable.
MappedDataTable is like a database table with a primary key. The value of the primary key field of each row
is enforced to be unique within the scope of the data table. GenericDataTable is like a database table with
no primary key. For data interchange, it is safer to use MappedDataTable, assuming your data set has a
primary key. If there is no primary key, use GenericDataTable.

### MappedDataTable

#### Example: Mapped data table

```groovy
@Grab(group='edu.upenn.pmbb', module='carnival-util', version='0.2.6')

import carnival.util.MappedDataTable

def mdt = new MappedDataTable(
  name:"myMappedDataTable",
  idFieldName:'ID'
)
mdt.dataAdd(ID:'1A', NAME:'alex')

def currentDir = new File(System.getProperty("user.dir"))
mdt.writeFiles(currentDir)
```

- Use `@Grab` to incorporate the carnival-util dependency.
- All data tables have a name, which will be used to name file representations.
- Set the name of the identifier field of this data table to 'ID'.
- Add a record to the data table.
- Write the file representation of this data table to the current directory.
