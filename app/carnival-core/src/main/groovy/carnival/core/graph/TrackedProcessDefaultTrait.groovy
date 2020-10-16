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
trait TrackedProcessDefaultTrait extends TrackedProcessTrait {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger(TrackedProcessDefaultTrait)


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

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

}
