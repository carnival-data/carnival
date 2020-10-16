package carnival.core.graph



import groovy.transform.Synchronized

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.T

import carnival.graph.VertexDefTrait
import carnival.graph.DynamicVertexDef



/**
 * The ReaperMethod interface must be implemented by any reaper methods...
 *
 */
abstract class ReaperMethod implements ReaperMethodInterface, TrackedProcessTrait {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

	/** carnival logger */
    static Logger log = LoggerFactory.getLogger(ReaperMethod)


    /** */
    static void assertPatientVertices(Map args) {
        if (args.patientVertices) {
            assert args.patientVertices.each {
                assert it instanceof Vertex
                assert it.label() == "Patient"
            }
        }
    }


    /** */
    static List patientVerts(GraphTraversalSource g, Map args) {
        def patientVerts = []
        def operateOverAllPatients = false
        if (args.containsKey('patientVertices')) {
            patientVerts = args.patientVertices
        } else {
            patientVerts = g.patients().toList()
            operateOverAllPatients = true
        }

        patientVerts.each {
            assert it instanceof Vertex
            assert it.label() == "Patient"
        }

        return [patientVerts, operateOverAllPatients]            
    }



    ///////////////////////////////////////////////////////////////////////////
    // CALL
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Map ensure(Map args = [:]) {
        optionallyRunSingletonProcess(this, args)
    }



    ///////////////////////////////////////////////////////////////////////////
    // TRACKED PROCESS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    Vertex createReaperProcess() {
        log.trace "createReaperProcess() ${this}"
        
        def g = traversal()
        
        Core.VX.DATA_TRANSFORMATION_PROCESS_CLASS.setSubclassOf(g, Core.VX.PROCESS_CLASS)
        Reaper.VX.REAPER_PROCESS_CLASS.setSubclassOf(g, Core.VX.DATA_TRANSFORMATION_PROCESS_CLASS)

        def procClassDef = getTrackedProcessClassDef()
        log.trace "procClassDef: $procClassDef"
        
        procClassDef.setSubclassOf(g, Reaper.VX.REAPER_PROCESS_CLASS)

        createTrackedProcessVertex(graph)
    }


    /** */
    void setReaperProcessInputs(Vertex procVert, Collection<Vertex> inputVertices) {
        setTrackedProcessInputs(procVert, inputVertices)
    }


    /** */
    void setReaperProcessInputs(Collection<Vertex> inputVertices) {
        setTrackedProcessInputs(inputVertices)
    }

    /** */
    void setReaperProcessInput(Vertex inputVertex) {
        setTrackedProcessInputs([inputVertex])
    }


    /** */
    String getTrackedProcessClassName() { "${this.class.simpleName}ProcessClass" }

    /** */
    String getTrackedProcessName() { "${this.class.simpleName}Process" }


    /** */
    @Override
    VertexDefTrait getTrackedProcessClassDef() {
        def name = getTrackedProcessClassName()
        DynamicVertexDef.singletonFromCamelCase(getGraph(), traversal(), name)
    }


    /** */
    @Override
    VertexDefTrait getTrackedProcessDef() { 
        def name = getTrackedProcessName()
        DynamicVertexDef.singletonFromCamelCase(getGraph(), traversal(), name) 
    }


    ///////////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Convenience method to run some cypher with optional logging.
     *
     */
    Collection doCypher(String cypher, Map args = [:]) {
        if (args.log) {
            sqllog.info(cypher)
            if (args) sqllog.info "args: ${args.keySet()}"
        }
        
        def res

        if (args) res = graph.cypher(cypher, args).toList()
        else res = graph.cypher(cypher).toList()

        //log.debug "res: $res"
        return res
    }


    /**
     * Convenience method to commit the graph with logging.
     *
     */
    @Synchronized
    void graphCommit() {
        log.trace "commit start..."
        graph.tx().commit()
        log.trace "commit done."
    }


    /**
     * Convenience method to great a lookup hash for the given objects.
     *
     */
    Map createHash(Collection objects) {
        def hash = new HashMap()
        objects.each { hash.put(it, 1) }
        return hash
    }    


    /** */
    protected Object withTraversal(Closure cl) {
        def g = traversal()
        GremlinTraitUtilities.withTraversal(graph, g, cl)
    }

    /** */
    protected Object withGremlin(Closure cl) {
        def g = traversal()
        try {
            GremlinTraitUtilities.withGremlin(graph, g, cl)
        } finally {
            try {
                if (g != null) g.close()
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }

}
