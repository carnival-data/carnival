# Graph Methods

Carnival defines an API for graph methods, which provide a standardized way to implement transformations of the graph with provenance.  When a graph method runs, it is represented in the graph as a process with optional inputs and outputs.  By representing not just data, but the operations that create and mutate data, Carnival provides a more wholistic view of the information present in the system.  Furthermore, by tracking graph method invocations in the graph, Carnival can offer a data builder API that includes methods to run a process if it has not already been run or assert that a process has already been run.

```groovy
@Grab(group='org.pmbb', module='carnival-util', version='2.1.1-SNAPSHOT')
@Grab(group='org.pmbb', module='carnival-core', version='2.1.1-SNAPSHOT')

import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod

def vine = new MdtTestVine()

class DemoMethods implements GraphMethods { 

    class AddPeople extends GraphMethod {

        void execute(Graph graph, GraphTraversalSource g) {

            def mdt = vine
                .method('People')
                .args(p1:'alice')
                .call()
            .result

            mdt.data.values().each { rec ->
                log.trace "rec: ${rec}"
                DemoModel.VX.PERSON.instance()
                    .withNonNullMatchingProperties(rec)
                .ensure(graph, g)
            }

        }

    }

}

```




