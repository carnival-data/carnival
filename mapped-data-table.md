# MappedDataTable

The MappedDataTable supports tabular data where each row of data is identified by a single unique key

_Example: Mapped data table_

```groovy
@Grab(group='org.pmbb', module='carnival-util', version='2.0.1-SNAPSHOT')

import carnival.util.MappedDataTable

def mdt = new MappedDataTable(
  name:"myMappedDataTable",
  idFieldName:'ID'
)
mdt.dataAdd(ID:'1A', NAME:'alex')

def currentDir = new File(System.getProperty("user.dir"))
mdt.writeFiles(currentDir)
```

-   Use `@Grab` to incorporate the carnival-util dependency.
-   All data tables have a name, which will be used to name file representations.
-   Set the name of the identifier field of this data table to 'ID'.
-   Add a record to the data table.
-   Write the file representation of this data table to the current directory.

The result of this script will be two files in the current directory:

-   myMappedDataTable.yaml: data descriptor
-   myMappedDataTable.csv: the data in comma separated value format

As noted above, mapped data tables have a primary key, which is enforced to be unique.

_Example: Non-unique identifiers_

```groovy
@Grab(group='org.pmbb', module='carnival-util', version='0.2.6')

import carnival.util.MappedDataTable

def mdt = new MappedDataTable(
    name:"myMappedDataTable",
    idFieldName:'ID'
)

mdt.dataAdd(ID:'1A', NAME:'alex') mdt.dataAdd(ID:'1A', NAME:'bob')
```

This will cause an exception, since the ID '1A' was already added to the data table.
