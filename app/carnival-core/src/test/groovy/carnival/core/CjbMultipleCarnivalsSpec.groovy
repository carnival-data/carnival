package carnival.core



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex

import org.janusgraph.graphdb.database.management.ManagementSystem
import org.janusgraph.core.schema.JanusGraphManagement
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder
import org.janusgraph.core.schema.JanusGraphIndex
import org.janusgraph.core.Connection
import org.janusgraph.core.VertexLabel
import org.janusgraph.core.EdgeLabel
import org.janusgraph.core.PropertyKey
import org.janusgraph.core.schema.SchemaStatus

import carnival.graph.*
import carnival.core.graph.*



/**
 * gradle test --tests "carnival.core.CjbMultipleCarnivalsSpec"
 *
 */
class CjbMultipleCarnivalsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // MODEL
    ///////////////////////////////////////////////////////////////////////////

    @PropertyModel 
    static enum PX {
        VOLUME(
            dataType: Float.class
        ),
        NOTE_PAD(
            cardinality: PropertyDefinition.Cardinality.LIST
        ),
        ID
    }

    @VertexModel
    static enum VX {
        SUITCASE(
            vertexProperties:[
                PX.VOLUME.withConstraints(required:true, index:true),
                PX.NOTE_PAD,
                PX.ID.withConstraints(required:true, unique:true)
            ]
        ),
        PENCIL
    }

    @EdgeModel
    static enum EX {
        CONTAINS(
            multiplicity: EdgeDefinition.Multiplicity.ONE2MANY,
            domain:[VX.SUITCASE], 
            range:[VX.PENCIL]
        ),
        IS_CONTAINED_BY(
            multiplicity: EdgeDefinition.Multiplicity.MANY2ONE
        )
    }
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() { } 

    def setup() { }

    def cleanup() { }

    def cleanupSpec() { }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "create one carnival"() {
        when:
        CarnivalJanusBerkeley.Config conf1 = new CarnivalJanusBerkeley.Config()
        conf1.storage.directory = 'data/cmc-graph1'
        Carnival c1 = CarnivalJanusBerkeley.create(conf1)

        c1.withGremlin { graph, g ->
            VX.PENCIL.instance().create(graph)
        }

        then:
        noExceptionThrown()

        cleanup:
        c1.close()
        CarnivalJanusBerkeley.clearGraph(conf1)
    }



    def "create two carnivals"() {
        when:
        CarnivalJanusBerkeley.Config conf1 = new CarnivalJanusBerkeley.Config()
        conf1.storage.directory = 'data/cmc-graph1'
        Carnival c1 = CarnivalJanusBerkeley.create(conf1)

        c1.withGremlin { graph, g ->
            VX.PENCIL.instance().create(graph)
        }

        then:
        noExceptionThrown()

        when:
        CarnivalJanusBerkeley.Config conf2 = new CarnivalJanusBerkeley.Config()
        conf2.storage.directory = 'data/cmc-graph2'
        Carnival c2 = CarnivalJanusBerkeley.create(conf2)

        c2.withGremlin { graph, g ->
            VX.PENCIL.instance().create(graph)
            VX.PENCIL.instance().create(graph)
        }

        then:
        noExceptionThrown()

        when:
        def numPencils1
        c1.withGremlin { graph, g ->
            numPencils1 = g.V().isa(VX.PENCIL).count().next()
        }
        def numPencils2
        c2.withGremlin { graph, g ->
            numPencils2 = g.V().isa(VX.PENCIL).count().next()
        }

        then:
        numPencils1 == 1
        numPencils2 == 2

        cleanup:
        c1.close()
        CarnivalJanusBerkeley.clearGraph(conf1)
        c2.close()
        CarnivalJanusBerkeley.clearGraph(conf2)
    }



}

