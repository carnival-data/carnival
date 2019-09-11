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
trait GenericDataTableVineMethod extends VineMethod {

    public Class getReturnType() { return GenericDataTable }

    public GenericDataTable createEmptyDataTable(Map methodArgs = [:]) {
        //log.trace "GenericDataTableVineMethod.createEmptyDataTable methodArgs:${methodArgs}"

        def mdt = new GenericDataTable(meta(methodArgs))
        def vineData = generateVineData(methodArgs)
        assert vineData
        mdt.vine = vineData

        return mdt
    }
    
}