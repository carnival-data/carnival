package carnival.core.vineold



import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.DataTable



/**
 * Contains the seed of tests to check that the expected meta name is generated and that
 * live data can be queried
 */
trait CachingVineSpecTrait {

    /** */
    void assertExpectedVineMetaName(Vine vine, String methodName, Map methodArgs, String expectedDtName) {
        def vmc = vine.findVineMethodClass(methodName)
        assert vmc : "${vine.class.name}.findVineMethodClass(\"$methodName\") failed"
        
        def vm = vmc.newInstance()
        def mdtMeta = vm.meta(methodArgs)

        assert mdtMeta.name == expectedDtName
    }


    /** */
    @Deprecated
    void assertLiveDataFetchReturnsSomeData(Vine vine, String methodName, Map methodArgs, Boolean allowWarningWords = false, def minExpectedRecords = 5) {
        assertLiveDataFetchReturnsSomeData(vine, methodName, methodArgs, [allowWarningWords:false, minExpectedRecords:minExpectedRecords])
    }


    /**
     * Attempt to run the vine against live data and assert that some results are returned
     *
     * Vine vine -
     * String methodName - 
     * Map methods - 
     * 
     * Map resultCheckArgs (optional) -
     *   allowWarningWords (Boolean) - [false] allow warning words as dt data values or dt data keys 
     *   minExpectedRecords (int) - [5] minimun number of records that must be returned
     *   dataSourceDateOfUpdateMustBeSet (Boolean) - [false] assert that dt.dataSourceDateOfUpdate is not null
     */
    void assertLiveDataFetchReturnsSomeData(Vine vine, String methodName, Map methodArgs, Map resultCheckArgs) {
        if(!resultCheckArgs.containsKey('allowWarningWords')) resultCheckArgs.allowWarningWords = false
        if(!resultCheckArgs.containsKey('minExpectedRecords')) resultCheckArgs.minExpectedRecords = 5
        if(!resultCheckArgs.containsKey('dataSourceDateOfUpdateMustBeSet')) resultCheckArgs.dataSourceDateOfUpdateMustBeSet = false

        vine.cacheMode = CachingVine.CacheMode.IGNORE
        def dt = vine."${methodName}"(methodArgs)

        assert dt.data.size() >= resultCheckArgs.minExpectedRecords


        // check that the data does not contain the string "null" or any of the datatable keyword strings
        def reservedWords = [DataTable.MISSING, DataTable.NULL]
        def warningWords = DataTable.WARNINGS

        if (dt instanceof MappedDataTable) {
            dt.data.each { key, row ->
                assert !(key in reservedWords) : "The returned datatable contains key $key which is one of the reserved words: $reservedWords"
                assert row.values().disjoint(reservedWords) : "The returned datatable contains a row which contains reserved words: $reservedWords \nkey: $key\nrow: $row"

                if(!resultCheckArgs.allowWarningWords) {
                    assert !(key in warningWords) : "The returned datatable contains key $key which is one of the warning words: $warningWords"
                    assert row.values().disjoint(warningWords) : "The returned datatable contains a row which contains warning words: $warningWords \nkey: $key\nrow: $row"
                }
            }
        }
        else if (dt instanceof GenericDataTable) {
            dt.data.each { row ->

                assert row.values().disjoint(reservedWords) : "The returned datatable contains a row which contains reserved words: $reservedWords \nkey: $key\nrow: $row"

                if(!resultCheckArgs.allowWarningWords) {
                    assert row.values().disjoint(warningWords) : "The returned datatable contains a row which contains warning words: $warningWords \nkey: $key\nrow: $row"
                }
            }
        }
        else {
            assert false : "DataTable $dt has an unhandled class: ${dt.class}"
        }

        if (resultCheckArgs.dataSourceDateOfUpdateMustBeSet) assert dt.dataSourceDateOfUpdate
    }

}