package carnival.core.graph



import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Instant
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.util.CoreUtil
import carnival.graph.Base
import carnival.graph.VertexDefTrait



/**
 * GraphMethod encapsulates a unit of business logic that modifies the graph.
 * GraphMethods may return a Map result, but the fundamental result of a graph
 * method is a mutation of the graph.  An executed graph method will be
 * represented in the graph as a "process" vertex with optional links to
 * outputs.
 *
 * 
 *
 */
abstract class GraphMethod {    


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * An abstract method to be implemented by the concretizing class to
     * implement the logic of the method.
     *
     */
    abstract Map execute(Graph graph, GraphTraversalSource g) 



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS 
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * the arguments supplied to the execute method and used to compute a hash
     * of the call for unique naming.
     */
    Map arguments

    /** */
    VertexDefTrait processVertexDef = Core.VX.GRAPH_PROCESS

    /** */
    VertexDefTrait processClassVertexDef = Core.VX.GRAPH_PROCESS_CLASS


    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set the arguments of a graph method prior to executing it.
     *
     */
    public GraphMethod arguments(Map args) {
        assert args != null
        this.arguments = args
        this
    }


    /**
     *
     *
     */
    public GraphMethod processDefinition(VertexDefTrait vdt) {
        assert vdt != null
        this.setProcessVertexDef(vdt)
        this
    }


    /**
     *
     *
     */
    public GraphMethod processClassDefinition(VertexDefTrait vdt) {
        assert vdt != null
        this.setProcessClassVertexDef(vdt)
        this
    }


    /**
     * Calls the execute() method and represents the call in the graph.
     *
     */
    public GraphMethodCall call(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null

        Map result 
        Instant stopTime
        Exception exception
        Instant startTime
        
        // execute the graph method recording the start
        // and stop times
        try {
            startTime = Instant.now()
            result = execute(graph, g)
        } catch (Exception e) {
            exception = e
        } finally {
            stopTime = Instant.now()
        }
        
        // compute a hash to record in the process vertex
        String argsHash = CoreUtil.standardizedUniquifier(String.valueOf(this.arguments))

        // grab the defs
        def pcvDef = getProcessClassVertexDef()
        def pvDef = getProcessVertexDef()

        // assert that the process def has the required properties
        pvDef.propertyDefs.addAll(Core.VX.GRAPH_PROCESS.propertyDefs)
        
        // create the process vertex
        Vertex procV = pvDef.instance().withProperties(
            Core.PX.NAME, this.class.name,
            Core.PX.ARGUMENTS_HASH, argsHash,
            Core.PX.START_TIME, startTime.toEpochMilli(),
            Core.PX.STOP_TIME, stopTime.toEpochMilli()
        ).create(graph)
        
        // the process vertex is an instance of the process class
        Core.EX.IS_INSTANCE_OF.instance()
            .from(procV)
            .to(pcvDef.vertex)
        .create()
        
        // ensure that the process class is a subclass of GRAPH_PROCESS_CLASS
        // it is troubling that this happens every time a graph method is called
        if (pcvDef != Core.VX.GRAPH_PROCESS) {
            Base.EX.IS_SUBCLASS_OF.instance()
                .from(pcvDef.vertex)
                .to(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .ensure(g)
        }

        // if an exception was caught, record the message in the process vertex
        if (exception != null) {
            try {
                Core.PX.EXCEPTION_MESSAGE.set(procV, exception.message)
            } catch (Exception e) {
                log.warn "could not set exception message of process vertex ${procV} ${e.message}"
            }
            throw e
        }

        // construct and return a graph method call
        GraphMethodCall gmc = new GraphMethodCall(
            arguments: this.arguments,
            result: result,
            processVertex: procV
        )

        gmc
    }


    ///////////////////////////////////////////////////////////////////////////
    // GRAPH GETTER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the graph representations in the graph of executed graph methods.
     *
     */
    public Set<GraphMethodProcess> processes(GraphTraversalSource g) {
        String argsHash = CoreUtil.standardizedUniquifier(String.valueOf(this.arguments))
        Set<Vertex> procVs = g.V()
            .isa(getProcessVertexDef())
            .has(Core.PX.ARGUMENTS_HASH, argsHash)
        .toSet()
    }

}