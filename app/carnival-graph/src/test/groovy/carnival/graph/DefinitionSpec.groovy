package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.graph.EdgeDefinition.Multiplicity




/**
 *
 */
class DefinitionSpec extends Specification {



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "parseLabel"() {
        when:
        String res = Definition.parseLabel(source)

        then:
        res == expected

        where:
        expected | source
        'Thing' | 'Thing0CarnivalGraphVertexdefinitionspecVx'
        'SomeThing' | 'SomeThing0CarnivalGraphVertexdefinitionspecVx'
        'Thing1' | 'Thing10CarnivalGraphVertexdefinitionspecVx'
        'Some0Thing' | 'Some0Thing0CarnivalGraphVertexdefinitionspecVx'        
    }

}

