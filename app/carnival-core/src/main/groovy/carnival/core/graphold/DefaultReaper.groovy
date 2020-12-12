package carnival.core.graphold



import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph

import carnival.core.graph.CoreGraph



/** */
public class DefaultReaper extends Reaper {

    @ReaperMethodResource
    CoreGraph coreGraph

    public DefaultReaper(CoreGraph coreGraph) {
        assert coreGraph
        assert coreGraph.graph
        this.coreGraph = coreGraph
    }

    protected Graph getGraph() { this.coreGraph.graph }

    protected GraphTraversalSource traversal() {
        this.coreGraph.graph.traversal()
    }

}

