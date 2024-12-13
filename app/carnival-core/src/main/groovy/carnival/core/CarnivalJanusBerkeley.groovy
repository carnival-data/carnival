package carnival.core



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import groovy.transform.ToString
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import org.apache.commons.io.FileUtils

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import org.janusgraph.core.JanusGraph
import org.janusgraph.core.JanusGraphFactory
import org.janusgraph.core.PropertyKey
import org.janusgraph.core.VertexLabel
import org.janusgraph.core.EdgeLabel
import org.janusgraph.core.Multiplicity
import org.janusgraph.core.Cardinality
import org.janusgraph.core.schema.SchemaStatus
import org.janusgraph.core.schema.EdgeLabelMaker
import org.janusgraph.core.schema.JanusGraphManagement
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder
import org.janusgraph.core.schema.JanusGraphIndex
import org.janusgraph.core.schema.PropertyKeyMaker
import org.janusgraph.core.schema.VertexLabelMaker
import org.janusgraph.graphdb.database.management.ManagementSystem


import carnival.core.graph.DefaultGraphSchema
import carnival.core.graph.DefaultGraphValidator
import carnival.core.graph.GraphSchema
import carnival.core.graph.GraphValidator
import carnival.core.graph.GraphValidationError
import carnival.core.graph.GremlinTraitUtilities
import carnival.core.graph.EdgeConstraint
import carnival.core.graph.PropertyConstraint
import carnival.core.graph.VertexConstraint
import carnival.core.graph.VertexPropertyConstraint
import carnival.graph.Base
import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.ElementDefinition
import carnival.graph.VertexDefinition





/** 
 * A Carnival with an underlying Tinkergraph implementation.
 */
@InheritConstructors
@Slf4j
class CarnivalJanusBerkeley extends Carnival {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** CarnivalJanusBerkeley configuration */
    Config config

    /** The names of the indexes created */
    Set<String> indexNames = new HashSet<String>()

    /** */
    boolean openedExisting = false


	///////////////////////////////////////////////////////////////////////////
	// FACTORY 
	///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a ready-to-use CarnivalJanusBerkeley object.
     * @return A CarnivalJanusBerkeley object.
     */
    public static CarnivalJanusBerkeley create(Map args = [:]) {
        Config config = new Config()
        create(config, args)
    }



    /** 
     * Create a ready-to-use CarnivalJanusBerkeley object.
     * @param config The CarnivalJanusBerkeley.Config
     * @param args Optional arguments
     * @param args.failOnExisting Fail if the graph directory already exists
     * @return A CarnivalJanusBerkeley object.
     */
    public static CarnivalJanusBerkeley create(Config config, Map args = [:]) {
		log.info "CarnivalJanusBerkeley create args:$args"

        assert config
        assert config.storage.directory

        String graphDir = config.storage.directory
        Path graphPath = Paths.get(graphDir)
        boolean graphDirExists = Files.isDirectory(graphPath)
        if (graphDirExists && args.failOnExisting) {
            DirectoryExistsException exception = new DirectoryExistsException(
                "graph directory exists: ${graphDir}"
            )
            exception.graphDirectoryPath = graphDir
            throw exception
        }
        
        //if (Files.isDirectory(graphPath)) Files.createDirectory(graphPath)
        //Files.isDirectory(graphPath)

        JanusGraph gremlinGraph = JanusGraphFactory.build().
            set("storage.backend", "berkeleyje").
            set("storage.directory", config.storage.directory).
            set("storage.transactions", config.storage.transactions).
        open();

        def transactionsAreSupported = gremlinGraph
            .features()
            .graph()
        .supportsTransactions()
        assert transactionsAreSupported

		def graphSchema
        if (args.vertexBuilders) graphSchema = new DefaultGraphSchema(
            args.vertexBuilders
        )
        else graphSchema = new DefaultGraphSchema()

        def graphValidator = new DefaultGraphValidator()
        def carnival = new CarnivalJanusBerkeley(
            gremlinGraph, 
            graphSchema, 
            graphValidator
        )
        carnival.config = config

        if (graphDirExists) {

            carnival.openedExisting = true
            carnival.initPropModelNoMgmt()
            carnival.initVertModelNoMgmt()
            carnival.initEdgeModelNoMgmt()

        } else {

            carnival.initPropModel()
            carnival.initVertModel()
            carnival.initEdgeModel()

        }

		return carnival
    }


