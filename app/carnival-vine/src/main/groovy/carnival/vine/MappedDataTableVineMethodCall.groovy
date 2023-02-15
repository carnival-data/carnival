package carnival.vine



import java.io.BufferedReader
import java.io.StringReader
import groovy.util.logging.Slf4j

import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles
import carnival.util.DataTable





/**
 * A vine method call for a MappedDataTableVineMethod.
 */
@Slf4j
class MappedDataTableVineMethodCall extends DataTableVineMethodCall<MappedDataTable> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a vine method call object from the provided cache files.
     * @param cacheFiles The cache files.
     * @return A vine method call object.
     */
    static public MappedDataTableVineMethodCall createFromFiles(DataTableFiles cacheFiles) {
        MappedDataTable mdt = MappedDataTable.createFromFiles(cacheFiles)
        MappedDataTableVineMethodCall methodCall = new MappedDataTableVineMethodCall()
        methodCall.result = mdt
        methodCall.arguments = mdt.vine.arguments
        methodCall.vineMethodClass = Class.forName(mdt.vine.methodClass)
        methodCall
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the result returned by the call */
    MappedDataTable result


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - RESULT
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the data table result of this vine method call.
     * @return A data table object.
     */
    public MappedDataTable getResult() {
        return this.result
    }

    /**
     * Set the data table result of this vine method call.
     * @param result The data table result.
     */
    public void setResult(MappedDataTable result) {
        this.result = result
    }

}

