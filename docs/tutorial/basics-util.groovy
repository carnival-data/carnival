@Grab(group='edu.upenn.pmbb', module='carnival-util', version='0.2.6')

import carnival.util.MappedDataTable
import carnival.util.KeyType

def mdt = new MappedDataTable(
    name:"myMappedDataTable",
    idFieldName:'ID',
    idKeyType:KeyType.GENERIC_STRING_ID
)

mdt.dataAdd(ID:'1A', NAME:'alex')
def currentDir = new File(System.getProperty("user.dir"))
mdt.writeFiles(currentDir)

