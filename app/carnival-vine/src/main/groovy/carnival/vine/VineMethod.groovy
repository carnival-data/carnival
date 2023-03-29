package carnival.vine



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.util.logging.Slf4j




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

    /**
     * Perform the fine method logic and return a result.
     * @param args A map of arguments
     * @return The result of the fetch.
     */
    abstract Object fetch(Map args)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** The configuration that the vine method will use */
    VineConfiguration vineConfiguration = VineConfiguration.defaultConfiguration()

    /** The arguments that will be passed to the call() methods */
    Map arguments = [:]


    ///////////////////////////////////////////////////////////////////////////
    // CACHE MODE
    ///////////////////////////////////////////////////////////////////////////

    /** The cache mode that will be used when this vine method is called. */
    CacheMode cacheMode

    /**
     * Returns the cache more of this vine method.
     * @return The CacheMode object.
     */
    public CacheMode getCacheMode() {
        if (cacheMode != null) return cacheMode
        assert vineConfiguration != null
        vineConfiguration.getCacheMode()
    }

    /** 
     * Set the cache mode of this vine method.
     * @param cacheMode The CacheMode object to use.
     */
    public void setCacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode
    }

    /**
     * Set the cache mode and return this vine method for method chaining.
     * @param cm The CacheMode object.
     * @return This vine method object.
     */
    VineMethod cacheMode(CacheMode cm) {
        assert cm != null
        setCacheMode(cm)
        this
    }

    /**
     * Synonym for cacheMode().
     * @see #cacheMode(CacheMode)
     */
    VineMethod mode(CacheMode cm) {
        cacheMode(cm)
    }


    ///////////////////////////////////////////////////////////////////////////
    // CACHE DIRECTORY
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the cache directory of this vine method as a file.
     * @return The File object
     */
    File _cacheDirectory() {
        assert vineConfiguration != null
        vineConfiguration.getCacheDirectory()
    }


    /**
     * Return the cache directory of this vine method if it is valid, otherwise
     * return null.
     * @return The File object or null
     */
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


    /**
     * Initialize the cache directory.
     */
    void _cacheDirectoryInitialize() {
        Path cachePath = vineConfiguration.cache.directory
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

    /** 
     * Set the arguments to be used when the vine method is called 
     * @param args The map of arguments.
     * @return This VineMethod
     */
    VineMethod arguments(Map args) {
        this.arguments = args
        this
    }

    /** 
     * Synonym for arguments().
     * @see #arguments(Map) 
     */
    VineMethod args(Map args) {
        arguments(args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS CALL
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Call the vine method logic without modification 
     * @return A VineMethodCall object
     */ 
    abstract VineMethodCall call()

    /**
     * Call the vine method logic using the provided arguments.
     * @param Map of arguments
     * @return A VineMethodCall object
     */
    abstract VineMethodCall call(Map args)

    /**
     * Call the vine method logic using the provided cache mode.
     * @param cacheMode The cache mode to use.
     * @return A VineMethodCall object
     */
    abstract VineMethodCall call(CacheMode cacheMode)

    /**
     * Call the vine method logic using the provided cache mode and arguments.
     * @param Map of arguments
     * @param cacheMode The cache mode to use.
     * @return A VineMethodCall object
     */
    abstract VineMethodCall call(CacheMode cacheMode, Map args)

}

