package carnival.vine



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.util.logging.Slf4j
import groovy.transform.ToString



/**
 * Contains the configuration elements for a vine.
 *
 */
@Slf4j
@ToString(includeNames=true)
class VineConfiguration {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** the default cache directory path */
    final static String CACHE_PATH_DEFAULT = "carnival-home/vine/cache"

    /**
     * Returns a default configuration.
     * @return VineConfiguration default object
     */
    static public VineConfiguration defaultConfiguration() {
        Path currentRelativePath = Paths.get("");
        Path cachePath = currentRelativePath.resolve(CACHE_PATH_DEFAULT)

        VineConfiguration config = new VineConfiguration()
        config.cache.directory = cachePath

        return config
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONFIG VALUES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Class to contain configuration elements of a vine cache.
     *
     */
    @ToString(includeNames=true)
    static class Cache {

        /** the cache mode */
        CacheMode mode = CacheMode.IGNORE

        /** the cache directory */
        Path directory

        /** 
         * if set to true, the cache directory will be created if it is not
         * present when needed
        */
        Boolean directoryCreateIfNotPresent = true
    }

    /** the cache configuration object */
    Cache cache = new Cache()


    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the cache mode configuration item as a CacheMode object.
     * @return The CacheMode of the configuration.
     */
    public CacheMode getCacheMode() {
        String cmStr = cache.mode
        assert cmStr != null

        CacheMode cm = Enum.valueOf(CacheMode, cmStr.trim().toUpperCase())
        return cm
    }


    /**
     * Return the cache directory configuration item as a File.
     * @return File The cache directory as a File object.
     *
     */
    public File getCacheDirectory() {
        assert cache.directory != null
        return cache.directory.toFile()
    }
}