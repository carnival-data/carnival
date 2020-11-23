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
class GenericDataTableVineMethodCall extends DataTableVineMethodCall<GenericDataTable> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
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

    public GenericDataTable getResult() {
        return this.result
    }

    public void setResult(GenericDataTable result) {
        this.result = result
    }

}