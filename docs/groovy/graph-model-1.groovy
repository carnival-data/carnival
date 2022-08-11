///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('io.github.carnival-data:carnival-core:2.1.1-SNAPSHOT')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.10')


///////////////////////////////////////////////////////////////////////////////
// IMPORTS
///////////////////////////////////////////////////////////////////////////////

import groovy.transform.ToString
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.core.graph.CoreGraphTinker
import carnival.graph.VertexDefinition
import carnival.graph.EdgeDefinition
import carnival.graph.EdgeDefinition
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod


///////////////////////////////////////////////////////////////////////////////
// SCRIPT
///////////////////////////////////////////////////////////////////////////////

def cg = CoreGraphTinker.create()
def graph = cg.graph
println """\
An empty graph should have no model errors.
model: ${cg.checkModel()}
"""

graph.addVertex('Thing')
println """\
We have added a vertex with the unmodeled label 'Thing'
model: ${cg.checkModel()}
"""

@VertexDefinition(global="true")
enum VX1 {
    THING
}
Vertex thing1V = VX1.THING.instance().create(graph)
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
Vertex thing2V = VX2.THING.instance().create(graph)
VX2.ANOTHER_THING.instance().create(graph)
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

@EdgeDefinition
enum EX1 {
    IS_NOT(
        domain:[VX1.THING], 
        range:[VX2.THING]            
    )
}
Edge isNot1E = EX1.IS_NOT.instance().from(thing1V).to(thing2V).create()
println """\
We have modeled an edge/relationship IS_NOT, where the domain is restricted to
VX1.THING vertices and the domain is restricted to VX2.THING vertices.  We have
created an edge between two of the vertices we have previously created.  Since
we have not added this model to our graph model, we will see a model error.
model: ${cg.checkModel()}
"""

cg.addDefinitions(EX1)
println """\
Now that we have added the EX1 model to our graph model, there are no model
errors.
model: ${cg.checkModel()}
"""
