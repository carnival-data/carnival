// carnival dependencies
// the exclusions are required due to transitive dependencies
@GrabExclude(group='org.codehaus.groovy', module='groovy-swing')
@GrabExclude(group='org.codehaus.groovy', module='groovy-jsr223')
@GrabExclude(group='org.codehaus.groovy', module='groovy-nio')
@GrabExclude(group='org.codehaus.groovy', module='groovy-macro')
@Grab(group='edu.upenn.pmbb', module='carnival-core', version='0.2.6')


// imports
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.VertexDefTrait
import carnival.graph.PropertyDefTrait
import carnival.core.vine.CsvFileVine
import carnival.core.graph.Core
import carnival.core.graph.CoreGraph
import carnival.core.graph.CoreGraphNeo4j
import carnival.core.graph.CoreGraphTinker
import carnival.core.graph.Reaper
import carnival.core.graph.ReaperMethodResource
import carnival.core.graph.DefaultReaper
import carnival.core.graph.DefaultReaperMethod


// create a carnival core graph
CoreGraph coreGraph = CoreGraphTinker.create()
def graph = coreGraph.graph
def g = graph.traversal()


// utility method
printGraph = {
	println "vertices (n=${g.V().count().next()})"
	g.V().each { println "${it} ${it.label()}" }
}


// print the initial graph
printGraph()


// write the files that will be uses as source files in this demo
File data1File = new File("data-1.csv")
data1File.write """\
NAME,COLOR_HAIR,COLOR_EYE
alex,black,blue
amanda,brown,green
bob,brown,brown
"""


// get all records
def cohortVine = new CsvFileVine('data-1.csv')
def allRecs = cohortVine.allRecords()
int totalRecs = allRecs.data.size()
println "allRecs: $allRecs"
allRecs.dataIterator().each { println "$it" }



// model
enum VX implements VertexDefTrait {
    PERSON (vertexProperties:[Core.PX.NAME]), 
    HAIR (vertexProperties:[PX.COLOR]), 
    EYE (vertexProperties:[PX.COLOR])

    private VX() {}
    private VX(Map m) {m.each { k,v -> this."$k" = v }}
}

enum PX implements PropertyDefTrait {
    COLOR
}



// example reaper
class ExampleReaper extends DefaultReaper {

    @ReaperMethodResource
    CsvFileVine cohortVine

    public ExampleReaper(CoreGraph coreGraph, CsvFileVine cohortVine) {
    	super(coreGraph)
        assert cohortVine
        this.cohortVine = cohortVine
    }

    static class ExampleReaperMethod extends DefaultReaperMethod {
        Map reap(Map args = [:]) {
            println "ExampleReaperMethod.reap args: ${args.keySet()}"

            def graph = getGraph()
            def g = traversal()

			cohortVine.allRecords().dataIterator().each { rec ->
				println "rec: $rec"
				def pV = VX.PERSON.instance().withProperty(Core.PX.NAME, rec.NAME).createVertex(graph, g)
				if (rec.COLOR_HAIR) {
					def hV = VX.HAIR.instance().withProperty(PX.COLOR, rec.COLOR_HAIR).createVertex(graph, g)
					Core.EX.HAS_PART.relate(g, pV, hV)
				}
			}

            [graphModified:false]
        }
    }

}


// create an example reaper
def r1 = new ExampleReaper(coreGraph, cohortVine)
def res

// run an example reaper method
res = r1.method('ExampleReaperMethod').ensure()
printGraph()

// calling ensure again will have no effect
res = r1.method('ExampleReaperMethod').ensure()
printGraph()


// close the graph
coreGraph.graph.close()
