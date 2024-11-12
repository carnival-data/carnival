package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.*
import carnival.core.CarnivalTinker



/**
 * 
 *
 */
class EdgeConstraintSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared carnival
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() { } 

    def setup() {
        carnival = CarnivalTinker.create()
    }

    def cleanup() {
        if (carnival) carnival.graph.close()
    }

    def cleanupSpec() { }


    ///////////////////////////////////////////////////////////////////////////
    // MODEL
    ///////////////////////////////////////////////////////////////////////////

    @PropertyModel
    static enum PX {
        PROP_A, PROP_B
    }

    @EdgeModel
    static enum EX {
        REL_A,
        REL_B(            
            propertyDefs:[
                PX.PROP_A.withConstraints(index:true),
                PX.PROP_B
            ]
        )
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "create with property def"() {
        when:
        EdgeConstraint ec1 = EdgeConstraint.create(EX.REL_B)

        then:
        ec1.label == EX.REL_B.label
        ec1.properties

        when:
        EdgePropertyConstraint epc = ec1.properties.find({ 
            it.name == PX.PROP_A.label
        })

        then:
        epc
    }
    

    
}

