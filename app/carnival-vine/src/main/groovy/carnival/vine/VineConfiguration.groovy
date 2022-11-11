package carnival.vine



import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.util.logging.Slf4j
import groovy.transform.ToString



@Slf4j
@ToString(includeNames=true)
class VineConfiguration {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    final static String CACHE_PATH_DEFAULT = "carnival-home/vine/cache"

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

    @ToString(includeNames=true)
    static class Cache {
        CacheMode mode = CacheMode.IGNORE
        Path directory
        Boolean directoryCreateIfNotPresent = true
    }
    Cache cache = new Cache()


    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the cache mode configuration item as a CacheMode object.
     *
     */
    public CacheMode getCacheMode() {
        String cmStr = cache.mode
        assert cmStr != null

        CacheMode cm = Enum.valueOf(CacheMode, cmStr.trim().toUpperCase())
        return cm
    }


    /**
     * Return the cache directory configuration item as a File.
     *
     */
    public File getCacheDirectory() {
        assert cache.directory != null
        return cache.directory.toFile()
    }
}