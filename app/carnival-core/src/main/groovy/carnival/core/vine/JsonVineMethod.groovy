package carnival.core.vine



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils

import carnival.util.Defaults
import carnival.core.util.CoreUtil



/**
What do vine methods need to accomplish:
- implement the fetch operation
- expose the meta-data associated with a call to the vine method, which is needed to:
  - compute a cache file name
  - check cache validity
  - record the method call parameters
  - other details of the call, such as datetime

*/
@Slf4j
abstract class JsonVineMethod<T> {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    static final String DEFAULT_CACHE_MORE_CONFIG_KEY = 'carnival.cache-mode'


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    static public CacheMode defaultCacheMode() {
        String str = Defaults.getConfigValue(DEFAULT_CACHE_MORE_CONFIG_KEY)
        if (str == null) return CacheMode.IGNORE
        CacheMode cm = Enum.valueOf(CacheMode, str)
        if (cm) return cm
        CacheMode.IGNORE
    }


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    abstract T fetch(Map args)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    CacheMode cacheMode = defaultCacheMode()

    /** */
    Map arguments = [:]


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CACHE MODE
    ///////////////////////////////////////////////////////////////////////////

    JsonVineMethod<T> cacheMode(CacheMode cm) {
        assert cm != null
        this.cacheMode = cm
        this
    }

    JsonVineMethod<T> mode(CacheMode cm) {
        cacheMode(cm)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS ARGUMENTS
    ///////////////////////////////////////////////////////////////////////////

    JsonVineMethod<T> arguments(Map args) {
        this.arguments = args
        this
    }

    JsonVineMethod<T> args(Map args) {
        arguments(args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////


    JsonVineMethodCall<T> call(Map args) {
        assert args != null
        this.arguments = args
        call()
    }


    JsonVineMethodCall<T> call(CacheMode cacheMode) {
        assert cacheMode != null
        this.cacheMode = cacheMode
        call()
    }


    JsonVineMethodCall<T> call(CacheMode cacheMode, Map args) {
        assert cacheMode != null
        assert args != null
        this.cacheMode = cacheMode
        this.arguments = args
        call()
    }

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


    JsonVineMethodCall<T> _callCacheModeIgnore() {
        _fetchAndCache()
    }


    JsonVineMethodCall<T> _callCacheModeOptional() {
        JsonVineMethodCall<T> methodCall
        File cacheFile = findCacheFile()
        if (cacheFile) {
            methodCall = _readFromCache(cacheFile)
        } else {
            methodCall = _fetchAndCache()
        }
        methodCall
    }


    JsonVineMethodCall<T> _callCacheModeRequired() {
        final String EXT = "cache-mode 'required' requires an existing cache file."

        File cacheDir = _cacheDirectory()
        if (cacheDir == null) throw new RuntimeException("cache directory is null. ${EXT}")
        if (!cacheDir.exists()) throw new RuntimeException("cache directory does not exist. ${EXT}")
        if (!cacheDir.isDirectory()) throw new RuntimeException("cache directory is not a directory. ${EXT}")
        if (!cacheDir.canRead()) throw new RuntimeException("cache directory is not readable. ${EXT}")

        File cacheFile = JsonVineMethodCall.findFile(cacheDir, this.class, this.arguments)
        if (cacheFile == null) throw new RuntimeException("cache file does not exist. ${EXT}")        
        if (!cacheFile.exists()) throw new RuntimeException("cache file does not exist. ${EXT}")
        if (!cacheFile.isFile()) throw new RuntimeException("cache file is not a regular file. ${EXT}")
        if (!cacheFile.canRead()) throw new RuntimeException("cache file is not readable. ${EXT}")

        _readFromCache(cacheFile)
    }


    File findCacheFile() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. findCacheFile will return null."
            return null
        }
        JsonVineMethodCall.findFile(cacheDir, this.class, this.arguments)
    }


    File cacheFile() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. cacheFile will return null."
            return null
        }
        JsonVineMethodCall.file(cacheDir, this.class, this.arguments)
    }


    JsonVineMethodCall<T> _fetchAndCache() {
        T fetchResult = fetch(this.arguments)
        JsonVineMethodCall<T> methodCall = _createCallObject(this.arguments, fetchResult)
        _writeCacheFile(methodCall)
        methodCall        
    }


    JsonVineMethodCall<T> _readFromCache(File cacheFile) {
        assert cacheFile.exists()
        assert cacheFile.canRead()
        JsonVineMethodCall.createFromFile(cacheFile)
    }


    File _writeCacheFile(JsonVineMethodCall<T> methodCall) {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. no cache file will be written."
            return null
        }
        
        methodCall.writeFile(cacheDir)
    }


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


    File _cacheDirectory() {
        Defaults.getDataCacheDirectory()
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILE NAMES
    ///////////////////////////////////////////////////////////////////////////



}

