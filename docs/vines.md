# Vines

Vines are data adapters that can be helpful when loading data from source systems. The principal benefit of using Vines to access your source data systems is the Vine caching mechanism.

A Vine is a logical grouping of vine methods, which are the routines that do the work of aggregating and returning data. A vine can contain any number of vine methods of any type.

There are two categories of vine methods, DataTable and JSON. DataTable vine methods use DataTable objects as their data format while JSON vine methods use JSON. The access methods and caching functionality are similar to both types.

## Vine Methods

Vine methods are functions that return data. In application code, vine method are expected to be implemented as inner classes of a Vine and must implement the VineMethod interface. There are two types of vine methods: DataTable and JSON.

## MappedDataTable Vine Methods

MappedDataTable vines return data in MappedDataTable objects.

```groovy
@Grab(group='org.pmbb', module='carnival-util', version='2.1.5')
@Grab(group='org.pmbb', module='carnival-core', version='2.1.5')

import groovy.transform.ToString
import carnival.util.MappedDataTable
import carnival.core.vine.Vine
import carnival.core.vine.MappedDataTableVineMethod
import carnival.core.vine.CacheMode

class MdtTestVine implements Vine {

    class People extends MappedDataTableVineMethod {
        MappedDataTable fetch(Map args) {
            def mdt = createDataTable(idFieldName:'ID')
            
            // normally, a data source would be queried here
            // for this example, we just create a single record
            // that includes a name taken from the arguments
            mdt.dataAdd(id:'1', name:args.p1)
            
            mdt
        }
    }
}

def vine = new MdtTestVine()
def methodCall = vine
    .method('People')
    .args(p1:'alice')
    .mode(CacheMode.IGNORE)
.call()

def currentDir = new File(System.getProperty("user.dir"))
methodCall.writeFiles(currentDir)
println "methodCall.writtenTo: ${methodCall.writtenTo}"

def mdt = methodCall.getResult()
println "mdt: $mdt"

assert mdt != null
assert mdt instanceof MappedDataTable
assert mdt.data.size() == 1
assert mdt.dataGet('1', 'name') == 'alice'

mdt.dataIterator().each {
    println "Hi ${it.name}!"
}

```
