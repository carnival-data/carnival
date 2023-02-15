package carnival.vine



import groovy.util.logging.Slf4j

import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles

import carnival.util.DataTable





/**
 * A vine method that returns a MappedDataTable as a result.
 *
 */
@Slf4j
abstract class MappedDataTableVineMethod extends DataTableVineMethod<MappedDataTable,MappedDataTableVineMethodCall> {


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a vine method call using the provided cache files.
     * @param cacheFiles The cache files.
     * @return A vine method call object.
     */
    MappedDataTableVineMethodCall _readFromCache(DataTableFiles cacheFiles) {
        assert cacheFiles != null
        assert cacheFiles.exist()
        assert cacheFiles.areReadable()
        MappedDataTableVineMethodCall.createFromFiles(cacheFiles)
    }


    /**
     * Create a vine method call object using the provided arguments and
     * result.
     * @param arguments Map of arguments.
     * @param result A data table result.
     * @return A vine method call object.
     */
    MappedDataTableVineMethodCall _createCallObject(Map arguments, MappedDataTable result) {
        assert arguments != null
        assert result != null

        MappedDataTableVineMethodCall mc = new MappedDataTableVineMethodCall()
        mc.vineMethodClass = this.class
        mc.arguments = arguments
        mc.result = result
        mc
    }


    /**
     * Create an empty data suitable for a result for this vine method.
     * @param args Map of args.
     * @return A data table object.
     */
    MappedDataTable createDataTable(Map args) {
        assert args != null
        assert args.idFieldName
        String idFieldName = args.idFieldName
        String name = DataTableVineMethodCall.computedName(this.class, this.arguments)
        new MappedDataTable(
            name:name,
            idFieldName:idFieldName,
            vine:[
                //vineClass:Vine.this.class.name,
                methodClass:this.class.name,
                arguments:this.arguments
            ]
        )
    }


}



