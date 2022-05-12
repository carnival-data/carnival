package carnival.core.vine



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils

import carnival.core.config.Defaults
import carnival.core.util.CoreUtil
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


    GenericDataTableVineMethodCall _readFromCache(DataTableFiles cacheFiles) {
        assert cacheFiles != null
        assert cacheFiles.exist()
        assert cacheFiles.areReadable()
        GenericDataTableVineMethodCall.createFromFiles(cacheFiles)
    }


    GenericDataTableVineMethodCall _createCallObject(Map arguments, GenericDataTable result) {
        assert arguments != null
        assert result != null

        GenericDataTableVineMethodCall mc = new GenericDataTableVineMethodCall()
        mc.vineMethodClass = this.class
        mc.arguments = arguments
        mc.result = result
        mc
    }


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