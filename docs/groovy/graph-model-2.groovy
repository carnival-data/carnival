///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('io.github.carnival-data:carnival-core:3.0.1')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.10')


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
import carnival.graph.PropertyModel
import carnival.graph.EdgeModel
import carnival.graph.Base
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod


///////////////////////////////////////////////////////////////////////////////
// SCRIPT
///////////////////////////////////////////////////////////////////////////////

def cg = CarnivalTinker.create()
def graph = cg.graph

@PropertyModel
enum PX {
    IS_ALIVE
}

@VertexModel
enum VX {
    PERSON (
        propertyDefs:[
            PX.IS_ALIVE.withConstraints(index:true)
        ]
    )
}
cg.addModel(VX)

Vertex person1 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)

println """\
person1 label : ${person1.label()}
person1 name space : ${Base.PX.NAME_SPACE.valueOf(person1)}
person1 is alive : ${PX.IS_ALIVE.valueOf(person1)}
"""

@EdgeModel
enum EX {
    IS_FRIENDS_WITH(
        domain:[VX.PERSON],
        range:[VX.PERSON]
    )
}
cg.addModel(EX)


Vertex person2 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)
Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).create()

println """\
person1 label : ${edge1.label()}
edge1 name space : ${Base.PX.NAME_SPACE.valueOf(edge1)}
"""
