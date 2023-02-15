package carnival.vine

/**
 * Represent the three cache modes of Carnival vines.
 */
enum CacheMode {
    /** Cache files will be written, but not read.*/
    IGNORE,

    /**
     * If a cache file exists, it will be used. If no cache file, then the
     * method will be called and the cache file written.
     */
    OPTIONAL, 

    /**
     * Requires that a cache file be present. If no cache file is present, an
     * error is thrown.
     */
    REQUIRED
}