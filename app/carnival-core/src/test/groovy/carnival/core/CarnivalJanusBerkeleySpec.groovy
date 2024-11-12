package carnival.core



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import org.janusgraph.core.schema.JanusGraphManagement
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder
import org.janusgraph.core.schema.JanusGraphIndex
import org.janusgraph.core.Connection
import org.janusgraph.core.VertexLabel
import org.janusgraph.core.EdgeLabel
import org.janusgraph.core.PropertyKey

import carnival.graph.*
import carnival.core.graph.*



/**
 * gradle test --tests "carnival.core.CarnivalJanusBerkeleySpec"
 *
 */
class CarnivalJanusBerkeleySpec extends Specification {

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

    @Shared CarnivalJanusBerkeley.Config config
    @Shared Carnival carnival


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() { } 

    def setup() {
        config = new CarnivalJanusBerkeley.Config()
        CarnivalJanusBerkeley.clearGraph(config)
        carnival = CarnivalJanusBerkeley.create(config)
    }

    def cleanup() {
        carnival.close()
    }

    def cleanupSpec() { }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "property uniqueness"(){
        when:
        carnival.addModel(PX)
        carnival.addModel(VX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()

        String idxName = CarnivalJanusBerkeley.indexNameOf(
            VX.SUITCASE, PX.ID
        )
        println "idxName: ${idxName}"
        JanusGraphIndex idx = mgmt.getGraphIndex(idxName)

        mgmt.rollback()

        then:
        idx
        idx.isUnique()
    }


    def "vertex isclass namespace compound indices"() {
        when:
        carnival.addModel(PX)
        carnival.addModel(VX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()

        String idxName = CarnivalJanusBerkeley.indexNameOf(
            VX.SUITCASE, Base.PX.IS_CLASS, Base.PX.NAME_SPACE
        )
        println "idxName: ${idxName}"
        JanusGraphIndex idx = mgmt.getGraphIndex(idxName)

        mgmt.rollback()

        then:
        idx
    }


    def "vertex namespace compound indices"() {
        when:
        carnival.addModel(PX)
        carnival.addModel(VX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()

        String idxName = CarnivalJanusBerkeley.indexNameOf(
            VX.SUITCASE, Base.PX.NAME_SPACE, PX.VOLUME
        )
        println "idxName: ${idxName}"
        JanusGraphIndex idx = mgmt.getGraphIndex(idxName)

        mgmt.rollback()

        then:
        idx
    }


    def "isclass and namespace are indexed"() {
        when:
        JanusGraphManagement mgmt = carnival.graph.openManagement()

        String idxName = CarnivalJanusBerkeley.indexNameOf(
            Base.PX.IS_CLASS, Base.PX.NAME_SPACE
        )
        println "idxName: ${idxName}"
        JanusGraphIndex idx = mgmt.getGraphIndex(idxName)

        mgmt.rollback()

        then:
        idx
    }


    def "namespace is indexed"() {
        when:
        JanusGraphManagement mgmt = carnival.graph.openManagement()

        String idxName = 'NameSpace0CarnivalGraphBasePx'
        JanusGraphIndex idx = mgmt.getGraphIndex(idxName)

        mgmt.rollback()

        then:
        idx
    }


    def "vertex property index"() {
        when:
        carnival.addModel(PX)
        carnival.addModel(VX)
        carnival.addModel(EX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()

        String idxName = VX.SUITCASE.label + '-' + PX.VOLUME.label
        JanusGraphIndex idx = mgmt.getGraphIndex(idxName)

        mgmt.rollback()

        then:
        idx

    }


    def "vertex mapped connections"() {
        when:
        carnival.addModel(PX)
        carnival.addModel(VX)
        carnival.addModel(EX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()
        Set<VertexLabel> vertexLabels = mgmt.getVertexLabels().toSet()

        VertexLabel suitcaseLabel = vertexLabels.find {
            it.name() == VX.SUITCASE.label
        }
        Set<Connection> cs = suitcaseLabel.mappedConnections().toSet()
        mgmt.rollback()

        then:
        suitcaseLabel
        cs
        cs.size() == 1
    }


    def "vertex mapped properties"() {
        when:
        carnival.addModel(PX)
        carnival.addModel(VX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()
        Set<VertexLabel> vertexLabels = mgmt.getVertexLabels().toSet()

        VertexLabel suitcaseLabel = vertexLabels.find {
            it.name() == VX.SUITCASE.label
        }
        Set<PropertyKey> pks = suitcaseLabel.mappedProperties().toSet()
        mgmt.rollback()

        then:
        suitcaseLabel
        pks
        pks.size() == 3
    }


    def "property cardinality"() {
        when:
        carnival.addModel(PX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()
        Set<PropertyKey> propertyKeys = mgmt.getRelationTypes(PropertyKey).toSet()
        PropertyKey volumeKey = propertyKeys.find {
            it.name() == PX.VOLUME.label
        }
        org.janusgraph.core.Cardinality volumeCardinality = volumeKey.cardinality()
        PropertyKey notePadKey = propertyKeys.find {
            it.name() == PX.NOTE_PAD.label
        }
        org.janusgraph.core.Cardinality notePadCardinality = notePadKey.cardinality()
        mgmt.rollback()

        then:
        volumeKey
        volumeCardinality == org.janusgraph.core.Cardinality.SINGLE
        notePadCardinality == org.janusgraph.core.Cardinality.LIST
    }


    def "property data type"() {
        when:
        carnival.addModel(PX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()
        Set<PropertyKey> propertyKeys = mgmt.getRelationTypes(PropertyKey).toSet()
        PropertyKey volumeKey = propertyKeys.find {
            it.name() == PX.VOLUME.label
        }
        Class volumeDataType = volumeKey.dataType()
        PropertyKey notePadKey = propertyKeys.find {
            it.name() == PX.NOTE_PAD.label
        }
        Class notePadDataType = notePadKey.dataType()
        mgmt.rollback()

        then:
        volumeKey
        volumeDataType == Float.class
        notePadDataType == String.class
    }


    def 'edge multiplicity'() {
        when:
        // addModel() calls openManagement and commit()
        carnival.addModel(EX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()
        Set<EdgeLabel> edgeLabels = mgmt.getRelationTypes(EdgeLabel).toSet()
        EdgeLabel containsLabel = edgeLabels.find {
            it.name() == EX.CONTAINS.label
        }
        org.janusgraph.core.Multiplicity clm = containsLabel.multiplicity()
        mgmt.rollback()

        then:
        containsLabel
        clm == org.janusgraph.core.Multiplicity.ONE2MANY
    }


    def 'edge label'() {
        when:
        // addModel() calls openManagement and commit()
        carnival.addModel(VX)
        carnival.addModel(EX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()
        Set<EdgeLabel> edgeLabels = mgmt.getRelationTypes(EdgeLabel).toSet()

        println "edgeLabels: ${edgeLabels}"

        edgeLabels.each { EdgeLabel vl ->
            println "vl: ${vl.label()} ${vl.name()}"
        }

        String lbl = EX.CONTAINS.label.toString()
        println "lbl: ${lbl}"

        EdgeLabel containsLabel = edgeLabels.find {
            it.name() == lbl
        }
        mgmt.rollback()

        then:
        containsLabel
    }


    def 'add model vertex'() {
        when:
        // addModel() calls openManagement and commit()
        carnival.addModel(VX)

        JanusGraphManagement mgmt = carnival.graph.openManagement()
        Set<VertexLabel> vertexLabels = mgmt.getVertexLabels().toSet()

        println "vertexLabels: ${vertexLabels}"

        vertexLabels.each { VertexLabel vl ->
            println "vl: ${vl.label()} ${vl.name()}"
        }

        String lbl = VX.SUITCASE.label.toString()
        println "lbl: ${lbl}"

        VertexLabel suitcaseLabel = vertexLabels.find {
            it.name() == lbl
        }
        mgmt.rollback()

        then:
        suitcaseLabel
    }


    def 'test basic ops'() {
        when:
        carnival.addModel(VX)
        carnival.withGremlin { graph, g ->
            carnival.addGraphSchemaVertices(graph, g)
        }

        then:
        carnival

        when:
        def numScs
        carnival.withGremlin { graph, g ->
            numScs = g.V().isa(VX.SUITCASE).count().next()
        }

        then:
        numScs == 0

        when:
        def sc1
        carnival.withGremlin { graph, g ->
            sc1 = VX.SUITCASE.instance().withProperties(
                PX.ID, 'id1',
                PX.VOLUME, 1
            ).create(graph)
        }

        then:
        sc1

        when:
        def sc2
        carnival.withGremlin { graph, g ->
            sc2 = g.V().isa(VX.SUITCASE).next()
        }

        then:
        sc2
        sc2.id() == sc1.id()
        sc2 == sc1
    }


    def 'test create'() {
        expect:
        carnival
    }

}

