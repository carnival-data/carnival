package carnival.core.vine



import java.io.BufferedReader
import java.io.StringReader
import groovy.util.logging.Slf4j

import carnival.core.util.CoreUtil
import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles
import carnival.util.DataTable






@Slf4j
class MappedDataTableVineMethodCall extends DataTableVineMethodCall<MappedDataTable> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
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

    public MappedDataTable getResult() {
        return this.result
    }

    public void setResult(MappedDataTable result) {
        this.result = result
    }

}

