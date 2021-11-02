# GenericDataTable

Generic data tables can hold arbitrary tabular data.

_docs/groovy/basic/generic-data-table-1.groovy_

```groovy
@Grab(group='org.pmbb', module='carnival-util', version='0.2.6')

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
