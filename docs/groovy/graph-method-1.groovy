///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('org.carnival:carnival-core:2.1.1-SNAPSHOT')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.10')



///////////////////////////////////////////////////////////////////////////////
// IMPORTS
///////////////////////////////////////////////////////////////////////////////

import groovy.transform.ToString
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.core.graph.CoreGraphTinker
import carnival.graph.VertexDefinition
import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod



///////////////////////////////////////////////////////////////////////////////
// CREATE A CARNIVAL
///////////////////////////////////////////////////////////////////////////////

def cg = CoreGraphTinker.create()
def graph = cg.graph



///////////////////////////////////////////////////////////////////////////////
// DEFINE A GRAPH MODEL
///////////////////////////////////////////////////////////////////////////////

@VertexDefinition
enum VX {
    RECORD (
        propertyDefs:[
            PX.NAME
        ]
    ),
    NAME (
        propertyDefs:[
            PX.FIRST,
            PX.LAST
        ]
    ),
}

@PropertyDefinition
enum PX {
    NAME,
    FIRST,
    LAST
}

@EdgeDefinition
enum EX {
    IS_DERIVED_FROM
}


///////////////////////////////////////////////////////////////////////////////
// ADD SOME TEST DATA
///////////////////////////////////////////////////////////////////////////////

VX.RECORD.instance().withProperty(PX.NAME, "John Smith ").create(graph)
VX.RECORD.instance().withProperty(PX.NAME, "Alice  Brown").create(graph)
VX.RECORD.instance().withProperty(PX.NAME, " Bob  ").create(graph)
VX.RECORD.instance().create(graph)



///////////////////////////////////////////////////////////////////////////////
// DEFINE AN EXAMPLE GRAPH METHOD
///////////////////////////////////////////////////////////////////////////////

/**
 * Example class that contains expanders, methods that extract data from
 * source records and expands
 *
 */
class Expanders implements GraphMethods {
    
    /**
     * Example graph method that cleans names by removing whitespace.
     *
     */
    class Name extends GraphMethod {

        /**
         * The execute method contains the logic of the graph method.
         *
         */
        void execute(Graph graph, GraphTraversalSource g) {
            
            g.V().isa(VX.RECORD).each { recV ->
                if (!PX.NAME.of(recV).isPresent()) return

                String val = PX.NAME.valueOf(recV)
                List<String> words = val.trim().split(/\s+/)
                if (words.size == 0) return

                Vertex nameV

                if (words.size == 1) {
                    nameV = VX.NAME.instance().withProperty(PX.FIRST, words[0]).ensure(graph, g)
                }
                if (words.size == 2) {
                    nameV = VX.NAME.instance().withProperties(
                        PX.FIRST, words[0],
                        PX.LAST, words[1]
                    ).ensure(graph, g)
                }

                EX.IS_DERIVED_FROM.instance().from(nameV).to(recV).ensure(g)
            }

        }
    }
}



///////////////////////////////////////////////////////////////////////////////
// RUN THE NAME EXPANDER GRAPH METHOD
// INSPECT THE RESULTS
///////////////////////////////////////////////////////////////////////////////

// the variable names graph2 and g2 are used because the variables graph and g
// already appear in this scope, which is a groovy script scope.  normally,
// the variable names graph and g would be used
cg.withTraversal { Graph graph2, GraphTraversalSource g2 ->
    Expanders expanders = new Expanders()
    expanders.method('Name').ensure(graph2, g2)

    g2.V()
        .isa(VX.RECORD).as('r')
        .in(EX.IS_DERIVED_FROM)
        .isa(VX.NAME).as('n')
        .select('r', 'n')
    .each { m ->
        print "${m.n}"
        PX.FIRST.of(m.n).ifPresent { print " ${it}" }
        PX.LAST.of(m.n).ifPresent { print " ${it}" }
        print " is derived from ${m.r}"
        print " \"${PX.NAME.valueOf(m.r)}\""
        println ""
    }
}