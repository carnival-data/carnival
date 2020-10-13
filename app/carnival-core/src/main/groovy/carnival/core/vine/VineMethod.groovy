package carnival.core.vine



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils

import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.Defaults



/**
 * The VineMethod interface must be implemented by any vine methods that are to
 * be included in a CachingVine.  The fetch() method is analogous to the legacy
 * methods that would build features.  The name method is used by the
 * CachingVine machinery whenever a locally unique name is required, such as
 * constructing a cache file name or naming a DataTable.
 *
 */
@Slf4j
trait VineMethod {

    /** */
    Closure withSql

    /** */
    Vine enclosingVine

    /** */
    public getEnclosingVine() {
        return this.enclosingVine
    }


    /**
     * Return a the meta-data for this vine method.  See MappedDataTable and
     * GenericDataTable for implementations of DataTable.MetaData.
     *
     */
    abstract DataTable.MetaData meta(Map args)

    /**
     * Fetch the data using the given arguments.  It is expected that this
     * method will pull data from the soure directly via SQL or an API call.
     *
     */
    abstract DataTable fetch(Map args)

    /**
     * Returns an empty data table appropriate to the given method arguments.
     * 
     */
    abstract DataTable createEmptyDataTable(Map methodArgs)


    /**
     * Returns the class of DataTable returned by the vine method, currently
     * MappedDataTable or GenericDataTable.
     *
     */
    abstract Class getReturnType()


    /**
     * Create a meta-data object for the vine method identified by methodName
     * for the given args.
     *
     */
    public Map generateVineData(Map methodArgs = [:]) {
        //log.trace "VineMethod.generateVineData methodArgs:${methodArgs}"

        def name = this.class.getEnclosingClass().getName()
        def methodName = this.class.getSimpleName()

        def vine = [
            name:name,
            method:methodName,
            args:methodArgs
        ]

        return vine
    }


    public MappedDataTable.MetaData createUniqueMeta(Map args) {
        assert args.name
        assert args.idFieldName
        assert args.uniquifier

        String str = String.valueOf(args.uniquifier)
        String inputHash = DigestUtils.md5(str.bytes).encodeHex().toString()
        args.name = "${args.name}-${inputHash}"

        new MappedDataTable.MetaData(args)
    }

}

