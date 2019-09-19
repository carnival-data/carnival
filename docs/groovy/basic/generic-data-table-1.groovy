@Grab(group='edu.upenn.pmbb', module='carnival-util', version='0.2.6')

import carnival.util.GenericDataTable

def mdt = new GenericDataTable(
    name:"myMappedDataTable"
)
mdt.dataAdd(ID:'1A', NAME:'alex')
mdt.dataAdd(ID:'1A', NAME:'bob')

def currentDir = new File(System.getProperty("user.dir"))
mdt.writeFiles(currentDir)