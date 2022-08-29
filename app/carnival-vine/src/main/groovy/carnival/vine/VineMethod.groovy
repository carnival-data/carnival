package carnival.vine



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils




/**
 * VineMethod is an abstract class that defines the interface that must be
 * implemented by vine methods as well as providing some high level methods
 * that contribute to the operation of vine methods.  It is not expected that
 * client code would extend this class directly, but rather one of the more
 * concrete subclasses, such as JsonVineMethod, GenericDataTableVineMethod,
 * and MappedDataTableVineMethod.
 *
 */
@Slf4j
abstract class VineMethod {


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    abstract Object fetch(Map args)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    VineConfiguration vineConfiguration = VineConfiguration.defaultConfiguration()

    /** */
    Map arguments = [:]


    ///////////////////////////////////////////////////////////////////////////
    // CACHE MODE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    CacheMode cacheMode

    /** */
    public CacheMode getCacheMode() {
        if (cacheMode != null) return cacheMode
        assert vineConfiguration != null
        vineConfiguration.getCacheMode()
    }

    /** */
    public void setCacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode
    }

    VineMethod cacheMode(CacheMode cm) {
        assert cm != null
        setCacheMode(cm)
        this
    }

    VineMethod mode(CacheMode cm) {
        cacheMode(cm)
    }


    ///////////////////////////////////////////////////////////////////////////
    // CACHE DIRECTORY
    ///////////////////////////////////////////////////////////////////////////

    File _cacheDirectory() {
        assert vineConfiguration != null
        vineConfiguration.getCacheDirectory()
    }

    File _cacheDirectoryValidated() {
        File cacheDir = _cacheDirectory()
        if (cacheDir == null) {
            log.warn "cache directory is not configured."
            return null
        }
        if (!cacheDir.exists()) {
            log.warn "cache directory does not exist. ${cacheDir}"
            return null
        }
        if (!cacheDir.isDirectory()) {
            log.warn "cache directory is not a directory. ${cacheDir}"
            return null
        }
        return cacheDir
    }

    void _cacheDirectoryInitialize() {
        Path cachePath = Paths.get(vineConfiguration.cache.directory)
		if (cachePath == null) throw new RuntimeException("cachePath is null")
        
		def assertDirectoryAttributes = { Path dirPath ->
			String dirPathString = dirPath.toAbsolutePath().toString()
			if (!Files.exists(dirPath)) {
                throw new RuntimeException("${dirPathString} does not exist")
			}
            if (!Files.isDirectory(dirPath)) {
                throw new RuntimeException("${dirPathString} is not a directory")
            }
            if (!Files.isWritable(dirPath)) {
                throw new RuntimeException("${dirPathString} is not writable")
            }
            if (!Files.isReadable(dirPath)) {
                throw new RuntimeException("${dirPathString} is not readable")
            }
		}

        if (!Files.exists(cachePath) && vineConfiguration.cache.directoryCreateIfNotPresent) {
			log.trace "Files.createDirectories ${cachePath}"
			Files.createDirectories(cachePath)
		}
		
		assertDirectoryAttributes(cachePath)        
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS ARGUMENTS
    ///////////////////////////////////////////////////////////////////////////

    VineMethod arguments(Map args) {
        this.arguments = args
        this
    }

    VineMethod args(Map args) {
        arguments(args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    abstract VineMethodCall call()
    abstract VineMethodCall call(Map args)
    abstract VineMethodCall call(CacheMode cacheMode)
    abstract VineMethodCall call(CacheMode cacheMode, Map args)

}

