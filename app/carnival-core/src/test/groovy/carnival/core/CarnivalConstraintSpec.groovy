package carnival.core



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.*
import carnival.core.graph.*



/**
 *
 */
class CarnivalConstraintSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // MODELS
    ///////////////////////////////////////////////////////////////////////////

    @PropertyModel 
    static enum PX {
        VOLUME(
            dataType: Float.class
        ),
        NOTE_PAD(
            cardinality: PropertyDefinition.Cardinality.LIST
        )
    }

    @VertexModel
    static enum VX {
        SUITCASE(
            propertyDefs:[
                PX.VOLUME,
                PX.NOTE_PAD
            ]
        )
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared carnival

    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setup() {
        carnival = CarnivalTinker.create()
        carnival.addModel(PX)
        carnival.addModel(VX)
    }

    def setupSpec() { } 


    def cleanupSpec() { }


    def cleanup() {
        if (carnival) carnival.close()
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "property constraints"() {
        expect:
        carnival.graphSchema
        carnival.graphSchema.propertyConstraints
        carnival.graphSchema.propertyConstraints.size() == 2

        PX.VOLUME instanceof PropertyDefinition
        PX.VOLUME.dataType == Float.class

        PX.NOTE_PAD instanceof PropertyDefinition
        PX.NOTE_PAD.dataType == String.class
        PX.NOTE_PAD.cardinality == PropertyDefinition.Cardinality.LIST
    }


    def "vertex constraints"() {
        expect:
        carnival.graphSchema
        carnival.graphSchema.vertexConstraints
        carnival.graphSchema.vertexConstraints.size() == 15
    }
    
    
}

