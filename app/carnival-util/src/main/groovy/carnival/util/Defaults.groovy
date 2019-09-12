package carnival.util



import org.yaml.snakeyaml.Yaml
import org.slf4j.Logger
import org.slf4j.LoggerFactory



/**
 * Configuration utility -- to be re-written!!!!
 *
 */
public class Defaults {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')

    /** */
    static final Random random = new Random()


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

        // look for config in -DconfigDir
        if (!configFile && System.getProperty('carnival.home')) {
            configDirPath = System.getProperty('carnival.home') + "/config"
            configFile = findApplicationConfigurationFileInDirectoryPath(configDirPath)
        }
        
        // look for config in CARNIVAL_HOME/config
        if (!configFile && env.containsKey('CARNIVAL_HOME')) {
            configDirPath = env.get('CARNIVAL_HOME') + "/config"
            configFile = findApplicationConfigurationFileInDirectoryPath(configDirPath)
        }

        // look for config in ./config
        if (!configFile) {
            configFile = findApplicationConfigurationFileInDirectoryPath('config')
        }

        // if we don't have a config file by now, it's an error.
        if (!configFile) {
            def msg = "Could not find application config file. See documentation."
            msg += " carnival.home(sys prop): ${System.getProperty('carnival.home')}."
            if (env.containsKey('CARNIVAL_HOME')) msg += " CARNIVAL_HOME(env): ${env.get('CARNIVAL_HOME')}"
            else msg += " CARNIVAL_HOME(env) is not set."
            
            log.error msg
            throw new RuntimeException(msg)
        }

        log.trace "configFile: ${configFile}"

        return configFile
    }


    static private File findApplicationConfigurationFileInDirectoryPath(String dirPath) {
        if (dirPath == null) {
            throw new RuntimeException('dirPath is null')
            return null
        }
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
        if (!configFile) throw new RuntimeException('could not locate configuration file')
        return configFile.parentFile
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONFIGURATION LOADERS
    ///////////////////////////////////////////////////////////////////////////

    static public Map loadApplicationConfiguration() {
        def confFile = findApplicationConfigurationFile()

        // blow up if we have no configuration
        if (!confFile) throw new RuntimeException('could not locate configuration file')

        def conf = loadApplicationConfiguration(confFile)
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
    // CONVENIENCE GETTERS
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

    static private File getHomeDir() {
        def homeDir
        if (!homeDir) homeDir = findDirectoryFromSysProp('carnival.home')
        if (!homeDir) homeDir = findDirectoryFromEnv('CARNIVAL_HOME')
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

    static public String getTargetDirectoryPath() {
        getDirectoryConfigValue('directories.execution.target', 'target') 
    }

    static public File getTargetDirectory() {
        new File(getTargetDirectoryPath())
    }

    static public String getDataDirectoryPath() {
        getDirectoryConfigValue('directories.data.root', 'data') 
    }

    static public File getDataDirectory() {
    	new File(getDataDirectoryPath())
    }

    static public String getDataCacheDirectoryPath() {
        return getConfigValue('directories.data.cache') ?: "${dataDirectoryPath}/cache"
    }

    static public File getDataCacheDirectory() {
    	new File(getDataCacheDirectoryPath())
    }

    static public String getDataGraphDirectoryPath() {
        return getConfigValue('directories.data.graph.app') ?: "${dataDirectoryPath}/graph/app"
    }

    static public File getDataGraphDirectory() {
        new File(getDataGraphDirectoryPath())
    }

    static public String getDataGraphPublishBaseDirectoryPath() {
        return getConfigValue('directories.data.graph.publish.base') ?: "${dataDirectoryPath}/graph/publish/base"
    }

    static public File getDataGraphPublishBaseDirectory() {
        new File(getDataGraphPublishBaseDirectoryPath())
    }

    static public String getDataGraphPublishWorkspaceDirectoryPath() {
        return getConfigValue('directories.data.graph.publish.workspace') ?: "${dataDirectoryPath}/graph/publish/workspace"
    }

    static public File getDataGraphPublishWorkspaceDirectory() {
        new File(getDataGraphPublishWorkspaceDirectoryPath())
    }


    ///////////////////////////////////////////////////////////////////////////
    // INITIALIZERS
    ///////////////////////////////////////////////////////////////////////////

    static public void initDirectory(File dir) {
        assert dir != null

        log.trace "\n\nDefaults.initDirectory dir: $dir\n\n\n"

        if (!dir.exists()) {
            log.warn "${dir} does not exist. creating empty directory."
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
    }



    /*
    static private void setSystemProps(Map conf) {
        log.trace "directories: ${conf.get('directories')}"
        Map dconf = conf.get('directories').get('data')
        if (!dconf) throw new RuntimeException('directories.data not configured')

        ['root', 'graph', 'cache'].each {
            def propKey = "directory-data-${it}"
            if (dconf.get(it)) System.setProperty(propKey, dconf.get(it)) 
            log.debug "$it $propKey " + System.getProperty(propKey) 
        }

        Map econf = conf.get('directories').get('execution')
        if (!econf) throw new RuntimeException('directories.execution not configured')
        ['target'].each {
            def propKey = "directory-execution-${it}"
            if (econf.get(it)) System.setProperty(propKey, econf.get(it)) 
            log.debug "$it $propKey " + System.getProperty(propKey) 
        }
    }

    static public void setConfigDir(File configDir) {
        assert configDir
        assert configDir.exists()
        assert configDir.isDirectory()
        System.setProperty('directory-config', configDir.canonicalPath)
    }
    */






}





