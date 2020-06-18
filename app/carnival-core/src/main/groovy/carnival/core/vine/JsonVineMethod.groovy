package carnival.core.vine



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils

import carnival.util.Defaults
import carnival.core.util.CoreUtil
import carnival.core.vine.CachingVine.CacheMode



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
        CacheMode cm = Enum.valueOf(CacheMode, str)
        if (cm == null) cm = CacheMode.IGNORE
        cm
    }


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    abstract T fetch(Map args)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    CacheMode cacheMode


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CACHE MODE
    ///////////////////////////////////////////////////////////////////////////

    CacheMode getCacheMode() {
        if (this.cacheMode != null) return this.cacheMode 
        else return defaultCacheMode()
    }

    CacheMode setCacheMode(CacheMode cm) {
        this.cacheMode = cm
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    JsonVineMethodCall<T> call(Map args) {
        CacheMode cm = getCacheMode()
        assert cm != null
        call(cm, args)
    }


    JsonVineMethodCall<T> call(CacheMode cacheMode, Map args) {
        JsonVineMethodCall<T> methodCall
        switch (cacheMode) {
            case CacheMode.IGNORE: methodCall = callCacheModeIgnore(args); break;
            case CacheMode.OPTIONAL: methodCall = callCacheModeOptional(args); break;
            case CacheMode.REQUIRED: methodCall = callCacheModeRequired(args); break;

            default:
            throw new RuntimeException("unrecognized cache mode: $cacheMode")
        }
        assert methodCall != null
        methodCall
    }

    JsonVineMethodCall<T> callCacheModeIgnore(Map args) {
        _fetchAndCache(args)
    }


    JsonVineMethodCall<T> callCacheModeOptional(Map args) {
        JsonVineMethodCall<T> methodCall
        File cacheFile = findCacheFile(args)
        if (cacheFile) {
            methodCall = _readFromCache(cacheFile)
        } else {
            methodCall = _fetchAndCache(args)
        }
        methodCall
    }


    JsonVineMethodCall<T> callCacheModeRequired(Map args) {
        final String EXT = "cache-mode 'required' requires an existing cache file."

        File cacheDir = _cacheDirectory()
        if (cacheDir == null) throw new RuntimeException("cache directory is null. ${EXT}")
        if (!cacheDir.exists()) throw new RuntimeException("cache directory does not exist. ${EXT}")
        if (!cacheDir.isDirectory()) throw new RuntimeException("cache directory is not a directory. ${EXT}")
        if (!cacheDir.canRead()) throw new RuntimeException("cache directory is not readable. ${EXT}")

        File cacheFile = JsonVineMethodCall.findFile(cacheDir, this.class, args)
        if (cacheFile == null) throw new RuntimeException("cache file does not exist. ${EXT}")        
        if (!cacheFile.exists()) throw new RuntimeException("cache file does not exist. ${EXT}")
        if (!cacheFile.isFile()) throw new RuntimeException("cache file is not a regular file. ${EXT}")
        if (!cacheFile.canRead()) throw new RuntimeException("cache file is not readable. ${EXT}")

        _readFromCache(cacheFile)
    }


    File findCacheFile(Map args) {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. findCacheFile will return null."
            return null
        }
        JsonVineMethodCall.findFile(cacheDir, this.class, args)
    }


    File cacheFile(Map args) {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. cacheFile will return null."
            return null
        }
        JsonVineMethodCall.file(cacheDir, this.class, args)
    }


    JsonVineMethodCall<T> _fetchAndCache(Map args) {
        T fetchResult = fetch(args)
        JsonVineMethodCall<T> methodCall = _createCallObject(args, fetchResult)
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
        JsonVineMethodCall<T> mc = new JsonVineMethodCall<T>()
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

