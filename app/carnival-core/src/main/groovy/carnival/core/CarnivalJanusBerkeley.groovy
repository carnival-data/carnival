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
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Utility method to run a closure in the context of a fresh transaction
     * that will be comitted and closed upon successful termination of the
     * closure. 
     * If there is a prior open transaction, it will be closed, but not 
     * committed.
     * If the closure accepts no arguments, it will be called without 
     * arguments.
     * If the closure accepts one argument, it will be called with the 
     * transaction object as the argument.
     * If the closure accepts any other number of arguments, a runtime
     * exception will be thrown.
     * Note that if the gremlin graph does not support transactions, then an
     * error will be thrown.
     * @param graph The gremlin graph.
     * @param cl The closure to execute.
     */
    static public void withTransaction(Graph graph, Closure cl) {
        assert graph != null
        assert cl != null

        // open a new transaction
        def tx = graph.tx()
        if (tx.isOpen()) tx.close()
        tx.open()

        // execute the closure
        def maxClosureParams = cl.getMaximumNumberOfParameters()
        try {
            if (maxClosureParams == 0) {
                cl()
            } else if (maxClosureParams == 1) {
                cl(tx)
            } else {
                throw new RuntimeException("closure must accept zero or one arguments")
            }
        } catch (Exception e) {
            try {
                tx.rollback()
            } catch (Exception e2) {
                log.error("could not rollback", e2)
            }
            throw e
        } finally {
            try {
                tx.commit()
                tx.close()
            } catch (Exception e3) {
                log.error("could not commit", e3)
            }
        }
    }


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

        String idxName = vc.label + '-' + vcp.name
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

        String idxName = vd.label + '-' + pd1.label
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

        String idxName = vd.label + '-' + pd1.label + '-' + pd2.label
        return idxName
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    Config config


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
     * @return A CarnivalJanusBerkeley object.
     */
    public static CarnivalJanusBerkeley create(Config config, Map args = [:]) {
		log.info "CarnivalJanusBerkeley create args:$args"

        assert config
        assert config.storage.directory

        JanusGraph graph = JanusGraphFactory.build().
            set("storage.backend", "berkeleyje").
            set("storage.directory", config.storage.directory).
            set("storage.transactions", config.storage.transactions).
        open();

        def transactionsAreSupported = graph.features().graph().supportsTransactions()
        assert transactionsAreSupported

		def graphSchema
        if (args.vertexBuilders) graphSchema = new DefaultGraphSchema(args.vertexBuilders)
        else graphSchema = new DefaultGraphSchema()

        def graphValidator = new DefaultGraphValidator()
        def carnival = new CarnivalJanusBerkeley(graph, graphSchema, graphValidator)
        carnival.config = config

    	def g = graph.traversal()
    	try {
	    	carnival.addModelCoreProperties(graph, g)
    	} finally {
    		if (g) g.close()
    	}

        carnival.janusSchemaManagement()

        g = graph.traversal()
        try {
            carnival.addModelCoreVerticesEdges(graph, g)
        } finally {
            if (g) g.close()
        }

    	assert carnival
		return carnival
    }


    /** 
     * Initialize a gremlin graph with the core Carnival graph model.
     * @param graph The gremlin graph to initialize
     * @param g A graph traversal source to use during initialization.
     */
    public void addModelCoreProperties(Graph graph, GraphTraversalSource g) {
        log.info "Carnival addModelCoreProperties graph:$graph g:$g"
        [Base.PX, Core.PX].each {
            addModel(graph, g, it)
        }
    }


    public void addModelCoreVerticesEdges(Graph graph, GraphTraversalSource g) {
        log.info "Carnival addModelCoreVerticesEdges graph:$graph g:$g"
        [Base.EX, Core.VX, Core.EX].each {
            addModel(graph, g, it)
        }
    } 


    ///////////////////////////////////////////////////////////////////////////
    // SCHEMA MANAGEMENT
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Apply the property uniqueness constraints contained in the graph schema 
     * using the provided graph and graph traversal source.
     * @param graph The target gremlin graph
     * @param g The graph traversal source to use
     */
    public void janusSchemaManagement() {
        log.debug "janusSchemaManagement"

        JanusGraphManagement mgmt = graph.openManagement()
        List<String> idxNames = new ArrayList<String>()

        try {
            // create indexes for all Base.PX properties
            Set<PropertyKey> propertyKeys = mgmt.getRelationTypes(PropertyKey).toSet()
            
            EnumSet.allOf(Base.PX).each { PropertyDefinition bpx ->
                PropertyKey pk = propertyKeys.find {
                    it.name() == bpx.label
                }
                assert pk
                String idxName = indexNameOf(bpx) 
                idxNames.add(idxName)
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
            mgmt
                .buildIndex(inIdxName, Vertex.class)
                .addKey(icpk)
                .addKey(nspk)
            .buildCompositeIndex()
        } catch (Exception e) {
            log.error "error creating janus schema", e
            log.warn "rolling back janus management"
            mgmt.rollback()
        } finally {
            mgmt.commit()
        }

        // wait for index availability
        /*idxNames.each {
            ManagementSystem.awaitGraphIndexStatus(graph, it).call()
        }*/
    }


    /** */
    public void janusPropertySchema(
        Set<EdgeConstraint> propertyConstraints, 
        Graph graph, 
        GraphTraversalSource g
    ) {
        log.debug "janusPropertySchema"

        assert propertyConstraints
        assert graph
        assert g

        //log.debug "propertyConstraints: ${propertyConstraints}"

        JanusGraphManagement mgmt = graph.openManagement()

        try {
            propertyConstraints.each { PropertyConstraint pc ->
                log.debug "pc: ${pc}"

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
            }
        } catch (Exception e) {
            log.error "error creating janus property schema", e
            log.warn "rolling back janus management"
            mgmt.rollback()
        } finally {
            mgmt.commit()
        }
    }


    /** */
    public void janusEdgeSchema(
        Set<EdgeConstraint> edgeConstraints, 
        Graph graph, 
        GraphTraversalSource g
    ) {
        log.debug "janusEdgeSchema"

        assert edgeConstraints
        assert graph
        assert g

        //log.debug "edgeConstraints: ${edgeConstraints}"

        JanusGraphManagement mgmt = graph.openManagement()

        try {
            Set<VertexLabel> vertexLabels = mgmt.getVertexLabels().toSet()

            edgeConstraints.each { EdgeConstraint ec ->
                log.debug "ec: ${ec}"
                
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
    public void janusVertexSchema(
        Set<VertexConstraint> vertexConstraints, 
        Graph graph, 
        GraphTraversalSource g
    ) {
        log.debug "janusVertexSchema"

        assert vertexConstraints
        assert graph
        assert g

        //log.debug "vertexConstraints: ${vertexConstraints}"

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
                log.debug "vc: ${vc}"

                VertexLabelMaker vlm = mgmt.makeVertexLabel(vc.label)
                VertexLabel vl = vlm.make()

                // combo isclass and namespace 
                String icnsIdxName = indexNameOf(
                    vc.vertexDef, Base.PX.IS_CLASS, Base.PX.NAME_SPACE
                )
                log.debug "icnsIdxName: ${icnsIdxName}"
                idxNames.add(icnsIdxName)
                mgmt
                    .buildIndex(icnsIdxName, Vertex.class)
                    .addKey(icpk)
                    .addKey(nspk)
                    .indexOnly(vl)
                .buildCompositeIndex()

                vc.properties.each { VertexPropertyConstraint vpc ->
                    PropertyKey pk = propertyKeys.find {
                        it.name() == vpc.name
                    }
                    if (pk) {
                        mgmt.addProperties(vl, pk)

                        // property index
                        String idxName = indexNameOf(vc, vpc)
                        log.debug "idxName: ${idxName}"
                        idxNames.add(idxName)
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
                        log.debug "comboIdxName: ${comboIdxName}"
                        idxNames.add(comboIdxName)
                        mgmt
                            .buildIndex(comboIdxName, Vertex.class)
                            .addKey(nspk)
                            .addKey(pk)
                            .indexOnly(vl)
                        .buildCompositeIndex()

                    } else {
                        log.warn "could not find Janus property key with name: ${vpc.name}"
                    }

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
        /*idxNames.each {
            ManagementSystem.awaitGraphIndexStatus(graph, it).call()
        }*/
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

    /** 
     * Add the model defined in the given class to this Carnival.
     * @param defClass The element definition class.
     */
    @Override
    public void addModel(Class<ElementDefinition> defClass) {
        assert defClass
        withGremlin { graph, g ->
            addModel(graph, g, defClass)
        }
    }

    /**
     * Add a model defined in the given class to this Carnival using the
     * provided graph and graph traversal source.  This is an internal method;
     * it is expected that client code will use addModel(Class) to add models.
     * @see #addModel(Class<ElementDefinition>)
     * @param graph A gremlin graph.
     * @param g A grpah traversal source to use.
     * @param defClass The element definition class.
     */
    @Override
    public void addModel(Graph graph, GraphTraversalSource g, Class<ElementDefinition> defClass) {
        assert graph
        assert g
        assert defClass

        def defInterfaces = defClass.getInterfaces()
        if (defInterfaces.contains(VertexDefinition)) {
            addVertexModel(graph, g, defClass)
        } else if (defInterfaces.contains(EdgeDefinition)) {
            addEdgeModel(graph, g, defClass)
        } else if (defInterfaces.contains(PropertyDefinition)) {
            addPropertyModel(graph, g, defClass)
        } else {
            throw new RuntimeException("unrecognized definition class: $defClass")
        }

    }


    /**
     * Add a vertex model defined in the given vertex definition class to this
     * Carnival using the provided graph and graph traversal source.  This is
     * an internal method; it is expected that client code will use
     * addModel(Class) to add models.
     * @see #addModel(Class<ElementDefinition>)
     * @param defClass The vertex definition class.
     * @param graph A gremlin graph.
     * @param g A graph traversal source to use.
     */
    @Override
    public void addVertexModel(Graph graph, GraphTraversalSource g, Class<VertexDefinition> defClass) {
        assert graph
        assert g
        assert defClass

        Set<VertexConstraint> vertexConstraints = findNewVertexConstraints(defClass)
        vertexConstraints.each { vc ->
            addConstraint(vc)
        }
        janusVertexSchema(vertexConstraints, graph, g)
        
        GremlinTraitUtilities.withGremlin(graph, g) {
            addClassVertices(graph, g, vertexConstraints)
        }
    }


    /**
     * Add the edge models in the provided edge definition class to this
     * Carnival using the provided graph and graph traversal source. This is an
     * internal method and not expected to be called by client code.
     * @param graph A gremlin graph.
     * @param g A graph traversal source.
     * @param defClass An edge definition class.
     */
    @Override
    public void addEdgeModel(Graph graph, GraphTraversalSource g, Class<EdgeDefinition> defClass) {
        assert graph
        assert g
        assert defClass

        Set<EdgeConstraint> edgeConstraints = findNewEdgeConstraints(defClass)
        edgeConstraints.each { ec ->
            addConstraint(ec)
        }
        janusEdgeSchema(edgeConstraints, graph, g)
    }


    /**
     * Add the edge models in the provided property definition class to this
     * Carnival using the provided graph and graph traversal source. This is an
     * internal method and not expected to be called by client code.
     * @param graph A gremlin graph.
     * @param g A graph traversal source.
     * @param defClass A property definition class.
     */
    public void addPropertyModel(Graph graph, GraphTraversalSource g, Class<PropertyDefinition> defClass) {
        assert graph
        assert g
        assert defClass

        log.debug "\n\naddPropertyModel defClass:${defClass}\n\n"

        Set<PropertyConstraint> propertyConstraints = findNewPropertyConstraints(defClass)
        propertyConstraints.each { pc ->
            addConstraint(pc)
        }
        janusPropertySchema(propertyConstraints, graph, g)
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

}
