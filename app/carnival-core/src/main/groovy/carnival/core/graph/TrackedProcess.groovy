package carnival.core.graph



import groovy.transform.Synchronized

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.T

import carnival.graph.VertexDefTrait
import carnival.graph.Base
import carnival.graph.DynamicVertexDef
import carnival.core.graph.Core



/** */
public interface TrackedProcessInterface {

    /** */
    VertexDefTrait getTrackedProcessClassDef()

    /** */
    VertexDefTrait getTrackedProcessDef()

    /** */
    Vertex getTrackedProcessVertex()

    /** */
    void setTrackedProcessVertex(Vertex v)
}



/** */
trait TrackedProcessTrait implements TrackedProcessInterface {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')

    /** */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')


    ///////////////////////////////////////////////////////////////////////////
    // HELPER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    Vertex createAndSetTrackedProcessVertex(Graph graph) {
        assert graph
        def procV = createTrackedProcessVertex(graph)
        assert procV
        setTrackedProcessVertex(procV)
    }


    /** */
    Vertex createTrackedProcessVertex(Graph graph) {
        log.trace "TrackedProcess.createTrackedProcessVertex ${this}"        
        assert graph

        if (trackedProcessClassDef == null) {
            log.trace 'createTrackedProcessVertex: will not create process class vertex. trackedProcessClassDef is null'
            return null
        }

        if (trackedProcessDef == null) {
            log.trace 'createTrackedProcessVertex: will not create process vertex. trackedProcessDef is null'
            return null
        }

        assert trackedProcessClassDef.vertex
        assert trackedProcessDef.label

        long now = new Date().time
        def procV = graph.addVertex(
            T.label, trackedProcessDef.label,
            Base.PX.NAME_SPACE.label, trackedProcessDef.nameSpace,
            Core.PX.START_TIME.label, now,
            Core.PX.SUCCESS.label, false
        )
        //Core.EX.IS_INSTANCE_OF.relate(g, procV, trackedProcessClassDef.vertex)
        procV
            .addEdge(Core.EX.IS_INSTANCE_OF.label, trackedProcessClassDef.vertex)
            .property(Base.PX.NAME_SPACE.label, Core.EX.IS_INSTANCE_OF.nameSpace)
        
        log.trace "TrackedProcess.createTrackedProcessVertex procV:$procV"

        return procV
    }
    

    /** */
    void setTrackedProcessInputs(Vertex procVert, Collection<Vertex> inputVertices) {
        assert procVert
        assert inputVertices != null
        assert inputVertices.size() > 0

        inputVertices.each { pV ->
            pV.addEdge(Core.EX.IS_INPUT_OF.label, procVert)
        }            
    }


    /** */
    void setTrackedProcessInputs(Collection<Vertex> inputVertices) {
        assert inputVertices
        def procVert = getTrackedProcessVertex()
        setTrackedProcessInputs(procVert, inputVertices)
    }


    /** */
    Set<Vertex> getAllSuccessfulTrackedProcesses(GraphTraversalSource g) {
        assert g

        def procDef = getTrackedProcessDef()
        //log.debug "procDef: $procDef"
        assert procDef

        g.V()
            .hasLabel(procDef.label)
            .has(Core.PX.SUCCESS.label, true)
        .dedup().toSet()
    }


    /** */
    Set<Vertex> getAllTrackedProcesses(GraphTraversalSource g) {
        assert g

        def procDef = getTrackedProcessDef()
        //log.debug "procDef: $procDef"
        assert procDef

        g.V().hasLabel(procDef.label).dedup().toSet()
    }


    /** */
    Set<Vertex> getAllTrackedProcessInputs(GraphTraversalSource g) {
        assert g
        def procVs = getAllTrackedProcesses(g)
        if (procVs.size() == 0) return []

        g.V(procVs).in(Core.EX.IS_INPUT_OF.label).dedup().toSet()
    }


    /** */
    Set<Edge> getAllTrackedProcessInputEdges(GraphTraversalSource g) {
        assert g

        def procVs = getAllTrackedProcesses(g)
        if (procVs.size() == 0) return []

        g.V(procVs).inE(Core.EX.IS_INPUT_OF.label).dedup().toSet()
    }




    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    VertexDefTrait getTrackedProcessClassDef() { return null }

    /** */
    VertexDefTrait getTrackedProcessDef() { return null }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    Vertex trackedProcessVertex

}



/** */
public class TrackedProcess implements TrackedProcessTrait {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')

    /** */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** */
    static TrackedProcess create(Graph graph, GraphTraversalSource g, String name) {
        assert graph
        assert g
        assert name

        if (!name.endsWith('Process')) name += 'Process'

        def tp = new TrackedProcess(
            name:name,
            trackedProcessClassDef:DynamicVertexDef.singletonFromCamelCase(graph, g, "${name}Class"),
            trackedProcessDef:DynamicVertexDef.singletonFromCamelCase(graph, g, name)
        )

        return tp
    }


    /** */
    static Vertex ensure(Graph graph, GraphTraversalSource g, String name, String description, Closure cl) {
        optionallyRunSingletonProcess(graph, g, name, description, cl)
    }


    /** */
    static Vertex ensure(Graph graph, GraphTraversalSource g, String name, Closure cl) {
        optionallyRunSingletonProcess(graph, g, name, cl)
    }


    /** */
    static Vertex singleton(Graph graph, GraphTraversalSource g, String name, String description, Closure cl) {
        optionallyRunSingletonProcess(graph, g, name, description, cl)
    }


    /** */
    static Vertex singleton(Graph graph, GraphTraversalSource g, String name, Closure cl) {
        optionallyRunSingletonProcess(graph, g, name, cl)
    }


    /** */
    static Vertex optionallyRunSingletonProcess(Graph graph, GraphTraversalSource g, String name, String description, Closure cl) {
        assert graph
        assert g
        assert name != null
        //assert description != null
        assert cl != null
        assert cl instanceof Closure

        def tp = create(graph, g, name)
        def procs = tp.getAllSuccessfulTrackedProcesses(g)
        if (procs.size() > 0) return null
        
        def maxClosureParams = cl.getMaximumNumberOfParameters()

        def procV = tp.createTrackedProcessVertex(graph)
        if (description != null) procV.property(Core.PX.DESCRIPTION.label, description)

        if (maxClosureParams == 0) cl.call()
        else if (maxClosureParams == 1) cl.call(name)
        else if (maxClosureParams > 1 && description != null) cl.call(name, description)
        else cl.call(name)

        procV.property(Core.PX.SUCCESS.label, true)

        return procV
    }


    /** */
    static Vertex optionallyRunSingletonProcess(Graph graph, GraphTraversalSource g, String name, Closure cl) {
        optionallyRunSingletonProcess(graph, g, name, null, cl)
    }   


    /** */
    String name

    /** */
    VertexDefTrait trackedProcessDef

    /** */
    VertexDefTrait trackedProcessClassDef

}





