package carnival.util



import org.yaml.snakeyaml.Yaml
import groovy.util.logging.Slf4j



/**
 * Configuration utility -- to be re-written!!!!
 *
 */
@Slf4j
public class Defaults {


	///////////////////////////////////////////////////////////////////////////
	// STATIC FIELDS
	///////////////////////////////////////////////////////////////////////////

    /** */
    static final Random random = new Random()

    /** */
    final String[] requiredNeo4jConfigs = [
        'carnival.gremlin.neo4j.conf.dbms.security.auth_enabled',
        'carnival.gremlin.neo4j.conf.dbms.directories.plugins',
        'carnival.gremlin.neo4j.conf.dbms.security.procedures.unrestricted',
        'carnival.gremlin.neo4j.conf.dbms.security.procedures.whitelist',
        'carnival.gremlin.neo4j.conf.dbms.unmanaged_extension_classes'
    ]

    /** */
    static Map configData = null


    ///////////////////////////////////////////////////////////////////////////
    // CONFIGURATION FINDERS
    ///////////////////////////////////////////////////////////////////////////

    static public File findDirectoryFromEnv(String tag) {
        Map<String, String> env = System.getenv()
        if (!env.containsKey(tag)) {
            log.warn "$tag not set"
            return null
        }
        File f = new File(env.get(tag))
        if (f.exists() && f.isDirectory()) return f
        else return null
    }

    static public File findDirectoryFromSysProp(String tag) {
        def propVal = System.getProperty(tag)
        if (propVal == null) return null
        File f = new File(propVal)
        if (f.exists() && f.isDirectory()) return f
        else return null
    }

    static public File findApplicationConfigurationFile() {
        log.trace "findApplicationConfigurationFile()"

        // set-up
        Map<String, String> env = System.getenv()
        String configDirPath
        File configFile

        // if the system property carnival.home is set, then it is expected
        // that the directory exists and the configuration file is found there.
        // carnival.home is set automatically by the Carnival Gradle Plugin.
        // it is expected that applications that rely on Carnival as a library
        // will set this value.
        if (System.getProperty('carnival.home')) {
            configDirPath = System.getProperty('carnival.home') + "/config"
            configFile = findApplicationConfigurationFileInDirectoryPath(configDirPath)
            if (configFile != null && configFile.exists()) {
                log.info "config file from carnival.home: ${configFile}"
                return configFile
            } else {
                log.warn "configuration not found in config.home: ${configDirPath}"
                return null
            } 
        } else {
            log.info "carnival.home is not set"
        }

        // look for config in ./config
        configFile = findApplicationConfigurationFileInDirectoryPath('config')
        if (configFile != null && configFile.exists()) {
            log.info "config file from current directory: ${configFile}"
            return configFile
        }

        // if we have made it this far, it's a warning
        log.warn "could not find a configuration file in carnival.home, or ./config/"
        return null
    }


    static private File findApplicationConfigurationFileInDirectoryPath(String dirPath) {
        assert dirPath != null
        def dir = new File(dirPath)
        return findApplicationConfigurationFileInDirectory(dir)
    }


    static private File findApplicationConfigurationFileInDirectory(File dir) {
        if (dir == null) {
            log.warn "config dir is null"
            return null
        }
        if (!dir.exists()) {
            log.warn "config dir $dir does not exist"
            return null
        }
        if (!dir.isDirectory()) {
            log.warn "config dir $dir is not a directory"
            return null
        }

        def configFile
        ['application.yml', 'application.yaml'].each {
            if (!configFile) {
                configFile = new File(dir, it)
                if (!configFile.exists()) configFile = null
            }
        }

        return configFile
    }


