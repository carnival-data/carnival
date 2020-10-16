package carnival.core.vine



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils

import carnival.util.Defaults
import carnival.core.util.CoreUtil
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles



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
abstract class MappedDataTableVineMethod extends VineMethod {


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    abstract MappedDataTable fetch(Map args)



    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////


    MappedDataTableVineMethodCall call(Map args) {
        assert args != null
        this.arguments = args
        call()
    }


    MappedDataTableVineMethodCall call(CacheMode cacheMode) {
        assert cacheMode != null
        this.cacheMode = cacheMode
        call()
    }


    MappedDataTableVineMethodCall call(CacheMode cacheMode, Map args) {
        assert cacheMode != null
        assert args != null
        this.cacheMode = cacheMode
        this.arguments = args
        call()
    }


    MappedDataTableVineMethodCall call() {
        assert this.cacheMode != null
        assert this.arguments != null

        MappedDataTableVineMethodCall methodCall
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



    MappedDataTableVineMethodCall _callCacheModeIgnore() {
        _fetchAndCache()
    }


    MappedDataTableVineMethodCall _callCacheModeOptional() {
        MappedDataTableVineMethodCall methodCall
        DataTableFiles cacheFiles = findCacheFiles()
        if (cacheFiles) {
            methodCall = _readFromCache(cacheFiles)
        } else {
            methodCall = _fetchAndCache()
        }
        methodCall
    }


    MappedDataTableVineMethodCall _callCacheModeRequired() {
        final String EXT = "cache-mode 'required' requires existing cache files."

        File cacheDir = _cacheDirectory()
        if (cacheDir == null) throw new RuntimeException("cache directory is null. ${EXT}")
        if (!cacheDir.exists()) throw new RuntimeException("cache directory does not exist. ${EXT}")
        if (!cacheDir.isDirectory()) throw new RuntimeException("cache directory is not a directory. ${EXT}")
        if (!cacheDir.canRead()) throw new RuntimeException("cache directory is not readable. ${EXT}")

        DataTableFiles cacheFiles = MappedDataTableVineMethodCall.findFiles(cacheDir, this.class, this.arguments)
        if (cacheFiles == null || cacheFiles.areNull()) throw new RuntimeException("cache files are null. ${EXT}")        
        if (!cacheFiles.exist()) throw new RuntimeException("cache files do not exist. ${EXT}")
        if (!cacheFiles.areFiles()) throw new RuntimeException("cache files are not regular files. ${EXT}")
        if (!cacheFiles.areReadable()) throw new RuntimeException("cache files are not readable. ${EXT}")

        _readFromCache(cacheFile)
    }


    MappedDataTableVineMethodCall _readFromCache(DataTableFiles cacheFiles) {
        assert cacheFiles != null
        assert cacheFiles.exist()
        assert cacheFiles.areReadable()
        MappedDataTableVineMethodCall.createFromFiles(cacheFiles)
    }



    MappedDataTableVineMethodCall _fetchAndCache() {
        MappedDataTable fetchResult = fetch(this.arguments)
        MappedDataTableVineMethodCall methodCall = _createCallObject(this.arguments, fetchResult)
        _writeCacheFile(methodCall)
        methodCall        
    }


    List<File> _writeCacheFile(MappedDataTableVineMethodCall methodCall) {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. no cache file will be written."
            return null
        }
        
        methodCall.writeFiles(cacheDir)
    }


    MappedDataTableVineMethodCall _createCallObject(Map arguments, MappedDataTable result) {
        assert arguments != null
        assert result != null

        MappedDataTableVineMethodCall mc = new MappedDataTableVineMethodCall()
        mc.vineMethodClass = this.class
        mc.arguments = arguments
        mc.result = result
        mc
    }


    MappedDataTable createMappedDataTable(String idFieldName) {
        String name = MappedDataTableVineMethodCall.computedName(this.class, this.arguments)
        new MappedDataTable(
            name:name,
            idFieldName:idFieldName,
            vine:[
                //vineClass:Vine.this.class.name,
                methodClass:this.class.name,
                arguments:this.arguments
            ]
        )
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
        MappedDataTableVineMethodCall.findFiles(cacheDir, this.class, this.arguments)
    }
    

    DataTableFiles cacheFiles() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is null. cacheFile will return null."
            return null
        }
        DataTableFiles.create(cacheDir, MappedDataTableVineMethodCall.computedName(this))
    }

}

