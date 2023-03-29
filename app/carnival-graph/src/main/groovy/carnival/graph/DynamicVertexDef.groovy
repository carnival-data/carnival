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



/** 
 * DynamicVertexDef is a VertexDefinition that is created in code outside the
 * normal model definition construction of enumerated classes tagged with 
 * @VertexModel.
 */
@ToString
@EqualsAndHashCode(allProperties=true)
class DynamicVertexDef implements VertexDefinition {

    /** The name of the vertex definition */
    String name

    /** Override the namespace calculated from the classpath */
    String nameSpaceOverride

    /** 
     * Private constructor.
     * @param name The name of the vertex definition.
     */
    private DynamicVertexDef(String name) {
        this.name = name
    }

    /** 
     * Returns the name of the vertex definition.
     * @return The name of this vertex definition
     */
    public String name() {
        return this.name
    }

    /** 
     * Returns the namespace of this vertex definition, which will be
     * calculated from the class if not overridden.
     * @return The name space of this vertex definition
     */
    @Override
    public String getNameSpace() {
        if (this.nameSpaceOverride != null) return this.nameSpaceOverride

        //log.debug "${this.metaClass}"
        //log.debug "${this.metaClass.theClass}"
        //log.debug "${this.metaClass.theClass.name}"

        assert this.metaClass.theClass.name != null

        return "${this.metaClass.theClass.name}"
    }

    /** 
     * Set the name space of this definition to the provided value, which will
     * be used in getNameSpace() rather than the calculated value.
     * @param ns The namespace to use for this definition
     */
    public void setNameSpace(String ns) {
        this.nameSpaceOverride = ns
    }

}

