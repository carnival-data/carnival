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
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.core.Carnival
import carnival.core.CarnivalTinker
import carnival.core.CarnivalJanusBerkeley
import carnival.graph.VertexModel
import carnival.graph.PropertyModel
import carnival.graph.EdgeModel
import carnival.graph.Base
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod


///////////////////////////////////////////////////////////////////////////////
// SCRIPT
///////////////////////////////////////////////////////////////////////////////


// Uncomment the following to use the in-memory Tinkergraph database
Carnival cg = CarnivalTinker.create()

// Uncomment the following to use the Janus Berkeley DB graph database
//CarnivalJanusBerkeley.Config config = new CarnivalJanusBerkeley.Config()
//CarnivalJanusBerkeley.clearGraph(config)
//Carnival cg = CarnivalJanusBerkeley.create(config)


Graph graph = cg.graph
GraphTraversalSource g = graph.traversal()


@VertexModel
enum VX {
    
    // isClass tells Carnival CLASS_OF_DOGS is a class
    CLASS_OF_ALL_DOGS (
        isClass:true
    ),
    
    // Carnival assumes a definition that ends in _CLASS is a class
    COLLIE_CLASS (
        // set the superlass of COLLIE_CLASS
        superClass: CLASS_OF_ALL_DOGS
    ),

    SHIBA_INU_CLASS (
        superClass: CLASS_OF_ALL_DOGS
    ),

    SHIBA_INU (
        instanceOf: VX.SHIBA_INU_CLASS
    ),

    COLLIE (
        instanceOf: VX.COLLIE_CLASS
    )

}
cg.addModel(VX)


Vertex spotV = VX.COLLIE.instance().create(graph)
println "spotV: ${spotV} ${spotV.label()}"

Vertex spotClassV = g.V(spotV).instanceClass().next()
println "spotClassV: ${spotClassV} ${spotClassV.label()}"

List<Vertex> spotClassVs = g.V(spotV).classes().toList()
println "spotClassVs: ${spotClassVs} ${spotClassVs*.label()}"

Vertex pepperV = VX.SHIBA_INU.instance().create(graph)
println "pepperV: ${pepperV} ${pepperV.label()}"

List<Vertex> dogVs = g.V(VX.CLASS_OF_ALL_DOGS.vertex).instances().toList()
println "dogVs: ${dogVs} ${dogVs*.label()}"

List<Vertex> alsoDogVs = g.V()
    .isInstanceOf(VX.CLASS_OF_ALL_DOGS)
.toList()
println "alsoDogVs: ${alsoDogVs} ${alsoDogVs*.label()}"


cg.close()
