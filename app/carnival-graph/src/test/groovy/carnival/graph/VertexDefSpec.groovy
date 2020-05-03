package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex



/**
 * gradle test --tests "carnival.graph.VertexDefSpec"
 *
 */
class VertexDefSpec extends Specification {

    static enum VX implements VertexDefTrait {
        THING,

        THING_1(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        ),

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v }}
    }


    static enum PX implements PropertyDefTrait {
        PROP_A,
        PROP_B,

        public PX() {}
        public PX(Map m) {m.each { k,v -> this."$k" = v }}
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
        VertexDefTrait d1 = VertexDef.lookup(v1)

        then:
        d1 == VX.THING
    }

}

