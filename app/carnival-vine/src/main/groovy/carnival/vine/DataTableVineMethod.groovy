package carnival.vine



import groovy.util.logging.Slf4j

import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles

import carnival.util.DataTable



/**
 * DataTableVineMethod is a partial concretization of VineMethod for vine
 * methods that return a data table as a result.  It is not expected that
 * client code will extend this class, but rather the more concrete sub-classes
 * of GenericDataTableVineMethod or MappedDataTableVineMethod.
 *
 */
@Slf4j
abstract class DataTableVineMethod<T,U extends VineMethodCall> extends VineMethod { 

    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Executes the logic of this vine method.
     * @param Arguments for the vine method logic.
     * @return A DataTable object containing the results of the vime method
     *         logic.
     */
    abstract DataTable fetch(Map args)

    /**
     * Read the vine method result from the cache files.
     * @param cacheFiles The cache files.
     * @return The result data table.
     */
    abstract U _readFromCache(DataTableFiles cacheFiles)

    /**
     * Create the vine method call object representing a call to this vine
     * method.
     * @param arguments The map of argument used during the call.
     * @param result The result object of the call.
     * @return The vine method call object.
     */
    abstract U _createCallObject(Map arguments, T result) 

    /**
     * Create an empty data table for this vine methods if it were called using
     * the provided arguments.
     * @param args The map of arguments.
     * @return An empty data table.
     */
    abstract T createDataTable(Map args) 


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set the arguments to the provided ones and call the vine method.
     * @see #call()
     * @param args Map of arguments.
     * @return A vine method call object.
     */
    U call(Map args) {
        assert args != null
        this.arguments = args
        call()
    }


    /**
     * Set the cache mode to the provided one and call the vine method.
     * @see #call()
     * @param cacheMode The cache mode to use.
     * @return A vine method call object.
     */
    U call(CacheMode cacheMode) {
        assert cacheMode != null
        this.cacheMode = cacheMode
        call()
    }


    /**
     * Set the cache mode and arguments and call the vine method.
     * @see #call()
     * @param cacheMode The cache mode to use.
     * @param args Map of arguments.
     * @return A vine method call object.
     */
    U call(CacheMode cacheMode, Map args) {
        assert cacheMode != null
        assert args != null
        this.cacheMode = cacheMode
        this.arguments = args
        call()
    }


    /**
     * Call the vine method returning a vine method call object.  This method
     * respects the cache mode.
     * @return A vine method call object.
     */
    U call() {
        assert this.cacheMode != null
        assert this.arguments != null

        U methodCall
        switch (this.cacheMode) {
            case CacheMode.IGNORE: methodCall = _callCacheModeIgnore(); break;
            case CacheMode.OPTIONAL: methodCall = _callCacheModeOptional(); break;
            case CacheMode.REQUIRED: methodCall = _callCacheModeRequired(); break;

            default:
            throw new RuntimeException("unrecognized cache mode: ${this.cacheMode}")
        }
        assert methodCall != null
        methodCall    
    }


    /**
     * Call the _fetchAndCache() and return the vine method call object.
     * @see #_fetchAndCache()
     * @return A vine method call object
     */
    U _callCacheModeIgnore() {
        _fetchAndCache()
    }


    /**
     * Check for a cached result; return if present; if not present, call
     * _fetchAndCache() and return the resulting vine method call object.
     * @see #_fetchAndCache()
     */
    U _callCacheModeOptional() {
        _cacheDirectoryInitialize()

        U methodCall
        DataTableFiles cacheFiles = findCacheFiles()
        if (cacheFiles) {
            methodCall = _readFromCache(cacheFiles)
        } else {
            methodCall = _fetchAndCache()
        }
        
        methodCall
    }


    /**
     * Verify that a cached result exists and return it.
     * @return A vine method call object created from a cached result.
     */
    U _callCacheModeRequired() {
        _cacheDirectoryInitialize()

        final String EXT = "cache-mode 'required' requires existing cache files."

        File cacheDir = _cacheDirectoryValidated()
        if (cacheDir == null) throw new RuntimeException("cache directory invalid. ${cacheDir} ${EXT}")
        if (!cacheDir.canRead()) throw new RuntimeException("cache directory is not readable. ${cacheDir} ${EXT}")

        DataTableFiles cacheFiles = DataTableVineMethodCall.findFiles(cacheDir, this.class, this.arguments)
        if (cacheFiles == null || cacheFiles.areNull()) throw new RuntimeException("cache files are null. ${EXT}")        
        if (!cacheFiles.exist()) throw new RuntimeException("cache files do not exist. ${EXT}")
        if (!cacheFiles.areFiles()) throw new RuntimeException("cache files are not regular files. ${EXT}")
        if (!cacheFiles.areReadable()) throw new RuntimeException("cache files are not readable. ${EXT}")

        _readFromCache(cacheFile)
    }


    /**
     * Call fetch(), write the result to the cache, and return a vine method
     * call object.
     * @see #fetch(Map)
     * @return A vine method call object
     */
    U _fetchAndCache() {
        log.trace "_fetchAndCache"
        T fetchResult = fetch(this.arguments)
        U methodCall = _createCallObject(this.arguments, fetchResult)
        _writeCacheFile(methodCall)
        methodCall        
    }


    /**
     * Write the cache files to the cache directory for the provided method
     * call object.
     * @see DataTableVineMethodCall#writeFiles(java.io.File)
     * @param methodCall The method call object that will be written to the
     *                   cache directory.
     * @return The written cache files.
     */
    List<File> _writeCacheFile(U methodCall) {
        log.trace "_writeCacheFile methodCall:${methodCall?.class.simpleName}"
        File cacheDir = _cacheDirectoryValidated()
        log.trace "cacheDir:${cacheDir}"
        if (cacheDir == null) {
            log.warn "cache directory is invalid. no cache file will be written."
            return null
        }
        methodCall.writeFiles(cacheDir)
    }


    ///////////////////////////////////////////////////////////////////////////
    // CACHING
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Convenience method to call DataTableVineMethodCall.findFiles for this
     * vine method.
     * @see DataTableVineMethodCall#findFiles(java.io.File, Class, Map)
     * @return A DataTableFiles object.
     */
    DataTableFiles findCacheFiles() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. findCacheFile will return null."
            return null
        }
        DataTableVineMethodCall.findFiles(cacheDir, this.class, this.arguments)
    }
    

    /**
     * Convenience method to call DataTableFiles.create for this vine method.
     * @see carnival.util.DataTableFiles#create(java.io.File, String)
     * @return A DataTableFiles object.
     */
    DataTableFiles cacheFiles() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. cacheFile will return null."
            return null
        }
        DataTableFiles.create(cacheDir, DataTableVineMethodCall.computedName(this))
    }

}
