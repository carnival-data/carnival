package carnival.vine



import groovy.util.logging.Slf4j

import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles

import carnival.util.DataTable




/** 
 * GenericDataTableVineMethod is a partially concretized version of  
 * DataTableVineMethod to support vine methods that return a
 * GenericDataTable.
 *
 */
@Slf4j
abstract class GenericDataTableVineMethod extends DataTableVineMethod<GenericDataTable,GenericDataTableVineMethodCall> {


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return a vine method call object using the provided cache files.
     * @param cacheFiles The cache files.
     * @return A vine method call object.
     */
    GenericDataTableVineMethodCall _readFromCache(DataTableFiles cacheFiles) {
        log.trace "_readFromCache cacheFiles: ${cacheFiles}"
        assert cacheFiles != null
        assert cacheFiles.exist()
        assert cacheFiles.areReadable()
        GenericDataTableVineMethodCall.createFromFiles(cacheFiles)
    }


    /**
     * Create a vine method call object using the provided arguments and data
     * table result.
     * @param arguments Map of arguments.
     * @result Data table result.
     */
    GenericDataTableVineMethodCall _createCallObject(Map arguments, GenericDataTable result) {
        assert arguments != null
        assert result != null

        GenericDataTableVineMethodCall mc = new GenericDataTableVineMethodCall()
        mc.vineMethodClass = this.class
        mc.arguments = arguments
        mc.result = result
        mc
    }


    /**
     * Create an empty data table suitable as a result for this vine method.
     * @param args Optional map of arguments.
     * @return An empty data table object.
     */
    GenericDataTable createDataTable(Map args = [:]) {
        String name = DataTableVineMethodCall.computedName(this.class, this.arguments)
        new GenericDataTable(
            name:name,
            vine:[
                methodClass:this.class.name,
                arguments:this.arguments
            ]
        )
    }


}