package carnival.graph



import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.util.StringUtils



/** */
@ToString
@EqualsAndHashCode(allProperties=true)
class DynamicVertexDef implements VertexDefTrait {

    /** 
     * @param name The name of the vertex def formatted as per Java enum convention, eg. SOME_NAME
     *
     */
    static public DynamicVertexDef singletonFromScreamingSnakeCase(Graph graph, GraphTraversalSource g, String name) {
        def dvf = new DynamicVertexDef(name)
        if (dvf.isClass()) dvf.vertex = dvf.controlledInstance().vertex(graph, g)
        return dvf
    }

    /** 
     * @param name The name of the vertex def formatted in camel case, eg. SomeName
     *
     */
    static public DynamicVertexDef singletonFromCamelCase(Graph graph, GraphTraversalSource g, String name) {
        singletonFromScreamingSnakeCase(
            graph, 
            g, 
            StringUtils.toScreamingSnakeCase(name)
        )
    }

    /** */
    String name

    /** */
    String nameSpaceOverride

    /** */
    private DynamicVertexDef(String name) {
        this.name = name
    }

    /** */
    public String name() {
        return this.name
    }

    /** */
    @Override
    public String getNameSpace() {
        if (this.nameSpaceOverride != null) return this.nameSpaceOverride

        log.debug "${this.metaClass}"
        log.debug "${this.metaClass.theClass}"
        log.debug "${this.metaClass.theClass.name}"

        assert this.metaClass.theClass.name != null

        return "${this.metaClass.theClass.name}"
    }

    /** */
    public void setNameSpace(String ns) {
        this.nameSpaceOverride = ns
    }

}

