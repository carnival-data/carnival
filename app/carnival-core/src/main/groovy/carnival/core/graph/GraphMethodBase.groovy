package carnival.core.graph



import java.time.Instant
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.util.CoreUtil
import carnival.graph.Base
import carnival.graph.VertexDefinition
import carnival.core.Core



/**
 * A basic elements of a graph method.
 *
 */
class GraphMethodBase {    


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS 
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * the arguments supplied to the execute method and used to compute a hash
     * of the call for unique naming.
     */
    Map arguments = new HashMap()

    /** The process vertex definition */
    VertexDefinition processVertexDef = Core.VX.GRAPH_PROCESS

    /** The process class vertex definition */
    VertexDefinition processClassVertexDef = Core.VX.GRAPH_PROCESS_CLASS

    /** The name of this graph method */
    String name = this.class.name

    /** 
     * Make the process vertex available in a non-thread-safe way for use by
     * the execute methods.
     */
    protected Vertex processVertex


    ///////////////////////////////////////////////////////////////////////////
    // ACCESSORS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return the args used during execution.
     * @return A map of arguments
     */
    public Map getArgs() {
        this.arguments
    }


    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set the arguments of a graph method prior to executing it.
     * @param A map of arguments
     * @return This object
     */
    public GraphMethodBase arguments(Map args) {
        assert args != null
        this.arguments = args
        this
    }


    /**
     * Set the process vertex definition.
     * @param vdt The vertex definition
     * @return This object
     */
    public GraphMethodBase processDefinition(VertexDefinition vdt) {
        assert vdt != null
        this.setProcessVertexDef(vdt)
        this
    }


    /**
     * Set the process class vertex definition.
     * @param vdt The class vertex definition
     * @return This object
     */
    public GraphMethodBase processClassDefinition(VertexDefinition vdt) {
        assert vdt != null
        this.setProcessClassVertexDef(vdt)
        this
    }


    /**
     * Set the name of this graph method.
     * @param name The name to use
     * @return This object
     */
    public GraphMethodBase name(String name) {
        assert name != null
        this.setName(name)
        this
    }



    ///////////////////////////////////////////////////////////////////////////
    // GRAPH GETTER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the graph representations in the graph of executed graph methods.
     * @param g The traph traversal source to use
     * @return The set of graph method process
     */
    public Set<GraphMethodProcess> processes(GraphTraversalSource g) {
        assert g != null
        processesVertices(g).collect({ new GraphMethodProcess(vertex:it) })
    }


    /**
     * Return the set of graph method process vertices for this object.
     * @param g The graph traversal source to use
     * @return The set of graph method process vertices
     */
    public Set<Vertex> processesVertices(GraphTraversalSource g) {
        assert g != null
        String argsHash = CoreUtil.argumentsUniquifier(this.arguments)
        g.V()
            .isa(getProcessVertexDef())
            .has(Core.PX.NAME, getName())
            .has(Core.PX.ARGUMENTS_HASH, argsHash)
        .toSet()
    }



