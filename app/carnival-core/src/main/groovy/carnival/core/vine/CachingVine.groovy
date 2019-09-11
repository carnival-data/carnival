package carnival.core.vine



import groovy.transform.InheritConstructors

import static groovyx.gpars.dataflow.Dataflow.task
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowBroadcast

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper
import au.com.bytecode.opencsv.CSVWriter

import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.Defaults
import carnival.core.graph.query.QueryProcess




/**
 * The CachingVine trait can be added to a VineMethod to give it caching vine
 * functionality.
 *
 */
trait CachingVine {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * IGNORE - cache files are never used
     * OPTIONAL - cache files are used if they exist
     * REQUIRED - cache files are required. an exception is thrown is they do
     *            not exist
     */ 
    static enum CacheMode {IGNORE, OPTIONAL, REQUIRED}

    // loggers
    static final Logger log = LoggerFactory.getLogger('carnival')
    static final Logger qlog = LoggerFactory.getLogger('carnival-query-updates')


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY - TIME TO COMPLETION
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static public void logTimeToCompletion(QueryProcess qp) {
        log.trace "logTimeToCompletion qp:${qp.name}"

        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        pw.println ""
        logTimeToCompletion(qp, pw, 0)

        def str = sw.toString()
        qlog.info(str)
    }


    /** */
    static public void logTimeToCompletion(QueryProcess qp, PrintWriter pw, int indent) {
        // print indent
        def istr = ""
        (0..indent).each { istr += "    " }
        pw.print istr

        // all sub processes
        def subs = qp.subProcesses

        // special case the fully complete situation
        /*if (!subs.find({!it.completed})) pw.println "${qp.name}: completed"
        else pw.println "${qp.name}: ${completionMsg(qp)}"*/
        
        // message for this process
        pw.println "${qp.name}: ${completionMsg(qp)}"

        // increase the indend and print sub proc times
        indent++
        subs.each { logTimeToCompletion(it, pw, indent) }
    }


    /** */
    static public String completionMsg(QueryProcess qp) {
        if (qp.completed && qp.success) return "completed"
        else if (qp.completed && !qp.success) return "completed unsuccessful"
        else if (qp.timeEstimator.timeToCompletion) return "${qp.timeEstimator.timeToCompletionAsString}"
        else return "unknown"
    } 


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * the cache mode of this vine... should this be passed in to methods,
     * rather than be a field of the vine?
     */
    CacheMode cacheMode = CacheMode.OPTIONAL

    /** the cache directory used by this vine */
    File cacheDirectory = Defaults.dataCacheDirectory

    /** the target directory used by this vine */
    File targetDirectory = Defaults.targetDirectory

    /** ant builder */
    def ant = new AntBuilder()



