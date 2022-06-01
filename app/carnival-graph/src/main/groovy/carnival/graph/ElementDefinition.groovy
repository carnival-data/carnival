package carnival.graph



import groovy.transform.ToString
import groovy.util.logging.Slf4j

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Property

import carnival.util.StringUtils
import carnival.graph.Base



/** 
 * Inherited by traits that can have PropertyDefs: VertexDefinition and EdgedDefTrait
 *
 * @see carnival.graph.PropertyDefinition
 * @see carnival.graph.EdgeDefinition
 */
@Slf4j
trait ElementDefinition extends WithPropertyDefsTrait {

    ///////////////////////////////////////////////////////////////////////////
    // GLOBAL
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * if true, the `Base.PX.NAME_SPACE` property for verticies in the graph
     * will use a global namespace value instead of one generated from 
     * the package name.
     */
    boolean global = false

    /** */
    boolean getGlobal() { this.global }

    /** */
    void setGlobal(boolean val) {
        this.global = val
    }

    /** */
    public boolean isGlobal() {
        return global
    }


    ///////////////////////////////////////////////////////////////////////////
    // PROPERTIES MUST BE DEFINED
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * if false, verticies created by this definition can contain properties
     * that were not defined by this VertexDefinition.
     * */
    Boolean propertiesMustBeDefined = true


    ///////////////////////////////////////////////////////////////////////////
    // NAME SPACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String getNameSpace() {
        if (this.global) return Base.GLOBAL_NAME_SPACE
        return getElementDefinitionClass()
    }


    ///////////////////////////////////////////////////////////////////////////
    // ELEMENT DEFINITION
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public String getElementDefinitionClass() {
        if (this instanceof Enum) return "${this.declaringClass.name}"
        return "${this.metaClass.theClass.name}"
    }

}
