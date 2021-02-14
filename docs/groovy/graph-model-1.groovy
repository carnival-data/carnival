///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('org.pmbb:carnival-core:2.0.1-SNAPSHOT')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.8')


///////////////////////////////////////////////////////////////////////////////
// IMPORTS
///////////////////////////////////////////////////////////////////////////////

import groovy.transform.ToString
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.graph.CoreGraphTinker
import carnival.graph.VertexDefinition
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod


///////////////////////////////////////////////////////////////////////////////
// SCRIPT
///////////////////////////////////////////////////////////////////////////////

def cg = CoreGraphTinker.create()
println """\
An empty graph should have no model errors.
model: ${cg.checkModel()}
"""

cg.graph.addVertex('Thing')
println """\
We have added a vertex with the unmodeled label 'Thing'
model: ${cg.checkModel()}
"""

@VertexDefinition(global="true")
enum VX1 {
    THING
}
VX1.THING.instance().create(cg.graph)
println """\
We have created a model for 'Thing', but not yet incorporated it into the
graph model.  So, we will still see a model error.
model: ${cg.checkModel()}
"""

cg.addDefinitions(VX1)
println """\
We have added the model for 'Thing' to the graph model.  So, there are no
more errors.
model: ${cg.checkModel()}
"""

@VertexDefinition
enum VX2 {
    THING,
    ANOTHER_THING
}
VX2.THING.instance().create(cg.graph)
VX2.ANOTHER_THING.instance().create(cg.graph)
println """\
We have created a new model, which includes 'Thing', but is not global. We can
have both global and non-global models.  However, global models supercede non-
global models.  Here we see that the VX2.THING that we create in the graph does
not cause a model error, due to the global model.  However, VX2.ANOTHER_THING 
does cause an error.
model: ${cg.checkModel()}
"""

cg.addDefinitions(VX2)
println """\
Now that we have added the VX2 model to our graph model, there are no model
errors.
model: ${cg.checkModel()}
"""





/*
class GmsTestMethods implements GraphMethods {

    class TestGraphMethod extends GraphMethod {
        public Map execute(Graph graph, GraphTraversalSource g) {
            VX.SOME_THING.instance().create(graph)
        }
    }

    class TestGraphMethodThrowsException extends GraphMethod {
        public Map execute(Graph graph, GraphTraversalSource g) {
            throw new Exception('boom')
        }
    }

}

println "HELLOOOOO"
*/
