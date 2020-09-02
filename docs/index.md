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
