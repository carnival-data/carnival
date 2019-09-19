@Grab(group='edu.upenn.pmbb', module='carnival-util', version='0.2.6')

import carnival.util.MappedDataTable

def mdt = new MappedDataTable(
    name:"myMappedDataTable",
    idFieldName:'ID'
)
mdt.dataAdd(ID:'1A', NAME:'alex')
mdt.dataAdd(ID:'1A', NAME:'bob')
