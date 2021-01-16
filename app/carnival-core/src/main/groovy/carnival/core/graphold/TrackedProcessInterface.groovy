package carnival.core.graphold



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

