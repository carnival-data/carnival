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
 *
 *
 */
@Slf4j
abstract class MappedDataTableVineMethod extends DataTableVineMethod<MappedDataTable,MappedDataTableVineMethodCall> {


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////


    MappedDataTableVineMethodCall _readFromCache(DataTableFiles cacheFiles) {
        assert cacheFiles != null
        assert cacheFiles.exist()
        assert cacheFiles.areReadable()
        MappedDataTableVineMethodCall.createFromFiles(cacheFiles)
    }


    MappedDataTableVineMethodCall _createCallObject(Map arguments, MappedDataTable result) {
        assert arguments != null
        assert result != null

        MappedDataTableVineMethodCall mc = new MappedDataTableVineMethodCall()
        mc.vineMethodClass = this.class
        mc.arguments = arguments
        mc.result = result
        mc
    }


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



