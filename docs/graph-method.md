# Graph Methods

Carnival defines an API for graph methods, which provide a standardized way to implement transformations of the graph with provenance.  When a graph method runs, it is represented in the graph as a process with optional inputs and outputs.  By representing not just data, but the operations that create and mutate data, Carnival provides a more wholistic view of the information present in the system.  Furthermore, by tracking graph method invocations in the graph, Carnival can offer a data builder API that includes methods to run a process if it has not already been run or assert that a process has already been run.


## GraphMethods

GraphMethods is a trait that can be applied to a class what will contain graph methods.  GraphMethods provides a mechanism to call the graph methods in a standardized way.  However, while it is recommended, it is not necessary to contain graph methods inside a class with the GraphMethods.  

```groovy
class MyGraphMethods implements GraphMethods {                      (1)

    class SomeGraphMethod extends GraphMethod {                       
        public void execute(Graph graph, GraphTraversalSource g) {  (2)
            // graph operations go here
        }
    }
    
}

def gms = new MyGraphMethods()                                      (3)
def gmc = gms
    .method('SomeGraphMethod')                                      (4)
.call(graph, g)
```
1. Apply the GraphMethods trait to MyGraphMethods
2. SomeGraphMethod can be invoked by name
3. Create an instance of MyGraphMethods
4. Call the graph method SomeGraphMethod


## GraphMethod
GraphMethod is an abstract class with a simple interface that graph methods must implement.  GraphMethod provides the functionality to track graph operations in the graph and implements singleton functionality analogous to that of the graph API.  

A GraphMethod can be contained within a GraphMethods class or be implemented independently.

```groovy
static class TestGraphMethod extends GraphMethod {
    public void execute(Graph graph, GraphTraversalSource g) {
        // graph operations
    }
}

def gm = new TestGraphMethod()

// assumes that the graph and a graph traversal source are available
gm.call(graph, g)
```

For the remainder of this documentation, we will assume that the GraphMethod is implemented inside a GraphMethods class, which is the recommended approach.


### GraphMethod

```groovy
class MyGraphMethods implements GraphMethods {                      
    class SomeGraphMethod extends GraphMethod {                       
        public void execute(graph, g) {
            // graph operations go here
        }
    }
}

def gms = new MyGraphMethods()                                      
def gmc = gms
    .method('SomeGraphMethod')                                      (1)
.call(graph, g)

```
1. Call SomeGraphMethod by name with no arguments


### GraphMethod arguments

```groovy
class MyGraphMethods implements GraphMethods {                      
    class SomeGraphMethod extends GraphMethod {                       
        public void execute(graph, g) {
            if (arguments != null) {                                (1)
                arguments instanceof Map
            }
        }
    }
}

def gms = new MyGraphMethods()                                      
def gmc = gms
    .method('SomeGraphMethod')
    .arguments(a:1)                                                 (2)
.call(graph, g)
```
1. Arguments are made available within the execute() method as a map.  If there are no arguments, the arguments variable will equal null.
2. Pass the arguments `[a:1]` to SomeGraphMethod


## GraphMethod provenance

### Process vertex
```groovy
class MyGraphMethods implements GraphMethods {                      
    class SomeGraphMethod extends GraphMethod {                       
        public void execute(graph, g) {
            // graph operations go here
        }
    }
}

def gms = new MyGraphMethods()                                      
def gmc = gms
    .method('SomeGraphMethod')
.call(graph, g)

def procV = gmc.processVertex       (1)
Core.VX.GRAPH_PROCESS.isa(procV)    (2)
```
1. The process vertex is available in the GraphMethodCall object returned by call()
2. The process vertex is an instance of carnival.core.graph.Core.VX.GRAPH_PROCESS

### Process properties

```groovy
class MyGraphMethods implements GraphMethods {                      
    class SomeGraphMethod extends GraphMethod {                       
        public void execute(graph, g) {
            // graph operations go here
        }
    }
}

def gms = new MyGraphMethods()                                      
def gmc = gms
    .method('SomeGraphMethod')
.call(graph, g)

Core.PX.NAME.of(procV).isPresent()                  (1)
Core.PX.START_TIME.of(procV).isPresent()            
Core.PX.STOP_TIME.of(procV).isPresent()             

!Core.PX.EXCEPTION_MESSAGE.of(procV).isPresent()    (2)
```
1. The `NAME`, `START_TIME`, and `STOP_TIME` of the process are stored as properties in the process vertex.
2. If an exception was thrown inside the execute() method, the exception message will be stored as a property of the process vertex.



### Process lookup

Processes can be retreived from the graph using the GraphMethod API.


```groovy
class MyGraphMethods implements GraphMethods {                      
    class SomeGraphMethod extends GraphMethod {                       
        public void execute(graph, g) {
            // graph operations go here
        }
    }
}

def gms = new MyGraphMethods()                                      

gms.method('SomeGraphMethod').processes(g).size() == 0                  (1)

gms.method('SomeGraphMethod').call(graph, g)                            (2)
gms.method('SomeGraphMethod').processes(g).size() == 1                  (3)

gms.method('SomeGraphMethod').arguments(a:1).call(graph, g)             (4)
gms.method('SomeGraphMethod').processes(g).size() == 1                  (5)
gms.method('SomeGraphMethod').arguments(a:1).processes(g).size() == 1   (6)
```
1. No processes have been run to start
2. Call SomeGraphMethod with no arguments
3. One process will be found
4. Call SomeGraphMethod with some arguments
5. There is still only the single process called with no arguments
6. There is now a process called with the same arguments


### Custom process names

Carnival calculates a default name for a process based on the class name.  The name() method can be used to override the default name of a process.

```groovy
class MyGraphMethods implements GraphMethods {                      
    class SomeGraphMethod extends GraphMethod {                       
        public void execute(graph, g) {
            // graph operations go here
        }
    }
}

def gms = new MyGraphMethods()                                      

gms.method('SomeGraphMethod').call(graph, g)                                
gms.method('SomeGraphMethod').processes(g).size() == 1                      

gms.method('SomeGraphMethod').name('custom-name').call(graph, g)            (1)
gms.method('SomeGraphMethod').processes(g).size() == 1                      (2)
gms.method('SomeGraphMethod').name('custom-name').processes(g).size() == 1  (3)
```
1. Set a custom name for the process
2. One process called with no arguments and no custom name
3. One process called with no arguments and the custom name
