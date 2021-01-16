package carnival.core.graphold



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex

import carnival.graph.EdgeDefTrait
import carnival.graph.VertexDefTrait
import carnival.core.graph.GremlinTrait
import carnival.core.graph.Core
import carnival.core.util.CoreUtil



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
    public Reasoner() { }

    /** */
    public Reasoner(Graph theGraph) {
        assert theGraph
        setGraph(theGraph)
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
        def runs = getAllSuccessfulTrackedProcesses(traversal())
        String argsHash = CoreUtil.standardizedUniquifier(String.valueOf(args))
        def theRun = runs.find { 
            Core.PX.ARGUMENTS_HASH.of(it).isPresent() && Core.PX.ARGUMENTS_HASH.valueOf(it) == argsHash 
        }
        if (theRun == null) {
            out.result = this.reason(args)
            out.processVertex = this.createAndSetTrackedProcessVertex(graph)
            Core.PX.ARGUMENTS_HASH.set(out.processVertex, argsHash)
            if (out.result?.success) Core.PX.SUCCESS.set(out.processVertex, true)
            log.info "${this.class.simpleName} ensure result: ${out.result}"
        } else {
            log.info "${this.class.simpleName} already run ${runs.size()} times"
        }
        out
    }


}