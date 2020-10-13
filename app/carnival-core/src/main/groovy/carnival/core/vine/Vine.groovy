package carnival.core.vine



import java.lang.reflect.Field

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.sql.GroovyRowResult

import carnival.util.CsvUtil
import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.core.util.CoreUtil



/**
 * Vine is the superclass of objects that interact read and write data to
 * data sources.
 *
 */
abstract class Vine {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** */
	static Logger log = LoggerFactory.getLogger(Vine)

    /** */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')
    


	/**
	 * Write the list of rows (as from a Groovy SQL query) to a CSV file.
	 * Note that it lower-cases the first row, which contains the field
	 * names.  This done only so that code that consumes the file can 
	 * assume lower case keys, which generally look better in a text
	 * editor.
	 *
	 */
    static File writeToCsvFile(List<GroovyRowResult> rows, String filename) {
        assert rows
        assert rows.size() > 0

        File file = new File(filename)
        PrintWriter pw = new PrintWriter(file);

        try {
            def keys = rows.first().keySet().toArray()
            pw.println keys.collect({ "${it.toLowerCase()}" }).join(",")

            rows.each { row ->
                def orderedValues = []
                keys.each { orderedValues << row[it] }
                orderedValues = orderedValues.collect { 
                    (it != null) ? "$it" : "" 
                }
                pw.println orderedValues.join(",")
            }
        } catch (Exception e) {
            log.error "writeToFile exception!!!"
            def epw = new PrintWriter(new File("${filename}-stacktrace.txt"))
            try {
                e.printStackTrace(epw)
            } finally {
                if (epw) epw.close()
            }
        } finally {
            if (pw) pw.close()
        }

        return file
    }



	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

    /** */
    public Vine() {
        // no-op
    }



    ///////////////////////////////////////////////////////////////////////////
    // CALL METHODS
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Call a vine method by name and arguments and return the result.
     *
     */
    public DataTable call(String methodName, Map methodArgs = [:]) {
        log.trace "Vine.call methodName:${methodName}"
        def vm = createVineMethodInstance(methodName)
        return call(vm, methodArgs)
    }


    /** */
    public DataTable call(VineMethod vm, Map methodArgs = [:]) {
        log.trace "Vine.call vm:${vm}"

        return fetchVineMethodData(vm, methodArgs)
    }


    /**
     * Helper method that uses the VineMethod to fetch fresh data.
     *
     */
    DataTable fetchVineMethodData(VineMethod vm, Map methodArgs = [:]) {
        log.trace "fetchVineMethodData vm: ${vm.meta(methodArgs).name}"

        // fetch the data
        def dataTable = vm.fetch(methodArgs)
        log.trace "fetchVineMethodData dataTable: ${dataTable.name}"
        
        // make sure vine data have been set
        assert dataTable.vine : "Vine.fetchVineMethodData($vm, ...) failed, dataTable.vine not set.  Ensure that the fetch method is calling VineMethod.createEmptyDataTable() to generate the initial dataTable."
        assert dataTable.vine.name 
        assert dataTable.vine.method
        assert dataTable.vine.args instanceof Map

        // return the mapped data table
        return dataTable
    }



    ///////////////////////////////////////////////////////////////////////////
    // METHOD MISSING - CONVENIENCE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * methodMissing is implemented to call VineMethods.  If a vine method is
     * found, it is called.  Otherwise, a MethodMissingException is thrown.
     *
     * Vine methods can have zero or 1 arguments.  It is expectd that vine
     * methods that need multiple arguments with have the single argument be a
     * map.
     *
     */
    def methodMissing(String name, def args) {
        log.trace "Vine invoke method via methodMissing name:$name args:${args?.class?.name}"

        if (findVineMethodClass(name)) {
            //def firstArg = (args != null && args.length > 0) ? args[0] : [:]
            if (args != null && args.length > 1) {
                throw new MissingMethodException(name, this.class, args)
            } else if (args != null && args.length == 1) {
                def firstArg = args[0]
                //log.debug "firstArg: $firstArg ${firstArg.class}"
                //log.debug "firstArg: ${firstArg.class}"
                return call(name, firstArg)
            } else {
                return call(name)
            }
        }

        throw new MissingMethodException(name, this.class, args)
    }



