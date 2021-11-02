package carnival.core.graph



import spock.lang.Specification
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.util.Defaults
import static carnival.util.AppUtil.sysPropFalse



/**
 *
 */
abstract class CoreGraphSpecification extends Specification {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    @Shared configFilePath

    @Shared coreGraph
    @Shared graph



    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////
    
    /** */
    final def setupSpec() {
        // Neo4j
        System.setProperty('log4j.configuration', 'log4j.properties')

        // get config
        configFilePath = Defaults.findApplicationConfigurationFile()
        if (configFilePath != null) configFilePath = configFilePath.canonicalPath

        // create the graph
        println "init neo4j graph..."
        CoreGraphNeo4j.clearGraph()
        coreGraph = CoreGraphNeo4j.create()
        graph = coreGraph.graph

        // validate the base graph
        assertGraphConstraints()

        if (this.respondsTo('localSetupSpec')) this.localSetupSpec()
    }


    /** */
    final def setup() {
        if (this.respondsTo('localSetup')) this.localSetup()
    }


    /** */
    final def cleanupSpec() {
        if (this.respondsTo('localCleanupSpec')) this.localCleanupSpec()
        if (graph) graph.close()
    }


    /** */
    final def cleanup() {
        if (this.respondsTo('localCleanup')) this.localCleanup()
        if (graph) {
            if (sysPropFalse('test.graph.rollback')) graph.tx().commit()
            else graph.tx().rollback() 
        }
    }


    /** */
    void assertGraphConstraints() {
        assert coreGraph.checkConstraints().size() == 0
        assert coreGraph.checkModel().size() == 0
    }

}





