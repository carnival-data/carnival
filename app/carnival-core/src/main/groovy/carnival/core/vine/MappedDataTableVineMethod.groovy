package carnival.core.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.util.AntBuilder

import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.Defaults
import carnival.util.StringUtils



/** */
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

    public String computedName(Map args = [:]) {
        String cn = this.class.name
        //println "cn: $cn"

        def name = cn.reverse()
        if (name.contains('$')) name = name.substring(0, name.indexOf('$')) //name.takeBefore('$')
        else if (name.contains('.')) name = name.substring(0, name.indexOf('.')) // name.takeBefore('.')
        name = name.reverse()
        name = StringUtils.toKebabCase(name)
        //println "name: $name"

        return name
    }

}


/** A vine method with a default computed name */
trait SimpleMappedDataTableVineMethod extends MappedDataTableVineMethod {

    abstract String idFieldName()

    MappedDataTable.MetaData meta(Map args = [:]) {
        String name = computedName(args)
        new MappedDataTable.MetaData(
            name:name,
            idFieldName:idFieldName()
        ) 
    } 

}


/** A vine method with a uniquified name based on the arguments. */
trait UniquifiedMappedDataTableVineMethod extends MappedDataTableVineMethod {

    abstract String idFieldName()
    abstract String uniquifier(Map args)

    MappedDataTable.MetaData meta(Map args = [:]) {
        String name = computedName(args)
        String uniq = uniquifier(args)

        createUniqueMeta(
            name:name,
            idFieldName:idFieldName(),
            uniquifier:uniq
        ) 
    } 

}

