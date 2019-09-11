package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex

import org.apache.tinkerpop.gremlin.neo4j.structure.*

import carnival.core.vine.Vine
import carnival.core.vine.CachingVine.CacheMode
import carnival.core.graph.GremlinTrait

import carnival.graph.EdgeDefTrait
import carnival.graph.VertexDefTrait



/** */
abstract public class Reasoner implements ReasonerInterface, GremlinTrait {
	
    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** sql log */
    static Logger sqllog = LoggerFactory.getLogger('sql')

    /** error log */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** carnival log */
    static Logger log = LoggerFactory.getLogger('carnival')

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

    /** */
    //static enum PX implements PropertyDefTrait { }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////


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

}