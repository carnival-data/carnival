///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('io.github.carnival-data:carnival-core:5.0.2-SNAPSHOT')
//@Grab('io.github.carnival-data:carnival-core:3.0.1')
@Grab('org.apache.tinkerpop:gremlin-core:3.7.2')


///////////////////////////////////////////////////////////////////////////////
// IMPORTS
///////////////////////////////////////////////////////////////////////////////

import groovy.transform.ToString
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.core.CarnivalTinker
import carnival.graph.VertexModel
import carnival.graph.EdgeModel
import carnival.graph.PropertyModel
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod


///////////////////////////////////////////////////////////////////////////////
// SCRIPT
///////////////////////////////////////////////////////////////////////////////

def cg = CarnivalTinker.create()
def graph = cg.graph
println """\
An empty graph should have no model errors.
model: ${cg.checkModel()}
"""
//System.console().readLine 'Return to continue'


//
// Vertex model definition
//
@VertexModel
enum VX1 {
    THING
}

Vertex thing1V = VX1.THING.instance().create(graph)
println """\
We have added a vertex with the type VX.THING, but not yet incorporated the 
model into the Carnival, so there will be model errors.
model: ${cg.checkModel()}
"""

cg.addModel(VX1)
println """\
We have added the model for 'Thing' to the graph model.  So, there are no
more errors.
model: ${cg.checkModel()}
"""

@VertexModel
enum VX2 {
    THING,
    ANOTHER_THING
}
cg.addModel(VX2)
Vertex thing2V = VX2.THING.instance().create(graph)
VX2.ANOTHER_THING.instance().create(graph)
println """\
There are no model errors for these new modeled vertices.
model: ${cg.checkModel()}
"""


@EdgeModel
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

cg.addModel(EX1)
println """\
Now that we have added the EX1 model to our graph model, there are no model
errors.
model: ${cg.checkModel()}
"""
