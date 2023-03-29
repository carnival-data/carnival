package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge




class EdgeDefSpec extends Specification {


    @VertexModel
    static enum VX {
        THING,
        THING_1
    }

    @EdgeModel
    static enum EX {
    	IS_NOT(
            domain:[VX.THING], 
            range:[VX.THING_1]            
        )
    }



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared graph
    @Shared g
    


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() {
    } 

    def setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()
    }

    def cleanup() {
        if (g) g.close()
        if (graph) graph.close()
    }

    def cleanupSpec() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "lookup"() {
        when:
        Vertex v1 = VX.THING.instance().create(graph)
        Vertex v2 = VX.THING_1.instance().create(graph)
        Edge e = EX.IS_NOT.instance().from(v1).to(v2).create()
        EdgeDefinition edt = Definition.lookup(e)

        then:
        edt == EX.IS_NOT
    }

}