    ///////////////////////////////////////////////////////////////////////////
    // CACHE MODE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public CacheMode getCacheMode() {
        return cacheMode
    }


    /** */
    public CachingVine setCacheMode(CacheMode cm) {
        this.cacheMode = cm
        return this
    }


    /** */
    Object withCacheMode(CacheMode mode, Closure cl) {
        def priorCacheMode = this.cacheMode
        def rval
        try {
            this.cacheMode = mode
            cl(this)
        } finally {
            this.cacheMode = priorCacheMode
        }
        rval
    }


    ///////////////////////////////////////////////////////////////////////////
    // CALL METHODS -- ASYNC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Promise callAsync(String methodName, Map methodArgs, Map asyncMap) {
        assert methodName
        assert methodArgs != null
        assert asyncMap != null

        CachingVine vine = this
        if (asyncMap.queryProcess) methodArgs.queryProcess = asyncMap.queryProcess

        task {
            try {
                asyncMap.dataTable = vine.call(methodName, methodArgs)
            } catch (Throwable t) {
                log.error("CachingVine.callAsync $methodName", t)
                if (asyncMap.queryProcess) asyncMap.queryProcess.fail(t)
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // CALL METHODS
    ///////////////////////////////////////////////////////////////////////////


    /** */
    public DataTable callWithMonitorThread(String methodName, Map methodArgs = [:]) {
        log.trace "CachingVine.callWithMonitorThread methodName:${methodName}"
        def vm = createVineMethodInstance(methodName)
        QueryProcess qp = new QueryProcess(methodName)
        vm.vineMethodQueryProcess = qp
        vm.useMonitorThread = true
        return call(vm, methodArgs)
    }


    /**
     * Call a vine method by name and arguments and return the result.
     *
     */
    public DataTable call(String methodName, Map methodArgs = [:]) {
        log.trace "CachingVine.call methodName:${methodName}"
        def vm = createVineMethodInstance(methodName)
        return call(vm, methodArgs)
    }


    /** */
    public DataTable call(VineMethod vm, Map methodArgs = [:]) {
        log.trace "CachingVine.call vm:${vm}"

        // strip out the queryProcess argument, if present
        if (methodArgs.containsKey('queryProcess')) {
            vm.vineMethodQueryProcess = methodArgs.queryProcess
            methodArgs.remove('queryProcess')
        }

        // switch on cache mode
        def mdt
        switch (cacheMode) {
            case CacheMode.IGNORE: mdt = callCacheModeIgnore(vm, methodArgs); break;
            case CacheMode.OPTIONAL: mdt = callCacheModeOptional(vm, methodArgs); break;
            case CacheMode.REQUIRED: mdt = callCacheModeRequired(vm, methodArgs); break;

            default:
            throw new RuntimeException("unrecognized cache mode: $cacheMode")
        }

        return mdt
    }


    /**
     * Call a vine method in the IGNORE cache mode where cache files are written,
     * but never read.
     *
     */
    private DataTable callCacheModeIgnore(VineMethod vm, Map methodArgs = [:]) {
        log.trace "callCacheModeIgnore vm: ${vm.meta(methodArgs).name}"

        // fetch the data
        return fetchVineMethodData(vm, methodArgs)
    }


    /** */
    public Map findFiles(VineMethod vm, Map methodArgs = [:]) {
        def name = vm.meta(methodArgs).name
        def files = DataTable.findFiles(cacheDirectory, name)
        return files
    }    


    /**
     * Call a vine method in the OPTIONAL cache mode where cache files are used
     * to create the DataTable if the cache files exist.
     *
     */
    private DataTable callCacheModeOptional(VineMethod vm, Map methodArgs = [:]) {
        log.trace "callCacheModeOptional vm: ${vm.meta(methodArgs).name}"

        // check for cached data
        def name = vm.meta(methodArgs).name
        def files = DataTable.findFiles(cacheDirectory, name)

        // if files exist, use them to build output
        if (files) {
            log.trace "callCacheModeOptional vm: ${name} - using cached data"
            def mdt = vm.returnType.createFromFiles(cacheDirectory, name)
            if (checkCacheValidity(files, mdt, vm, methodArgs)) {
                if (vm.vineMethodQueryProcess) {
                    vm.vineMethodQueryProcess.stop()
                }
                return mdt
            }
        }

        // if could not build from cache files, fetch data
        log.trace "callCacheModeOptional vm: ${vm.meta(methodArgs).name} - fetching data"
        return fetchVineMethodData(vm, methodArgs)
    }


    /**
     * Call a vine method in the REQUIRED cache mode where cache files must 
     * exist and if they do not, an exception is thrown.
     *
     */
    private DataTable callCacheModeRequired(VineMethod vm, Map methodArgs = [:]) {
        log.trace "callCacheModeRequired vm: ${vm.meta(methodArgs).name}"

        // check for cached data
        def name = vm.meta(methodArgs).name
        Map files = DataTable.findFiles(cacheDirectory, name)

        // if no cache data, throw exception
        if (!files) throw CacheFilesNotFoundException.create(this, vm, methodArgs)

        // build from cache files
        def mdt = vm.returnType.createFromFiles(cacheDirectory, name)
        def vr = checkCacheValidity(files, mdt, vm, methodArgs)
        if (!vr.validCache) throw CacheFilesInvalidException.create(this, mdt, vm, vr.message, methodArgs)
        return mdt
    }


    /** */
    public Map checkCacheValidity(Map files, DataTable cachedDataTable, VineMethod vm, Map methodArgs = [:]) {
        //log.trace "--checkCacheValidity(..., $cachedDataTable, ..., $methodArgs)"

        assert cachedDataTable.vine : "CachingVine.checkCacheValidity(${files}, ..., ${vm}, ...) failed, cachedDataTable.vine not set.  Expected vine method class: ${vm.class}"
        assert cachedDataTable.vine.name 
        assert cachedDataTable.vine.method
        assert cachedDataTable.vine.args instanceof Map

        def validCache = true
        def argsToIgnore = ['logPrefix']
        StringWriter message = new StringWriter()
        PrintWriter err = new PrintWriter(message)

        methodArgs.each { k, v ->

            log.trace "\n\n\ncheckCacheValidity $k $v\n\n\n"

            // TODO: hack - pmbbPds.pheCodeAssignments(vmargs) takes an argument 'logPrefix' which is only used to 
            // modify log output.  Ignore this argument for now.  In the future, either remove this argument
            // or add some way for a vineMethod to indicate which arguments should be ignored when calculating
            // cache validity.
            if (argsToIgnore.contains(k)) {
                log.trace "CachingVine.checkCacheValidity ignoring argument $k"
                return
            }

            // get the cached argument value
            def cv = cachedDataTable.vine.args.get(k)

            // if both are null, then there are no conflicts
            if (cv == null && v == null) return

            // if one is null and the other is not, then we have a conflict
            if (cv == null || v == null) {
                validCache = false
                def msg = "Warning: CachingVine.checkCacheValidity($files, ..., $vm, ...) - Cache not valid, cache value = $cv, arg value = $v"
                log.warn msg
                return
            }

            // TODO: hack - for now, do set equality on all collections.  
            // Need to remove this and update vineMethod argument checkers to check types, and 
            // enforce that non-ordered arguments must be sets.
            if (v instanceof Collection) {
                Set cachedArgValAsSet = cv.toSet()
                Set inputArgValAsSet = v.toSet()

                if (cachedArgValAsSet.size() != inputArgValAsSet.size()) {
                    validCache = false
                    def msg = "CachingVine.checkCacheValidity($files, ..., $vm, ...) - Cache and input arg set size mismatch. cache:${cachedArgValAsSet.size()} input:${inputArgValAsSet.size()}"
                    log.warn msg
                }

                Set cacheYesInputNo = new HashSet()
                cachedArgValAsSet.each { 
                    if (!inputArgValAsSet.contains(it)) {
                        cacheYesInputNo << it
                    }
                }
                //def cacheYesInputNo = cachedArgValAsSet.minus(inputArgValAsSet)
                if (cacheYesInputNo.size() > 0) {
                    validCache = false
                    def msg = "CachingVine.checkCacheValidity($files, ..., $vm, ...) - The following items were found in cache but not input: $cacheYesInputNo"
                    log.warn msg
                }

                Set inputYesCacheNo = new HashSet()
                inputArgValAsSet.each {
                    if (!cachedArgValAsSet.contains(it)) {
                        inputYesCacheNo << it
                    }
                }
                //def inputYesCacheNo = inputArgValAsSet.minus(cachedArgValAsSet)
                if (inputYesCacheNo.size() > 0) {
                    validCache = false
                    def msg = "CachingVine.checkCacheValidity($files, ..., $vm, ...) - The following items were found in input but not cache: $inputYesCacheNo"
                    log.warn msg
                }

                /*inputArgValAsSet.each { cav ->
                    if (!cachedArgValAsSet.contains(cav)) {
                        validCache = false
                        def msg = "CachingVine.checkCacheValidity($files, ..., $vm, ...) - Could not find cache value $cav in input set"
                        log.warn msg
                    }
                }*/

                /*if (!cv.toSet().equals(v.toSet())) {
                    validCache = false
                    def msg = "CachingVine.checkCacheValidity($files, ..., $vm, ...) - Cache not valid, input args do not match cache args when cast as set: \n\t ${cv}.asSet() != ${v}.asSet() || ${cv?.class} != ${v?.class} || ${cv?.first()?.class} != ${v?.first()?.class}"
                    log.warn msg
                }
                */
                return
            }

            // compare at the single object or literal level
            if (!cv.equals(v)) {
                validCache = false
                def msg = "CachingVine.checkCacheValidity($files, ..., $vm, ...) - Cache not valid, input args do not match cache args: \n\t${cv} != $v || ${cv?.class} != ${v?.class}"
                log.warn msg
                return
            }
        }

        err.close()

        return [
            validCache:validCache,
            message:message.toString()
        ]
    }


    /**
     * Helper method that uses the VineMethod to fetch fresh data.
     *
     */
    private DataTable fetchVineMethodData(VineMethod vm, Map methodArgs = [:]) {
        log.trace "fetchVineMethodData vm: ${vm.meta(methodArgs).name}"

        // if there is a query process, start it
        if (vm.vineMethodQueryProcess) vm.vineMethodQueryProcess.start()

        // fetch the data
        def dataTable
        try {
            if (vm.vineMethodQueryProcess && vm.useMonitorThread) vm.vineMethodQueryProcess.startMonitorThread()
            dataTable = vm.fetch(methodArgs)
        } finally {
            if (vm.vineMethodQueryProcess) vm.vineMethodQueryProcess.stop()
        }
        log.trace "fetchVineMethodData dataTable: ${dataTable.name}"

        // write files
        def files = dataTable.writeFiles(cacheDirectory)
        files.each { file ->
            ant.copy( 
                file:file, 
                todir:targetDirectory
            )        
        }
        //dataTable.writeFiles(targetDirectory)
        
        // make sure vine data has been set
        assert dataTable.vine : "CachingVine.fetchVineMethodData($vm, ...) failed, dataTable.vine not set.  Ensure that the fetch method is calling VineMethod.createEmptyDataTable() to generate the initial dataTable."
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
        log.trace "CachingVine invoke method via methodMissing name:$name args:${args?.class?.name}"

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

        // getDeclaredClasses returns all the classes and interfaces that are members of the
        // given class. see: https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getDeclaredClasses--
        allSubClasses.addAll(Arrays.asList(this.class.getDeclaredClasses()));

        //log.debug "${this.class?.name} allSubClasses:"
        allSubClasses.each { cl ->
            //log.debug "cl: $cl ${cl.name}"
            //if (VineMethod.isAssignableFrom(cl)) log.debug "VineMethod.isAssignableFrom(cl)"
            //if (cl.isAssignableFrom(VineMethod)) log.debug "cl.isAssignableFrom(VineMethod)"
            //cl.interfaces.each { ifc ->
            //    log.debug "ifc: $ifc ${ifc.name}"
            //}
        }

        // get all member classes that are sub-classes of VineMethod
        def allVineMethodClasses = allSubClasses.findAll { cl -> VineMethod.isAssignableFrom(cl) }
        //log.debug "allVineMethodClasses"
        //allVineMethodClasses.each { cl -> log.debug "log. $cl ${cl.name} ${cl.simpleName}" }

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
        log.trace "CachingVine.createVineMethodInstance methodName:${methodName}"

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
        log.trace "CachingVine.createDataTableMetaData methodName:${methodName}"

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
        log.trace "CachingVine.createMappedDataTable methodName:${methodName}"

        def vm = createVineMethodInstance(methodName)
        return vm.createEmptyDataTable(methodArgs)
    }


    /**
     * Create an empty generic data table for the given vine method with the
     * given args.  Useful for testing.
     *
     */
    public GenericDataTable createGenericDataTable(String methodName, Map methodArgs = [:]) {
        log.trace "CachingVine.createGenericDataTable methodName:${methodName}"

        // create a vine method instance
        def vm = createVineMethodInstance(methodName)
        return vm.createEmptyDataTable(methodArgs)
    }


    /** */
    public DataTable setTestCache(Map args) {
        assert args.tag
        assert args.sourceDataFileName
        assert args.methodName

        // set up test data cache directory
        def tagd = args.tag.replaceAll(' ', '-').trim()
        def ant = new AntBuilder()
        def testdir = new File("build/test/${tagd}")
        if (!testdir.exists()) ant.mkdir(dir:testdir)

        // set cacheDirectory of vine
        if (this.cacheDirectory != testdir) {
            def curDir = this.cacheDirectory
            this.cacheDirectory = testdir
            log.warn "setting cacheDirectory of ${this.class.simpleName} from ${curDir.canonicalPath} to ${this.cacheDirectory.canonicalPath}"
        }

        // create an empty data table
        def vm = this.createVineMethodInstance(args.methodName)

        def methodArgs = args.methodArgs ?: [:]

        def dt = vm.createEmptyDataTable(methodArgs)

        // write the meta file to the test directory
        dt.writeMetaFile(testdir)

        // copy the data file to the test directory
        DataTable.MetaData meta = vm.meta(methodArgs)
        def sourceName = args.sourceDataFileName
        if (!sourceName.endsWith('.csv')) sourceName += '.csv'
        ant.copy( 
            file:"data/test/${tagd}/${sourceName}", 
            tofile:"${testdir.canonicalPath}/${meta.name}.csv"
        )

        // re-create a data table from the copied data
        def dtFromDisk = dt.class.createFromFiles(testdir, dt.name)
        assert dtFromDisk.data.size() > 0

        // return a result
        return dtFromDisk
    }    

}




/**
 * CacheFilesNotFoundException is thrown if cache files are expected to exist,
 * but do not.
 *
 */
@InheritConstructors
class CacheFilesNotFoundException extends Exception {

    /**
     * Convenience CacheFilesNotFoundException creation method.
     *
     */
    static public create(CachingVine vine, VineMethod vm, Map methodArgs = [:]) {
        def name = vm.meta(methodArgs).name
        def cacheDirectory = vine.cacheDirectory
        def files = DataTable.files(cacheDirectory, name)

        def msg = "cache files not found: ${files.meta.canonicalPath} ${files.data.canonicalPath}"
        new CacheFilesNotFoundException(msg)
    }

}


/** */
@InheritConstructors
class CacheFilesInvalidException extends Exception {

    /**
     * Convenience CacheFilesInvalidException creation method.
     *
     */
    static public create(CachingVine vine, DataTable mdt, VineMethod vm, String message, Map methodArgs = [:]) {
        def name = vm.meta(methodArgs).name
        def cacheDirectory = vine.cacheDirectory
        def files = DataTable.files(cacheDirectory, name)

        def msg = new StringWriter()
        def pw = new PrintWriter(msg)

        pw.println "cache files are not valid with the current arguments: ${files.meta.canonicalPath} ${files.data.canonicalPath}.  Cache args: ${mdt.vine.args} Expected args: $methodArgs"
        pw.println message

        pw.close()

        new CacheFilesInvalidException(msg.toString())
    }

}







