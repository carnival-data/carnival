// carnival dependencies
// the exclusions are required due to transitive dependencies
@GrabExclude(group='org.codehaus.groovy', module='groovy-swing')
@GrabExclude(group='org.codehaus.groovy', module='groovy-jsr223')
@GrabExclude(group='org.codehaus.groovy', module='groovy-nio')
@GrabExclude(group='org.codehaus.groovy', module='groovy-macro')
@Grab(group='edu.upenn.pmbb', module='carnival-core', version='0.2.6')


import carnival.util.GenericDataTable
import carnival.core.vine.CsvFileVine


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
