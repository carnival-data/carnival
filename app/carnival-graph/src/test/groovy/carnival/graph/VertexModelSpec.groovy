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



/**
 * gradle test --tests "carnival.graph.VertexModelSpec"
 *
 */
class VertexModelSpec extends Specification {

    @VertexModel
    static enum VX {
        THING_1(PX)

        /*VX(Class aClass) {
            println "aClass: ${aClass}"
            if (aClass.isEnum()) {
                aClass.values().each {
                    this.propertyDefs.add(it) 
                } 
            } 
        }*/

        /*VX(Enum propertyDefEnum) {
            println "aString: ${aString}"
            println "anInteger: ${anInteger}"
            propertyDefEnum.values().each {
                this.propertyDefs.add(it) 
            } 
        }*/

        /*VX(Class enumClass) {
            if (enumClass.isEnum()) {
                def vals = enumClass.values()
                for (int i=0; i<vals.size(); i++) {
                    this.propertyDefs.add(vals[i])
                }
            }            
        }*/
    }


    /*@EdgeModel
    static enum EX {
    	IS_NOT(
            domain:[VX.THING], 
            range:[VX.THING_1]            
        )
    }*/


    @PropertyModel
    static enum PX {
        PROP_A,
        PROP_B,
        PROP_C
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



    def "vertex props"() {
        expect:
        VX.THING_1.propertyDefs != null
        VX.THING_1.propertyDefs.size() == 3
    }

}

