///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('org.carnival:carnival-core:2.1.1-SNAPSHOT')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.10')


///////////////////////////////////////////////////////////////////////////////
// IMPORTS
///////////////////////////////////////////////////////////////////////////////

import groovy.transform.ToString
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

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
Graph graph = cg.graph
GraphTraversalSource g = graph.traversal()


@VertexDefinition
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
        instanceOf:VX. SHIBA_INU_CLASS
    ),

    COLLIE (
        instanceOf:VX. COLLIE_CLASS
    )

}
cg.addDefinitions(VX)


Vertex spotV = VX.COLLIE.instance().create(graph)
println "spotV: ${spotV} ${spotV.label()}"

Vertex spotClassV = g.V(spotV)
    .out(Base.EX.IS_INSTANCE_OF)
.next()
println "spotClassV: ${spotClassV} ${spotClassV.label()}"

List<Vertex> spotClassVs = g.V(spotV)
    .out(Base.EX.IS_INSTANCE_OF)
    .has(Base.PX.IS_CLASS, true)
    .emit()
    .repeat(__.out(Base.EX.IS_SUBCLASS_OF))
.toList()
println "spotClassVs: ${spotClassVs} ${spotClassVs*.label()}"


Vertex pepperV = VX.SHIBA_INU.instance().create(graph)
println "pepperV: ${pepperV} ${pepperV.label()}"

List<Vertex> dogVs = g.V(VX.CLASS_OF_ALL_DOGS.vertex)
    .emit()
    .repeat(
        __.in(Base.EX.IS_SUBCLASS_OF)
    )
    .in(Base.EX.IS_INSTANCE_OF)
.toList()
println "dogVs: ${dogVs} ${dogVs*.label()}"