    static public File findApplicationConfigurationDirectory() {
        def configFile = findApplicationConfigurationFile()
        if (!configFile) {
            log.warn 'findApplicationConfigurationDirectory - could not locate configuration file. returning null.'
            return null
        }
        return configFile.parentFile
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONFIGURATION LOADERS
    ///////////////////////////////////////////////////////////////////////////

    static public Map loadApplicationConfiguration() {
        if (Defaults.configData) return Defaults.configData

        def confFile = findApplicationConfigurationFile()

        // blow up if we have no configuration
        if (!confFile) {
            log.warn "loadApplicationConfiguration - could not locate configuration file. using default configuration."
            this.configData = [:]
            return this.configData
        }

        def conf = loadApplicationConfiguration(confFile)
        this.configData = conf
        return conf
    }

    static public Map loadApplicationConfiguration(File confFile) {
        assert confFile.exists()
        def confFileText = confFile.text
        Yaml yaml = new Yaml()
        Map conf = (Map) yaml.load(confFileText)
        return conf
    }

    /*
    static public Map loadConfigurationProperties(String filePath) {
        assert filePath
        File confFile = new File(filePath)
        return loadConfigurationProperties(confFile)
    }

    static public Map loadConfigurationProperties(File confFile) {
        assert confFile.exists()
        Properties conf = new Properties()
        confFile.withInputStream {
            conf.load(it)
        }

        // for legacy reasons, set the data directory System property
        setSystemProps(conf)
        
        return conf
    }
    */


    ///////////////////////////////////////////////////////////////////////////
    // CONFIGURATION SETTER
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static public void setConfigData(Map m) {
        assert m != null
        if (Defaults.configData == null) Defaults.loadApplicationConfiguration()
        //if (Defaults.configData == null) Defaults.configData = [:]
        m.entrySet().each { entry -> setConfigData(Defaults.configData, entry )}
        log.trace "setConfigData final Defaults.configData: ${Defaults.configData}"
    }


    /** */
    static public void setConfigData(Map dest, Map.Entry toSet) {
        log.trace "setConfigData dest:${dest} toSet:${toSet}"

        def toSetKey = toSet.getKey()
        def toSetVal = toSet.getValue()

        if (toSetVal instanceof Map) {

            if (dest.containsKey(toSetKey)) {
                
                def destVal = dest.get(toSetKey)
                if (destVal == null) {
                    dest.put(toSetKey, toSetVal)
                } else {
                    if (!(destVal instanceof Map)) throw new IllegalArgumentException("trying to override existing scalar value with a map: ${destVal} ${toSetKey} ${toSetVal}")
                    toSetVal.each { entry -> setConfigData(destVal, entry)}
                }

            } else {

                dest.put(toSetKey, toSetVal)

            }

        } else {

            if (dest.containsKey(toSetKey)) {

                def destVal = dest.get(toSetKey)
                if (destVal != null && destVal instanceof Map) throw new IllegalArgumentException("trying to replace a map with scalar value: ${destVal} ${toSetVal}")
                dest.put(toSetKey, String.valueOf(toSetVal))

            } else {

                dest.put(toSetKey, String.valueOf(toSetVal))

            }

        }

    }


    ///////////////////////////////////////////////////////////////////////////
    // GENERIC GETTERS
    ///////////////////////////////////////////////////////////////////////////

    static public String getConfigValue(String key) {
        Map config = loadApplicationConfiguration()

        if (key == null) throw new IllegalArgumentException('key is null')

        def path = key.tokenize('.')
        def val = config
        path.each { k -> val = val?.get(k) }

        if (val == null) return null
        if (!(val instanceof String)) throw new IllegalArgumentException("val is not a string $key $val")

        val = val.trim()
        if (val.length() == 0) return null

        log.trace "getConfigValue($key): $val"
        return val
    }

    static public String getConfigValue(String key, String defaultValue) {
        def val = getConfigValue(key)
        if (val == null) val = defaultValue

        log.trace "getConfigValue($key, $defaultValue): $val"
        return val
    }


    ///////////////////////////////////////////////////////////////////////////
    // HOME DIR
    ///////////////////////////////////////////////////////////////////////////

    static private File getHomeDir() {
        def homeDir
        if (!homeDir) homeDir = findDirectoryFromSysProp('carnival.home')
        log.trace "homeDir: $homeDir"
        return homeDir
    }

    static private String getDirectoryConfigValue(String key, String defaultRelativePath) {
        def homeDir = getHomeDir()

        def defaultPath = defaultRelativePath
        if (homeDir) defaultPath = "${homeDir}/${defaultRelativePath}"

        def val = getConfigValue(key, defaultPath)
        log.trace "getDirectoryConfigValue($key, $defaultRelativePath): $val"
        return val
    }


    ///////////////////////////////////////////////////////////////////////////
    // CARNIVAL CONFIG GETTERS
    ///////////////////////////////////////////////////////////////////////////

    static public String getTargetDirectoryPath() {
        getDirectoryConfigValue('carnival.directories.execution.target', 'target') 
    }

    static public File getTargetDirectory() {
        new File(getTargetDirectoryPath())
    }

    static public String getDataDirectoryPath() {
        getDirectoryConfigValue('carnival.directories.data.root', 'data') 
    }

    static public File getDataDirectory() {
    	new File(getDataDirectoryPath())
    }

    static public String getDataCacheDirectoryPath() {
        return getConfigValue('carnival.directories.data.cache') ?: "${dataDirectoryPath}/cache"
    }

    static public File getDataCacheDirectory() {
    	new File(getDataCacheDirectoryPath())
    }

    static public String getDataGraphDirectoryPath() {
        return getConfigValue('carnival.directories.data.graph.app') ?: "${dataDirectoryPath}/graph/app"
    }

    static public File getDataGraphDirectory() {
        new File(getDataGraphDirectoryPath())
    }

    static public String getDataGraphPublishBaseDirectoryPath() {
        return getConfigValue('carnival.directories.data.graph.publish.base') ?: "${dataDirectoryPath}/graph/publish/base"
    }

    static public File getDataGraphPublishBaseDirectory() {
        new File(getDataGraphPublishBaseDirectoryPath())
    }

    static public String getDataGraphPublishWorkspaceDirectoryPath() {
        return getConfigValue('carnival.directories.data.graph.publish.workspace') ?: "${dataDirectoryPath}/graph/publish/workspace"
    }

    static public File getDataGraphPublishWorkspaceDirectory() {
        new File(getDataGraphPublishWorkspaceDirectoryPath())
    }


    ///////////////////////////////////////////////////////////////////////////
    // INITIALIZERS
    ///////////////////////////////////////////////////////////////////////////

    static public void initDirectory(File dir) {
        assert dir != null

        log.info "Defaults.initDirectory dir: $dir"

        if (!dir.exists()) {
            log.info "${dir} does not exist. creating empty directory."
            boolean success = dir.mkdirs()
            if (!success) throw new RuntimeException("failed create directory ${dir}")
            return
        }

        if (!dir.isDirectory()) throw new RuntimeException("${dir} exists, but is not a directory")
    }


    static public void initDirectories() {
        initDirectory(getTargetDirectory())
        initDirectory(getDataCacheDirectory())
        initDirectory(getDataGraphDirectory())
        initDirectory(getDataGraphPublishBaseDirectory())
        initDirectory(getDataGraphPublishWorkspaceDirectory())

        def configFile = findApplicationConfigurationFile()
        if (configFile == null) {
            def configDirectory = new File(getHomeDir(), 'config')
            initDirectory(configDirectory)
        }
    }

}





