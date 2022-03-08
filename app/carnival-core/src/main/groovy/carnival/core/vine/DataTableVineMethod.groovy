package carnival.core.vine



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils

import carnival.util.Defaults
import carnival.core.util.CoreUtil
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

    abstract DataTable fetch(Map args)

    abstract U _readFromCache(DataTableFiles cacheFiles)

    abstract U _createCallObject(Map arguments, T result) 

    abstract T createDataTable(Map args) 


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    U call(Map args) {
        assert args != null
        this.arguments = args
        call()
    }


    U call(CacheMode cacheMode) {
        assert cacheMode != null
        this.cacheMode = cacheMode
        call()
    }


    U call(CacheMode cacheMode, Map args) {
        assert cacheMode != null
        assert args != null
        this.cacheMode = cacheMode
        this.arguments = args
        call()
    }


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



    U _callCacheModeIgnore() {
        _fetchAndCache()
    }


    U _callCacheModeOptional() {
        U methodCall
        DataTableFiles cacheFiles = findCacheFiles()
        if (cacheFiles) {
            methodCall = _readFromCache(cacheFiles)
        } else {
            methodCall = _fetchAndCache()
        }
        methodCall
    }


    U _callCacheModeRequired() {
        final String EXT = "cache-mode 'required' requires existing cache files."

        File cacheDir = _cacheDirectory()
        if (cacheDir == null) throw new RuntimeException("cache directory is null. ${EXT}")
        if (!cacheDir.exists()) throw new RuntimeException("cache directory does not exist. ${EXT}")
        if (!cacheDir.isDirectory()) throw new RuntimeException("cache directory is not a directory. ${EXT}")
        if (!cacheDir.canRead()) throw new RuntimeException("cache directory is not readable. ${EXT}")

        DataTableFiles cacheFiles = DataTableVineMethodCall.findFiles(cacheDir, this.class, this.arguments)
        if (cacheFiles == null || cacheFiles.areNull()) throw new RuntimeException("cache files are null. ${EXT}")        
        if (!cacheFiles.exist()) throw new RuntimeException("cache files do not exist. ${EXT}")
        if (!cacheFiles.areFiles()) throw new RuntimeException("cache files are not regular files. ${EXT}")
        if (!cacheFiles.areReadable()) throw new RuntimeException("cache files are not readable. ${EXT}")

        _readFromCache(cacheFile)
    }



    U _fetchAndCache() {
        T fetchResult = fetch(this.arguments)
        U methodCall = _createCallObject(this.arguments, fetchResult)
        _writeCacheFile(methodCall)
        methodCall        
    }


    List<File> _writeCacheFile(U methodCall) {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is not configured. no cache file will be written."
            return null
        }
        if (!cacheDir.exists()) {
            log.warn "cache directory does not exist. no cache files will be written. ${cacheDir}"
            return null
        }
        assert cacheDir.isDirectory()
        
        methodCall.writeFiles(cacheDir)
    }


    ///////////////////////////////////////////////////////////////////////////
    // CACHING
    ///////////////////////////////////////////////////////////////////////////

    File _cacheDirectory() {
        Defaults.getDataCacheDirectory()
    }


    DataTableFiles findCacheFiles() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. findCacheFile will return null."
            return null
        }
        DataTableVineMethodCall.findFiles(cacheDir, this.class, this.arguments)
    }
    

    DataTableFiles cacheFiles() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. cacheFile will return null."
            return null
        }
        DataTableFiles.create(cacheDir, DataTableVineMethodCall.computedName(this))
    }

}
