package carnival.core.vine



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils

import carnival.core.config.Defaults
import carnival.core.util.CoreUtil




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
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    static final String DEFAULT_CACHE_MODE_CONFIG_KEY = 'carnival.cache-mode'


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    static public CacheMode defaultCacheMode() {
        String str = Defaults.getConfigValue(DEFAULT_CACHE_MODE_CONFIG_KEY)
        if (str == null) return CacheMode.IGNORE
        CacheMode cm = Enum.valueOf(CacheMode, str)
        if (cm) return cm
        CacheMode.IGNORE
    }


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    abstract Object fetch(Map args)


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

    VineMethod cacheMode(CacheMode cm) {
        assert cm != null
        this.cacheMode = cm
        this
    }

    VineMethod mode(CacheMode cm) {
        cacheMode(cm)
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

