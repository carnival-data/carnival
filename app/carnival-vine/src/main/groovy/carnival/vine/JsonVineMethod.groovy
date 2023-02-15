package carnival.vine



import groovy.util.logging.Slf4j




/**
 * JsonVineMethod is a parameterized abstract that concretizes the call(...)
 * methods of VineMethod where the data representation format is JSON, such as
 * data returned from a REST API call.
 *
 * The fetch() method is left un-implemented.  It is expected that client code
 * will extend this class to implement their fetch business logic.
 *
 */
@Slf4j
abstract class JsonVineMethod<T> extends VineMethod {


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The vine method logic.
     * @param args Arguments provided to the method logic.
     * @return A typed object representing the result.
     */
    abstract T fetch(Map args)



    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set the arguments then call the vine method logic.
     * @see call()
     * @param args Map of arguments
     * @return A vine method call object
     */
    JsonVineMethodCall<T> call(Map args) {
        assert args != null
        this.arguments = args
        call()
    }


    /**
     * Set the cache mode then call the vine method logic.
     * @see call()
     * @param cacheMode The CacheMode to use
     * @return A vine method call object
     */
    JsonVineMethodCall<T> call(CacheMode cacheMode) {
        assert cacheMode != null
        this.cacheMode = cacheMode
        call()
    }


    /**
     * Set the cache mode and arguments then call the vine method logic.
     * @see call()
     * @param cacheMode The CacheMode to use
     * @param args Map of arguments
     * @return A vine method call object
     */
    JsonVineMethodCall<T> call(CacheMode cacheMode, Map args) {
        assert cacheMode != null
        assert args != null
        this.cacheMode = cacheMode
        this.arguments = args
        call()
    }


    /**
     * Call the vine method logic respecting the cache mode.
     * @return A vine method call object.
     */
    JsonVineMethodCall<T> call() {
        assert this.cacheMode != null
        assert this.arguments != null

        JsonVineMethodCall<T> methodCall
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
     * Call _fetchAndCache() and return the result.
     * @see _fetchAndCache()
     * @return A vine method call object.
     */
    JsonVineMethodCall<T> _callCacheModeIgnore() {
        _fetchAndCache()
    }


    /**
     * Look for a cache file; if present, return a result from it; if not call
     * _fetchAndCache() and return the result.
     * @return A vine method call object.
     */
    JsonVineMethodCall<T> _callCacheModeOptional() {
        _cacheDirectoryInitialize()
        JsonVineMethodCall<T> methodCall
        File cacheFile = findCacheFile()
        if (cacheFile) {
            methodCall = _readFromCache(cacheFile)
        } else {
            methodCall = _fetchAndCache()
        }
        methodCall
    }


    /**
     * Assert that a cache file exists, then return a result from it.
     * @return A vine method call.
     */
    JsonVineMethodCall<T> _callCacheModeRequired() {
        _cacheDirectoryInitialize()

        final String EXT = "cache-mode 'required' requires an existing cache file."

        File cacheDir = _cacheDirectoryValidated()
        if (cacheDir == null) throw new RuntimeException("cache directory is invalid. ${cacheDir} ${EXT}")
        if (!cacheDir.canRead()) throw new RuntimeException("cache directory is not readable. ${cacheDir} ${EXT}")

        File cacheFile = JsonVineMethodCall.findFile(cacheDir, this.class, this.arguments)
        if (cacheFile == null) throw new RuntimeException("cache file does not exist. ${EXT}")        
        if (!cacheFile.exists()) throw new RuntimeException("cache file does not exist. ${EXT}")
        if (!cacheFile.isFile()) throw new RuntimeException("cache file is not a regular file. ${EXT}")
        if (!cacheFile.canRead()) throw new RuntimeException("cache file is not readable. ${EXT}")

        _readFromCache(cacheFile)
    }


    /**
     * Execute the vine method logic, cache the result, and return the result.
     * @return A vine method call object.
     */
    JsonVineMethodCall<T> _fetchAndCache() {
        T fetchResult = fetch(this.arguments)
        JsonVineMethodCall<T> methodCall = _createCallObject(this.arguments, fetchResult)
        _writeCacheFile(methodCall)
        methodCall        
    }


    /**
     * Return a result from the provided cache file.
     * @see JsonVineMethodCall#createFromFile(java.io.File)
     * @param cacheFile The cache file.
     * @return A vine method call objet.
     */
    JsonVineMethodCall<T> _readFromCache(File cacheFile) {
        assert cacheFile.exists()
        assert cacheFile.canRead()
        JsonVineMethodCall.createFromFile(cacheFile)
    }


    /**
     * Write the cache file for the provided vine method call object.
     * @param methodCall The vine method call object.
     * @return The list of files written.
     */
    List<File> _writeCacheFile(JsonVineMethodCall<T> methodCall) {
        File cacheDir = _cacheDirectoryValidated()
        if (cacheDir == null) {
            log.warn "cache directory is invalid. no cache file will be written."
            return null
        }
        
        methodCall.writeFiles(cacheDir)
    }


    /**
     * Create a vine method call object given the provided map of arguments
     * and result.
     * @param arguments Map of arguments.
     * @param result A result object.
     * @return A vine method call object.
     */
    JsonVineMethodCall<T> _createCallObject(Map arguments, T result) {
        assert result != null
        JsonVineMethodCall<T> mc 
        if (result instanceof List) mc = new JsonVineMethodListCall<T>()
        else mc = new JsonVineMethodCall<T>()
        //JsonVineMethodCall<T> mc = new JsonVineMethodCall<T>()
        mc.vineMethodClass = this.class
        mc.arguments = arguments
        mc.result = result
        mc
    }


    ///////////////////////////////////////////////////////////////////////////
    // CACHING
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Find the cache file inside the cache directory of this vine method
     * @return If the file exists, returns the file, otherwise null.
     */
    File findCacheFile() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. findCacheFile will return null."
            return null
        }
        JsonVineMethodCall.findFile(cacheDir, this.class, this.arguments)
    }

    /**
     * Return the cache file associated with the vine method
     * @return The cache file
     */
    File cacheFile() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. cacheFile will return null."
            return null
        }
        JsonVineMethodCall.file(cacheDir, this.class, this.arguments)
    }

}

