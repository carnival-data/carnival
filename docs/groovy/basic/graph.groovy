@Grab(group='edu.upenn.pmbb', module='carnival-graph', version='0.2.6')



import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.VertexDefTrait
import carnival.graph.PropertyDefTrait



/** vertex definitions */
enum VX implements VertexDefTrait {
    THING_1,

    THING_2(
        vertexProperties:[
            PX.PROP_A
        ]
    ),

    THING_3(
        vertexProperties:[
            PX.PROP_A.withConstraints(required:true)
        ]
    ),

    private VX() {}
    private VX(Map m) {m.each { k,v -> this."$k" = v }}
}


/** property definitions */
enum PX implements PropertyDefTrait {
    PROP_A,
    PROP_B,

    public PX() {}
    public PX(Map m) {m.each { k,v -> this."$k" = v }}
}


/** helper method to debug print a vertex to standard out */
def printVert = { vertex ->
	def str = "$vertex ${vertex.label} "
	vertex.keys().each { propKey ->
		//str += "$propKey "
		str += vertex.property(propKey)
		str += " "
	}
	println str
}


// open an in-memory graph and graph traversal
def graph = TinkerGraph.open()
def g = graph.traversal()

// graph is empty to start
assert g.V().count().next() == 0

def v

// create a THING_1 with no properties
v = VX.THING_1.instance().createVertex(graph)
assert g.V().count().next() == 1
printVert(v)

// create a THING_2 with no properties
v = VX.THING_2.instance().createVertex(graph)
assert g.V().count().next() == 2
printVert(v)

// create a THING_2 with a single optional property
v = VX.THING_2.instance().withProperty(PX.PROP_A, 'a').createVertex(graph)
assert g.V().count().next() == 3
printVert(v)

// try to create a THING_3 with no properties
// it will fail, because THING_3 has a required property
try {
    v = VX.THING_3.instance().createVertex(graph)
    fail 'an exception will be thrown before we reach this line of code'
} catch (Exception e) {
    println e.message
}

// no new vertices were created
assert g.V().count().next() == 3


// close the graph resources
g.close()
graph.close()