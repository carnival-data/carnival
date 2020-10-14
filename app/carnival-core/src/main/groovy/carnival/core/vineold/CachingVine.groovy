package carnival.core.vineold



import groovy.transform.InheritConstructors

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.util.DataTable
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.Defaults




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
    static final Logger log = LoggerFactory.getLogger(CachingVine)
    static final Logger qlog = LoggerFactory.getLogger('carnival-query-updates')


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

    /** ant builder */
    AntBuilder ant = new AntBuilder()



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
    // CALL METHODS
    ///////////////////////////////////////////////////////////////////////////


    /** 
     * This will take precedence over the similar method in Vine.
     *
     */
    public DataTable call(VineMethod vm, Map methodArgs = [:]) {
        log.trace "CachingVine.call vm:${vm}"

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

        def dataTable = fetchVineMethodData(vm, methodArgs)

        // write data to cache directory
        dataTable.writeFiles(cacheDirectory)

        // return result
        dataTable
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
                return mdt
            }
        }

        // if could not build from cache files, fetch data
        log.trace "callCacheModeOptional vm: ${vm.meta(methodArgs).name} - fetching data"
        def dataTable = fetchVineMethodData(vm, methodArgs)

        // write data to cache directory
        dataTable.writeFiles(cacheDirectory)

        // return result
        dataTable
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

            log.trace "checkCacheValidity $k $v".take(500)

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




    ///////////////////////////////////////////////////////////////////////////
    // TEST HELPERS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public DataTable setTestCache(Map args) {
        assert args.tag
        assert args.sourceDataFileName
        assert args.methodName

        // set up test data cache directory
        def tagd = args.tag.replaceAll(' ', '-').trim()
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


