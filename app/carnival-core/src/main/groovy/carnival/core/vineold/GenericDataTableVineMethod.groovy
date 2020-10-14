package carnival.core.vineold



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.Defaults



/**
 *
 *
 */
trait GenericDataTableVineMethod extends VineMethod {

    public Class getReturnType() { return GenericDataTable }

    public GenericDataTable createEmptyDataTable(Map methodArgs = [:]) {
        def mdt = new GenericDataTable(meta(methodArgs))
        def vineData = generateVineData(methodArgs)
        assert vineData
        mdt.vine = vineData

        return mdt
    }
    
}