    /** */
    public List<AddModelResult> initPropModelNoMgmt() {
        log.info "Carnival initPropModel"

        List<AddModelResult> results = new ArrayList<AddModelResult>()

        AddModelResult amrBase
        AddModelResult amrCore

        withGremlin { graph, g ->
            amrBase = addPropertyModelNoMgmt(Base.PX)
            amrCore = addPropertyModelNoMgmt(Core.PX)
        }

        results.add(amrBase)
        results.add(amrCore)

        return results
    }


    /** */
    public List<AddModelResult> initPropModel() {
        log.info "Carnival initPropModel"

        List<AddModelResult> results = new ArrayList<AddModelResult>()

        AddModelResult amrBase
        AddModelResult amrCore

        withGremlin { graph, g ->
            amrBase = addPropertyModelNoMgmt(Base.PX)
            amrCore = addPropertyModelNoMgmt(Core.PX)
        }

        results.add(amrBase)
        results.add(amrCore)

        JanusGraphManagement mgmt = graph.openManagement()
        List<String> idxNames = new ArrayList<String>()

        Exception exception

        try {

            idxNames.addAll(
                janusPropertySchema(amrBase.propertyConstraints, mgmt)
            )
            idxNames.addAll(
                janusPropertySchema(amrCore.propertyConstraints, mgmt)
            )
            idxNames.addAll(
                janusSchemaManagementGlobal(mgmt)
            )

        } catch (Exception e) {
            exception = e
            log.error "error creating janus schema", e
            log.warn "rolling back janus management"
            try {
                mgmt.rollback()
            } catch (Exception e2) {
                log.warn "error rolling back janus schema", e2
            }
            throw e
        } finally {
            if (!exception) mgmt.commit()
        }
        log.trace "initPropModel idxNames: ${idxNames}"

        // wait for index availability
        idxNames.each { idxName ->
            log.trace "initPropModel await registered ${idxName}"
            ManagementSystem
                .awaitGraphIndexStatus(graph, idxName)
                .status(SchemaStatus.ENABLED)
            .call()
        }

        return results
    }


    /** */
    public AddModelResult addPropertyModelNoMgmt(Class<ElementDefinition> defClass) {
        log.trace "addPropertyModelNoMgmt defClass:${defClass}"

        assert defClass

        AddModelResult res

        def defInterfaces = defClass.getInterfaces()
        assert defInterfaces.contains(PropertyDefinition)
        res = addPropertyModel(defClass)
    }


    /** */
    public List<AddModelResult> initVertModel() {
        log.info "Carnival initVertModel"
        List<AddModelResult> results = new ArrayList<AddModelResult>()
        [Base.EX, Core.VX].each {
            AddModelResult res = addModel(it)
            results.add(res)
        }
        results
    } 


    /** */
    public List<AddModelResult> initVertModelNoMgmt() {
        log.info "Carnival initVertModel"
        List<AddModelResult> results = new ArrayList<AddModelResult>()
        [Base.EX, Core.VX].each {
            AddModelResult res = addModelNoMgmt(it)
            results.add(res)
        }
        results
    } 


    /** */
    public List<AddModelResult> initEdgeModelNoMgmt() {
        log.info "Carnival initEdgeModel"
        List<AddModelResult> results = new ArrayList<AddModelResult>()
        [Core.EX].each {
            AddModelResult res = addModelNoMgmt(it)
            results.add(res)
        }
        results
    } 


    /** */
    public List<AddModelResult> initEdgeModel() {
        log.info "Carnival initEdgeModel"
        List<AddModelResult> results = new ArrayList<AddModelResult>()
        [Core.EX].each {
            AddModelResult res = addModel(it)
            results.add(res)
        }
        results
    } 


    ///////////////////////////////////////////////////////////////////////////
    // SCHEMA MANAGEMENT
    ///////////////////////////////////////////////////////////////////////////


