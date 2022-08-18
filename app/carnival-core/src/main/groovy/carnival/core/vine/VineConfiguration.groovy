package carnival.core.vine



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
        String cachePathString = cachePath.toAbsolutePath().toString()

        VineConfiguration config = new VineConfiguration()
        config.cache.directory = cachePathString

        return config
    }

    @ToString(includeNames=true)
    static class Cache {
        String mode = CacheMode.IGNORE.name()
        String directory
        Boolean directoryCreateIfNotPresent = true
    }
    Cache cache = new Cache()


    ///////////////////////////////////////////////////////////////////////////
    // INSTANCE
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
        String dirPathString = cache.directory
        assert dirPathString != null
        Path relativePath = Paths.get(dirPathString.trim())
        return relativePath.toFile()
    }
}