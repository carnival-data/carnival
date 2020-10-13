package carnival.core.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.Defaults



/** */
trait PartialCachingVineMethod {

	/** */
	final String PARTIAL = '-partial'

	/** */
	public DataTable createFromFiles(String dataTableName) {
		def dataTableClass = getReturnType()
		dataTableClass.createFromFiles(
			Defaults.dataCacheDirectory, 
			dataTableName
		)
	}

	/** */
	public Map findPartialFiles(Map methodArgs) {
        def dataTable = createEmptyDataTable(methodArgs)
        def dataTableBaseName = dataTable.name
		def partialName = dataTableBaseName + PARTIAL

        return DataTable.findFiles(Defaults.dataCacheDirectory, partialName)
	}


	/** */
	public getPartialDataTable(Map methodArgs) {
        def dataTable = createEmptyDataTable(methodArgs)
        def dataTableBaseName = dataTable.name
		dataTable.name += PARTIAL
		def existingFile = dataTable.findDataFile(Defaults.dataCacheDirectory)
		if (existingFile) dataTable = createFromFiles(dataTable.name)
		return dataTable
	}

	/** */
	public convertToFinal(DataTable dataTable) {
		def dataTableBaseName = dataTable.name.reverse().minus(PARTIAL.reverse()).reverse()
		//println "\n\n\ndataTableBaseName: ${dataTable.name} --> $dataTableBaseName\n\n\n"
		dataTable.name = dataTableBaseName
	}
}
