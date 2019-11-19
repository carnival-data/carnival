package carnival.core.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.util.AntBuilder

import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.Defaults
import carnival.core.graph.query.QueryProcess



/**
 *
 *
 */
trait MappedDataTableVineMethod extends VineMethod {

    public Class getReturnType() { return MappedDataTable }

    public MappedDataTable createEmptyDataTable(Map methodArgs = [:]) {
        def mdt = new MappedDataTable(meta(methodArgs))
        if (methodArgs.containsKey('dateFormat')) {
        	//println "\n\n\nMappedDataTableVineMethod.createEmptyDataTable() methodArgs:${methodArgs}\n\n\n"
        	mdt.dateFormat = methodArgs.dateFormat
        }
        def vineData = generateVineData(methodArgs)
        assert vineData
        mdt.vine = vineData

        return mdt
    }

}


