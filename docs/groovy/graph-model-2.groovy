///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('org.carnival:carnival-core:2.1.0-SNAPSHOT')
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
import carnival.graph.PropertyDefinition
import carnival.graph.EdgeDefinition
import carnival.graph.EdgeDefinition
import carnival.graph.Base
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod


///////////////////////////////////////////////////////////////////////////////
// SCRIPT
///////////////////////////////////////////////////////////////////////////////

def cg = CoreGraphTinker.create()
def graph = cg.graph

@PropertyDefinition
enum PX {
    IS_ALIVE
}

@VertexDefinition
enum VX {
    PERSON (
        propertyDefs:[
            PX.IS_ALIVE.withConstraints(index:true)
        ]
    )
}
cg.addDefinitions(VX)

Vertex person1 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)

println """\
person1 label : ${person1.label()}
person1 name space : ${Base.PX.NAME_SPACE.valueOf(person1)}
person1 is alive : ${PX.IS_ALIVE.valueOf(person1)}
"""

@EdgeDefinition
enum EX {
    IS_FRIENDS_WITH(
        domain:[VX.PERSON],
        range:[VX.PERSON]
    )
}
cg.addDefinitions(EX)


Vertex person2 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)
Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).create()

println """\
person1 label : ${edge1.label()}
edge1 name space : ${Base.PX.NAME_SPACE.valueOf(edge1)}
"""
