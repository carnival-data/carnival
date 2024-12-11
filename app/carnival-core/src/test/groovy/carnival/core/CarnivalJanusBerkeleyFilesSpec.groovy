package carnival.core



import java.nio.file.*
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
 * gradle test --tests "carnival.core.CarnivalJanusBerkeleySpec"
 *
 */
class CarnivalJanusBerkeleyFilesSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
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
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() { } 

    def setup() {
        /*
        config = new CarnivalJanusBerkeley.Config()
        CarnivalJanusBerkeley.clearGraph(config)
        carnival = CarnivalJanusBerkeley.create(config)
        */
    }

    def cleanup() {
        //carnival.close()
    }

    def cleanupSpec() { }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "create opens existing graph dir"() {
        when:
        CarnivalJanusBerkeley.Config config = new CarnivalJanusBerkeley.Config()
        CarnivalJanusBerkeley.clearGraph(config)
        Carnival carnival = CarnivalJanusBerkeley.create(config)

        def numVerts1
        carnival.withGremlin { graph, g ->
            numVerts1 = g.V().count().next()
        }

        then:
        numVerts1

        when:
        carnival.close()
        carnival = CarnivalJanusBerkeley.create(config)
        def numVerts2
        carnival.withGremlin { graph, g ->
            numVerts2 = g.V().count().next()
        }

        then:
        carnival
        numVerts1 == numVerts2

        cleanup:
        carnival.close()
    }


    def "failOnExisting fails if graph dir exists"() {
        when:
        CarnivalJanusBerkeley.Config config = new CarnivalJanusBerkeley.Config()
        String graphDir = config.storage.directory
        Path graphPath = Paths.get(graphDir)
        if (!Files.isDirectory(graphPath)) Files.createDirectory(graphPath)

        then:
        Files.isDirectory(graphPath)

        when:
        Carnival carnival = CarnivalJanusBerkeley.create(
            config, [failOnExisting:true]
        )

        then:
        Exception e = thrown()
    }


    def "create uses expected graph dir"() {
        when:
        CarnivalJanusBerkeley.Config config = new CarnivalJanusBerkeley.Config()
        String graphDir = config.storage.directory
        Path graphPath = Paths.get(graphDir)
        if (Files.isDirectory(graphPath)) CarnivalJanusBerkeley.clearGraph(config)

        then:
        !Files.isDirectory(graphPath)

        when:
        Carnival carnival = CarnivalJanusBerkeley.create(config)

        then:
        Files.isDirectory(graphPath)

        cleanup:
        carnival.close()
    }


    def "clear deletes graph dir"() {
        when:
        CarnivalJanusBerkeley.Config config = new CarnivalJanusBerkeley.Config()
        String graphDir = config.storage.directory
        Path graphPath = Paths.get(graphDir)
        if (!Files.isDirectory(graphPath)) Files.createDirectory(graphPath)

        then:
        Files.isDirectory(graphPath)

        when:
        CarnivalJanusBerkeley.clearGraph(config)

        then:
        !Files.isDirectory(graphPath)
    }


    def "config has non-null graph dir"() {
        when:
        CarnivalJanusBerkeley.Config config = new CarnivalJanusBerkeley.Config()
        String graphDir = config.storage.directory

        then:
        graphDir
    }


}

