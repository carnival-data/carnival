package carnival.core.graph



import java.time.Instant
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.util.CoreUtil
import carnival.graph.Base
import carnival.graph.VertexDefinition



/**
 *
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

    /** */
    VertexDefinition processVertexDef = Core.VX.GRAPH_PROCESS

    /** */
    VertexDefinition processClassVertexDef = Core.VX.GRAPH_PROCESS_CLASS

    /** */
    String name = this.class.name


    ///////////////////////////////////////////////////////////////////////////
    // ACCESSORS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Map getArgs() {
        this.arguments
    }


    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set the arguments of a graph method prior to executing it.
     *
     */
    public GraphMethodBase arguments(Map args) {
        assert args != null
        this.arguments = args
        this
    }


    /**
     *
     *
     */
    public GraphMethodBase processDefinition(VertexDefinition vdt) {
        assert vdt != null
        this.setProcessVertexDef(vdt)
        this
    }


    /**
     *
     *
     */
    public GraphMethodBase processClassDefinition(VertexDefinition vdt) {
        assert vdt != null
        this.setProcessClassVertexDef(vdt)
        this
    }


    /**
     *
     *
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
     *
     */
    public Set<GraphMethodProcess> processes(GraphTraversalSource g) {
        assert g != null
        String argsHash = CoreUtil.argumentsUniquifier(this.arguments)
        Set<Vertex> procVs = g.V()
            .isa(getProcessVertexDef())
            .has(Core.PX.NAME, getName())
            .has(Core.PX.ARGUMENTS_HASH, argsHash)
        .toSet()
    }



    ///////////////////////////////////////////////////////////////////////////
    // CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Calls the execute() method and represents the call in the graph.
     *
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

        gmc
    }


}