    ///////////////////////////////////////////////////////////////////////////
    // CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a graph method call object and instantiates the representation
     * in the graph.
     */
    protected GraphMethodCall graphMethodCallStart(
        Graph graph, 
        GraphTraversalSource g,
        Instant startTime
    ) {
        log.trace "graphMethodCallStart startTime:${startTime}"

        assert graph != null
        assert g != null
        assert startTime != null

        // compute a hash to record in the process vertex
        String argsHash = CoreUtil.argumentsUniquifier(this.arguments)

        // grab the defs
        def pcvDef = getProcessClassVertexDef()
        def pvDef = getProcessVertexDef()

        // assert that the process def has the required properties
        pvDef.propertyDefs.addAll(Core.VX.GRAPH_PROCESS.propertyDefs)
        
        // create the process vertex
        Vertex procV = pvDef.instance().withProperties(
            Core.PX.NAME, getName(),
            Core.PX.ARGUMENTS_HASH, argsHash,
            Core.PX.START_TIME, startTime.toEpochMilli()
        ).create(graph)
        
        // the process vertex is an instance of the process class
        Base.EX.IS_INSTANCE_OF.instance()
            .from(procV)
            .to(pcvDef.vertex)
        .create()
        
        // ensure that the process class is a subclass of GRAPH_PROCESS_CLASS
        // it is troubling that this happens every time a graph method is called
        if (
            pcvDef.label != Core.VX.GRAPH_PROCESS_CLASS.label ||
            pcvDef.nameSpace != Core.VX.GRAPH_PROCESS_CLASS.nameSpace
        ) {
            Base.EX.IS_SUBCLASS_OF.instance()
                .from(pcvDef.vertex)
                .to(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .ensure(g)
        }

        // set the internal process vertex
        this.processVertex = procV

        // construct and return a graph method call
        GraphMethodCall gmc = new GraphMethodCall(
            arguments: this.arguments,
            processVertex: procV
        )

        gmc
    }


    /**
     *
     *
     */
    protected void graphMethodCallStop(
        GraphMethodCall gmc,
        Instant stopTime,
        Map result,
        Exception exception        
    ) {
        log.trace "graphMethodCallStop gmc:${gmc}"
        log.trace "graphMethodCallStop stopTime:${stopTime}"
        log.trace "graphMethodCallStop result:${result}"
        log.trace "graphMethodCallStop exception:${exception}"

        assert gmc != null
        assert gmc.processVertex
        assert stopTime != null

        def procV = gmc.processVertex

        Core.PX.STOP_TIME.set(
            procV, 
            stopTime.toEpochMilli()
        )

        if (exception != null) {
            try {
                String msg = exception.message ?: "${exception.class}"
                Core.PX.EXCEPTION_MESSAGE.set(procV, msg)
            } catch (Exception e) {
                log.warn "could not set exception message of process vertex ${procV} ${e.message}"
            }
            throw exception
        }
        
        if (result != null) gmc.result = result
        if (exception != null) gmc.exception = exception
    }


    /**
     * Creates a graph method call object and instantiates the representation
     * in the graph.
     */
    protected GraphMethodCall graphMethodCall(
        Graph graph, 
        GraphTraversalSource g,
        Instant startTime,
        Instant stopTime,
        Map result,
        Exception exception
    ) {
        assert graph != null
        assert g != null
        assert stopTime != null
        assert startTime != null
        
        // compute a hash to record in the process vertex
        String argsHash = CoreUtil.argumentsUniquifier(this.arguments)

        // grab the defs
        def pcvDef = getProcessClassVertexDef()
        def pvDef = getProcessVertexDef()

        // assert that the process def has the required properties
        pvDef.propertyDefs.addAll(Core.VX.GRAPH_PROCESS.propertyDefs)
        
        // create the process vertex
        Vertex procV = pvDef.instance().withProperties(
            Core.PX.NAME, getName(),
            Core.PX.ARGUMENTS_HASH, argsHash,
            Core.PX.START_TIME, startTime.toEpochMilli(),
            Core.PX.STOP_TIME, stopTime.toEpochMilli()
        ).create(graph)
        
        // the process vertex is an instance of the process class
        Base.EX.IS_INSTANCE_OF.instance()
            .from(procV)
            .to(pcvDef.vertex)
        .create()
        
        // ensure that the process class is a subclass of GRAPH_PROCESS_CLASS
        // it is troubling that this happens every time a graph method is called
        if (
            pcvDef.label != Core.VX.GRAPH_PROCESS_CLASS.label ||
            pcvDef.nameSpace != Core.VX.GRAPH_PROCESS_CLASS.nameSpace
        ) {
            Base.EX.IS_SUBCLASS_OF.instance()
                .from(pcvDef.vertex)
                .to(Core.VX.GRAPH_PROCESS_CLASS.vertex)
            .ensure(g)
        }

        // if an exception was caught, record the message in the process vertex
        if (exception != null) {
            try {
                String msg = exception.message ?: "${exception.class}"
                Core.PX.EXCEPTION_MESSAGE.set(procV, msg)
            } catch (Exception e) {
                log.warn "could not set exception message of process vertex ${procV} ${e.message}"
            }
            throw exception
        }

        // construct and return a graph method call
        GraphMethodCall gmc = new GraphMethodCall(
            arguments: this.arguments,
            result: result,
            processVertex: procV
        )
        if (exception != null) gmc.exception = exception

        gmc
    }


}