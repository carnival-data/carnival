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
        //log.trace "MappedDataTableVineMethod.createEmptyDataTable methodArgs:${methodArgs}"

        def mdt = new MappedDataTable(meta(methodArgs))
        def vineData = generateVineData(methodArgs)
        assert vineData
        mdt.vine = vineData

        return mdt
    }

}


