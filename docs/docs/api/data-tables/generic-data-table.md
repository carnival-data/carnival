---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: page
title: Generic Data Table
nav_order: 3
has_children: false
parent: Data Tables
grand_parent: Application Programmer Interface
---

# GenericDataTable

Generic data tables can hold arbitrary tabular data.

_docs/groovy/basic/generic-data-table-1.groovy_

```groovy
@Grab(group='edu.upenn.pmbb', module='carnival-util', version='0.2.6')

import carnival.util.GenericDataTable

def mdt = new GenericDataTable(
  name:"myMappedDataTable"
)


mdt.dataAdd(ID:'1A', NAME:'alex')
mdt.dataAdd(ID:'1A', NAME:'bob')

def currentDir = new File(System.getProperty("user.dir"))
mdt.writeFiles(currentDir)
```

-   The only required parameter to the GenericDataTable constructor is name.
-   This will not throw an error. The ID column has no special meaning here.
