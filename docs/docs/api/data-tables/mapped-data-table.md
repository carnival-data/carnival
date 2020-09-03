---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
title: Mapped Data Table
nav_order: 2
has_children: false
parent: Data Tables
grand_parent: API Documentation
---

# MappedDataTable

_Example: Mapped data table_

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
@Grab(group='edu.upenn.pmbb', module='carnival-util', version='0.2.6')

import carnival.util.MappedDataTable

def mdt = new MappedDataTable(
    name:"myMappedDataTable",
    idFieldName:'ID'
)

mdt.dataAdd(ID:'1A', NAME:'alex') mdt.dataAdd(ID:'1A', NAME:'bob')
```

This will cause an exception, since the ID '1A' was already added to the data table.