    /** */
    public List<String> janusSchemaManagementGlobal(JanusGraphManagement mgmt) {
        log.trace "janusSchemaManagementGlobal mgmt"

        assert mgmt

        List<String> idxNames = new ArrayList<String>()

        // create indexes for all Base.PX properties
        Set<PropertyKey> propertyKeys = mgmt.getRelationTypes(PropertyKey).toSet()
        
        EnumSet.allOf(Base.PX).each { PropertyDefinition bpx ->
            PropertyKey pk = propertyKeys.find {
                it.name() == bpx.label
            }
            assert pk
            String idxName = indexNameOf(bpx) 
            idxNames.add(idxName)
            this.indexNames.add(idxName)
            mgmt
                .buildIndex(idxName, Vertex.class)
                .addKey(pk)
            .buildCompositeIndex()
        }

        // combo index for isClass and nameSpace
        PropertyKey icpk = propertyKeys.find {
            it.name() == Base.PX.IS_CLASS.label
        }
        assert icpk
        PropertyKey nspk = propertyKeys.find {
            it.name() == Base.PX.NAME_SPACE.label
        }
        assert nspk
        String inIdxName = indexNameOf(Base.PX.IS_CLASS, Base.PX.NAME_SPACE)
        idxNames.add(inIdxName)
        this.indexNames.add(inIdxName)
        mgmt
            .buildIndex(inIdxName, Vertex.class)
            .addKey(icpk)
            .addKey(nspk)
        .buildCompositeIndex()

        return idxNames
    }


    /** */
    public void janusSchemaManagement(AddModelResult amr) {
        assert amr

        if (amr.propertyConstraints) {
            janusPropertySchema(amr.propertyConstraints)
        }

        if (amr.vertexConstraints) {
            janusVertexSchema(amr.vertexConstraints)
        }

        if (amr.edgeConstraints) {
            janusEdgeSchema(amr.edgeConstraints)
        }
    }


    /** */
    public void janusPropertySchema(Set<PropertyConstraint> propertyConstraints) {
        log.trace "janusPropertySchema propertyConstraints"

        assert propertyConstraints
        assert graph

        graph.tx().rollback()

        JanusGraphManagement mgmt = graph.openManagement()
        List<String> idxNames

        try {

            idxNames = janusPropertySchema(propertyConstraints, mgmt)

        } catch (Exception e) {
            log.error "error creating janus property schema", e
            log.warn "rolling back janus management"
            mgmt.rollback()
        } finally {
            mgmt.commit()
        }

        // wait for index availability
        idxNames.each { idxName ->
            log.trace "janusPropertySchema await registered ${idxName}"
            ManagementSystem
                .awaitGraphIndexStatus(graph, idxName)
                .status(SchemaStatus.ENABLED)
            .call()
        }
    }



    /** */
    public List<String> janusPropertySchema(
        Set<PropertyConstraint> propertyConstraints,
        JanusGraphManagement mgmt
    ) {
        log.trace "janusPropertySchema propertyConstraints mgmt"

        assert propertyConstraints
        assert graph
        assert mgmt

        List<String> idxNames = new ArrayList<String>()

        // look for namespace property key
        Set<PropertyKey> propertyKeys = mgmt.getRelationTypes(PropertyKey).toSet()
        PropertyKey nspk = propertyKeys.find {
            it.name() == Base.PX.NAME_SPACE.label
        }

        propertyConstraints.each { PropertyConstraint pc ->
            log.trace "pc: ${pc}"

            // add property key
            PropertyKeyMaker pkm = mgmt
                .makePropertyKey(pc.label)
            .dataType(pc.dataType)

            if (pc.cardinality) {
                Cardinality pcc = Enum.valueOf(
                    Cardinality,
                    pc.cardinality.name()
                )
                assert pcc
                pkm = pkm.cardinality(pcc)
            }
            PropertyKey pk = pkm.make()

            // add composite index with namespace
            if (nspk) {
                String nsPkIdxName = indexNameOf(Base.PX.NAME_SPACE, pc.propertyDef)
                idxNames.add(nsPkIdxName)
                this.indexNames.add(nsPkIdxName)
                mgmt
                    .buildIndex(nsPkIdxName, Vertex.class)
                    .addKey(nspk)
                    .addKey(pk)
                .buildCompositeIndex()
            }
        }

        return idxNames
    }