    ///////////////////////////////////////////////////////////////////////////
    // UTILITY - VINE METHOD CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get all vine method classes using introspection.
     *
     */
    public Collection<Class> getAllVineMethodClasses() {
        def allSubClasses = []

        allSubClasses.addAll(Arrays.asList(this.class.getDeclaredClasses()));
        def allVineMethodClasses = allSubClasses.findAll { cl -> VineMethod.isAssignableFrom(cl) }

        return allVineMethodClasses
    }


    /**
     * Find a vine method class by case insensitive matching of the name.
     *
     */
    public Class findVineMethodClass(String methodName) {
        def vmcs = getAllVineMethodClasses()
        def matches = vmcs.findAll { it.simpleName.toLowerCase() == methodName.toLowerCase() }
        if (matches.size() > 1) throw new RuntimeException("multiple matches for $methodName: ${matches}")
        if (matches.size() < 1) return null

        def match = matches.first()
        return match
    }


    /** */
    public VineMethod createVineMethodInstance(String methodName) {
        log.trace "Vine.createVineMethodInstance methodName:${methodName}"

        // find the vine method class
        def vmc = findVineMethodClass(methodName)
        if (!vmc) throw new MissingMethodException(methodName, this.class)
        log.trace "vine method class: ${vmc.name}"

        // create a vine method instance
        def vm = vmc.newInstance()

        // add the withSql method to the vine method so it can access the
        // data source
        vm.withSql = { Closure closure -> withSql(closure) }
        vm.enclosingVine = this   

        // agg getRedcapRecords method
        if (this.metaClass.respondsTo(this, "getRedcapRecords"))
            vm.metaClass.getRedcapRecords = { args -> getRedcapRecords(args) }

        // add the vine method resource to the vine method instance
        def classes = CoreUtil.allClasses(this)
        //log.debug "Vine classes: $classes"

        //log.debug "this.class: ${this.class}"
        List<Field> vineMethodResourceFields = []
        classes.each { cl ->
            for (Field field: cl.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(VineMethodResource.class)) {
                    vineMethodResourceFields << field
                }
            }
        }
        //log.debug "vineMethodResourceFields: $vineMethodResourceFields"

        vineMethodResourceFields.each { vmrf ->
            vm.metaClass."${vmrf.name}" = vmrf.get(this)
        }

        return vm
    }



    ///////////////////////////////////////////////////////////////////////////
    // TEST HELPERS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Create a meta-data object for the vine method identified by methodName
     * for the given args.
     *
     */
    public DataTable.MetaData createDataTableMetaData(String methodName, Map methodArgs = [:]) {
        log.trace "Vine.createDataTableMetaData methodName:${methodName}"

        // create a vine method instance
        def vm = createVineMethodInstance(methodName)

        // create an empty MappedDataTable
        def dtMeta = vm.meta(methodArgs)

        return dtMeta
    }


    /**
     * Create an empty mapped data table for the given vine method with the
     * given args.  Useful for testing.
     *
     */
    public MappedDataTable createMappedDataTable(String methodName, Map methodArgs = [:]) {
        log.trace "Vine.createMappedDataTable methodName:${methodName}"

        def vm = createVineMethodInstance(methodName)
        return vm.createEmptyDataTable(methodArgs)
    }


    /**
     * Create an empty generic data table for the given vine method with the
     * given args.  Useful for testing.
     *
     */
    public GenericDataTable createGenericDataTable(String methodName, Map methodArgs = [:]) {
        log.trace "Vine.createGenericDataTable methodName:${methodName}"

        // create a vine method instance
        def vm = createVineMethodInstance(methodName)
        return vm.createEmptyDataTable(methodArgs)
    }

}



