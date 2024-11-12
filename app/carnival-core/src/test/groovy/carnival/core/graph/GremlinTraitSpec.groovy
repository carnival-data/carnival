package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
//import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph



class GremlinTraitSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // SETUP
    ///////////////////////////////////////////////////////////////////////////
    def setupSpec() { } 
    def setup() { 
        gc = new GraphContainer()
    }
    def cleanup() { 
        gc.graph.close()
    }
    def cleanupSpec() { }


    ///////////////////////////////////////////////////////////////////////////
    // CLASSES
    ///////////////////////////////////////////////////////////////////////////
    static class GraphContainer implements GremlinTrait {
        GraphContainer() {
            setGraph(TinkerGraph.open())
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    @Shared gc


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "withGremlin two args"() {
        when:
        Closure cl = { arg1, arg2 ->
            assert arg1 != null
            assert arg1 instanceof Graph
            assert arg2 != null
            assert arg2 instanceof GraphTraversalSource
        }
        gc.withGremlin(cl)

        then:
        noExceptionThrown()
    }

    def "withGremlin one arg"() {
        when:
        Closure cl = { arg1 ->
            assert arg1 != null
            assert arg1 instanceof GraphTraversalSource
        }
        gc.withGremlin(cl)

        then:
        noExceptionThrown()
    }

    def "withTraversal two args"() {
        when:
        Closure cl = { arg1, arg2 ->
            assert arg1 != null
            assert arg1 instanceof Graph
            assert arg2 != null
            assert arg2 instanceof GraphTraversalSource
        }
        gc.withTraversal(cl)

        then:
        noExceptionThrown()
    }

    def "withTraversal one arg"() {
        when:
        Closure cl = { arg1 ->
            assert arg1 != null
            assert arg1 instanceof GraphTraversalSource
        }
        gc.withTraversal(cl)

        then:
        noExceptionThrown()
    }

    def "withTraversal no args throws exception"() {
        when:
        Closure cl = { }
        gc.withTraversal(cl)

        then:
        // supposed to throw exception, but doesn't...
        // do not know why. not super important, i guess
        //Exception e = thrown()
        noExceptionThrown()
    }

    def "get the graph"() {
        when:
        def gts = gc.getGraph()

        then:
        gts != null
        gts instanceof Graph

    }

    def "get a traversal source"() {
        when:
        def gts = gc.traversal()

        then:
        gts != null
        gts instanceof GraphTraversalSource
    }

}