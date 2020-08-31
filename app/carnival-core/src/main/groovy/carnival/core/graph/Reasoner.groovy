package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex

import carnival.core.vine.Vine
import carnival.core.graph.GremlinTrait

import carnival.graph.EdgeDefTrait
import carnival.graph.VertexDefTrait



/** */
abstract public class Reasoner implements ReasonerInterface, GremlinTrait, TrackedProcessDefaultTrait {
	
    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** carnival log */
    static Logger log = LoggerFactory.getLogger(Reasoner)

    /** */
    static enum VX implements VertexDefTrait {
        REASONER_PROCESS_CLASS,
        REASONER_INCONSISTENCY_CLASS,
        REASONER_DECISION_CLASS,
    }

    /** */
    static enum EX implements EdgeDefTrait {
        HAS_INCONSISTENCY_CLASS, 
        HAS_REASONER_DECISION_CLASS,

        IS_INCONSISTENT_PER,
        IS_REASONED_PER,
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Reasoner(Graph graph) {
        assert graph
        this.graph = graph
    }


    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    abstract public Map validate(Map args)

    /** */
    abstract public Map reason(Map args)


    ///////////////////////////////////////////////////////////////////////////
    // TRACKING
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Map ensure(Map args) {
        def out = [:]
        def numRuns = getAllSuccessfulTrackedProcesses(traversal()).size()
        if (numRuns == 0) {
            out.result = this.reason(args)
            out.processVertex = this.createAndSetTrackedProcessVertex(graph)
            if (out.result?.success) Core.PX.SUCCESS.set(out.processVertex, true)
            log.info "${this.class.simpleName} ensure result: ${out.result}"
        } else {
            log.info "${this.class.simpleName} already run ${numRuns} times"
        }
        out
    }

}