    /** */
    public void janusEdgeSchema(Set<EdgeConstraint> edgeConstraints) {
        log.trace "janusEdgeSchema"

        assert edgeConstraints
        assert graph

        graph.tx().rollback()

        JanusGraphManagement mgmt = graph.openManagement()

        try {
            Set<VertexLabel> vertexLabels = mgmt.getVertexLabels().toSet()

            edgeConstraints.each { EdgeConstraint ec ->
                log.trace "ec: ${ec}"
                
                EdgeLabelMaker elm = mgmt.makeEdgeLabel(ec.label)
                if (ec.multiplicity) {
                    Multiplicity jm = Enum.valueOf(
                        Multiplicity, 
                        ec.multiplicity.name()
                    )
                    assert jm
                    elm = elm.multiplicity(jm)
                }
                EdgeLabel el = elm.make()

                if (ec.domainLabels && ec.rangeLabels) {
                    ec.domainLabels.each { dl ->
                        VertexLabel dll = vertexLabels.find {
                            it.name() == dl
                        }

                        ec.rangeLabels.each { rl ->
                            VertexLabel rll = vertexLabels.find {
                                it.name() == rl
                            }

                            if (dll && rll) {
                                mgmt.addConnection(el, dll, rll)
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error "error creating janus edge schema", e
            log.warn "rolling back janus management"
            mgmt.rollback()
        } finally {
            mgmt.commit()
        }
    }


    /** */
    public void janusVertexSchema(Set<VertexConstraint> vertexConstraints) {
        log.trace "janusVertexSchema"

        assert vertexConstraints
        assert graph

        graph.tx().rollback()

        JanusGraphManagement mgmt = graph.openManagement()
        List<String> idxNames = new ArrayList<String>()

        try {
            Set<PropertyKey> propertyKeys = mgmt.getRelationTypes(PropertyKey).toSet()

            // find Base.PX.NAME_SPACE property key
            PropertyKey nspk = propertyKeys.find {
                it.name() == Base.PX.NAME_SPACE.label
            }
            assert nspk

            // find Base.PX.IS_CLASS property key
            PropertyKey icpk = propertyKeys.find {
                it.name() == Base.PX.IS_CLASS.label
            }
            assert icpk

            vertexConstraints.each { VertexConstraint vc ->
                log.trace "vc: ${vc}"

                VertexLabelMaker vlm = mgmt.makeVertexLabel(vc.label)
                VertexLabel vl = vlm.make()

                // combo isclass and namespace 
                String icnsIdxName = indexNameOf(
                    vc.vertexDef, Base.PX.IS_CLASS, Base.PX.NAME_SPACE
                )
                log.trace "icnsIdxName: ${icnsIdxName}"
                idxNames.add(icnsIdxName)
                this.indexNames.add(icnsIdxName)
                mgmt
                    .buildIndex(icnsIdxName, Vertex.class)
                    .addKey(icpk)
                    .addKey(nspk)
                    .indexOnly(vl)
                .buildCompositeIndex()

                vc.properties.each { VertexPropertyConstraint vpc ->
                    Set<PropertyKey> pks = propertyKeys.findAll {
                        it.name() == vpc.name
                    }
                    if (!pks) {
                        log.warn "could not find Janus property key(s) with name: ${vpc.name}"
                        return
                    }
                    assert pks.size() == 1
                    
                    PropertyKey pk = pks.first()

                    // add the property
                    mgmt.addProperties(vl, pk)

                    // property index
                    String idxName = indexNameOf(vc, vpc)
                    log.trace "idxName: ${idxName}"
                    idxNames.add(idxName)
                    this.indexNames.add(idxName)
                    IndexBuilder ib = mgmt
                        .buildIndex(idxName, Vertex.class)
                        .addKey(pk)
                    .indexOnly(vl)
                    if (vpc.unique) ib = ib.unique()
                    JanusGraphIndex jgi = ib.buildCompositeIndex()

                    // combo namespace and vpc index
                    String comboIdxName = indexNameOf(
                        vc.vertexDef, Base.PX.NAME_SPACE, vpc.propertyDef
                    )
                    log.trace "comboIdxName: ${comboIdxName}"
                    idxNames.add(comboIdxName)
                    this.indexNames.add(comboIdxName)
                    mgmt
                        .buildIndex(comboIdxName, Vertex.class)
                        .addKey(nspk)
                        .addKey(pk)
                        .indexOnly(vl)
                    .buildCompositeIndex()

                }

            }
        } catch (Exception e) {
            log.error "error creating janus vertex schema", e
            log.warn "rolling back janus management"
            mgmt.rollback()
        } finally {
            mgmt.commit()
        }

        // wait for index availability
        idxNames.each { idxName ->
            log.trace "janusVertexSchema await registered ${idxName}"
            ManagementSystem
                .awaitGraphIndexStatus(graph, idxName)
                .status(SchemaStatus.ENABLED)
            .call()
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // STORAGE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return the graph directory from the provided configuration object as a 
     * File object using Paths.get.
     * @param @config The source configuration
     * @return The graph directory as a File object
     */
    public static File graphDir(Config config) {
        assert config
        def graphPath = Paths.get(config.storage.directory)
        File graphDir = graphPath.toFile()
        graphDir
    }


    /** 
     * Clear the graph directory of the provided configuration.
     * @param config The configuration from which to get the graph directory
     */
    public static void clearGraph(Config config) {
        log.info "clearGraph"
        assert config
        File graphDir = graphDir(config)
        if (graphDir.exists()) {
            FileUtils.deleteDirectory(graphDir)
            graphDir.delete()
        }
    }



    ///////////////////////////////////////////////////////////////////////////
    // GRAPH MODEL - ELEMENT DEFINITIONS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    boolean isDefinitionOfClass(
        Class<ElementDefinition> defClass, 
        Class<ElementDefinition> queryClass
    ) {
        def defInterfaces = defClass.getInterfaces()
        defInterfaces.contains(queryClass)
    }



    /** 
     * Add the model defined in the given class to this Carnival.
     * @param defClass The element definition class.
     */
    @Override
    public AddModelResult addModel(Class<ElementDefinition> defClass) {
        log.trace "addModel defClass:${defClass}"

        assert defClass

        AddModelResult res

        def defInterfaces = defClass.getInterfaces()
        if (defInterfaces.contains(VertexDefinition)) res = addVertexModel(defClass)
        else if (defInterfaces.contains(EdgeDefinition)) res = addEdgeModel(defClass)
        else if (defInterfaces.contains(PropertyDefinition)) res = addPropertyModel(defClass)
        else throw new RuntimeException("unrecognized definition class: $defClass")

        janusSchemaManagement(res)

        if (res.vertexConstraints) {
            withGremlin { graph, g ->
                addClassVertices(graph, g, res.vertexConstraints)
            }
        }

        res
    }


    /** 
     * Add the model defined in the given class to this Carnival.
     * @param defClass The element definition class.
     */
    public AddModelResult addModelNoMgmt(Class<ElementDefinition> defClass) {
        log.trace "addModel defClass:${defClass}"

        assert defClass

        AddModelResult res

        def defInterfaces = defClass.getInterfaces()
        if (defInterfaces.contains(VertexDefinition)) res = addVertexModel(defClass)
        else if (defInterfaces.contains(EdgeDefinition)) res = addEdgeModel(defClass)
        else if (defInterfaces.contains(PropertyDefinition)) res = addPropertyModel(defClass)
        else throw new RuntimeException("unrecognized definition class: $defClass")

        if (res.vertexConstraints) {
            withGremlin { graph, g ->
                addClassVertices(graph, g, res.vertexConstraints)
            }
        }

        res
    }



    ///////////////////////////////////////////////////////////////////////////
    // LIFE-CYCLE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Close this Carnival.
     */
    public void close() {
        graph.close()
    }


    /** 
     * Dispose of the resources of this Carnival.
     */
    public void dispose() {
        this.close()
        CarnivalJanusBerkeley.clearGraph(this.config)
    }



    ///////////////////////////////////////////////////////////////////////////
    // CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static class Config {

        /** */
        static class Storage {
            /** */
            String directory = 'data/graph'
            /** */
            boolean transactions = true
        }
        Storage storage = new Storage()
        
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC CLASS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static class DirectoryExistsException extends Exception { 
        String graphDirectoryPath
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return a compound index name for the provided series of property
     * definitions.
     */
    static String indexNameOf(PropertyDefinition... pds) {
        assert pds

        List<PropertyDefinition> pdlist = pds.toList()

        if (pdlist.size() == 0) return null
        else if (pdlist.size() == 1) return pdlist.first().label
        else return pdlist.collect({ it.label }).join('-')
    }


    /**
     * Return a compound index name for a vertex constraint and vertex property
     * constraint, ie for a single property of a vertex.
     */
    static String indexNameOf(VertexConstraint vc, VertexPropertyConstraint vcp) {
        assert vc
        assert vcp

        String idxName = 'idx' + vc.label + '-' + vcp.name
        return idxName
    }


    /**
     * Return a compound index name for a vertex definition and a property
     * definition.
     */
    static String indexNameOf(
        VertexDefinition vd, 
        PropertyDefinition pd1
    ) {
        assert vd
        assert pd1

        String idxName = 'idx' + vd.label + '-' + pd1.label
        return idxName
    }


    /**
     * Return a compound index name for a vertex definition and two property
     * definitions, useful for the combo namespace indexes.
     */
    static String indexNameOf(
        VertexDefinition vd, 
        PropertyDefinition pd1,
        PropertyDefinition pd2
    ) {
        assert vd
        assert pd1
        assert pd1

        String idxName = 'idx' + vd.label + '-' + pd1.label + '-' + pd2.label
        return idxName
    }

}
