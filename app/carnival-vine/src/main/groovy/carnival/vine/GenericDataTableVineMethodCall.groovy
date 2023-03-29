package carnival.vine



import java.io.BufferedReader
import java.io.StringReader
import groovy.util.logging.Slf4j

import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles
import carnival.util.DataTable



/**
 * A vine method call for a GenericDataTableVineMethod.
 */
@Slf4j
class GenericDataTableVineMethodCall extends DataTableVineMethodCall<GenericDataTable> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a vine method call object using the provided cache files.
     * @param cacheFiles The cache files.
     * @return A vine method call object.
     */
    static public GenericDataTableVineMethodCall createFromFiles(DataTableFiles cacheFiles) {
        GenericDataTable gdt = GenericDataTable.createFromFiles(cacheFiles)
        GenericDataTableVineMethodCall methodCall = new GenericDataTableVineMethodCall()
        methodCall.result = gdt
        methodCall.arguments = gdt.vine.arguments
        methodCall.vineMethodClass = Class.forName(gdt.vine.methodClass)
        methodCall
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the result returned by the call */
    GenericDataTable result


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - RESULT
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the data table result of this vine method call.
     * @return A data table.
     */
    public GenericDataTable getResult() {
        return this.result
    }


    /**
     * Set the data table result of this vine method call.
     * @param result The data table result.
     */
    public void setResult(GenericDataTable result) {
        this.result = result
    }

}