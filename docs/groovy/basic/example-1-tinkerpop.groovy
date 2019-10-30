// carnival dependencies
// the exclusions are required due to transitive dependencies
@GrabExclude(group='org.codehaus.groovy', module='groovy-swing')
@GrabExclude(group='org.codehaus.groovy', module='groovy-jsr223')
@GrabExclude(group='org.codehaus.groovy', module='groovy-nio')
@GrabExclude(group='org.codehaus.groovy', module='groovy-macro')
@Grab(group='edu.upenn.pmbb', module='carnival-core', version='0.2.6')


// imports
import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

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
def graph = TinkerGraph.open()
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
CsvIterator csvIterator = parseCsv(data1File.text)
def fileData = []
csvIterator.each { row ->
    fileData << row.toMap()
}
println "fileData: $fileData"
fileData.each { println "$it" }



// write to graph
fileData.each { rec ->
    def pV = graph.addVertex(T.label, 'Person', 'name', rec.NAME)
    if (rec.COLOR_HAIR) {
        def hV = graph.addVertex(T.label, 'Hair', 'color', rec.COLOR_HAIR)
        pV.addEdge('has_part', hV)
    }
}
printGraph()



// close the graph
coreGraph.graph.close()
