package carnival.core.config


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml




/**
 * A database configuration.
 *
 */
abstract class DatabaseConfig {

	static final Logger log = LoggerFactory.getLogger('carnival')

	static public Map loadYaml(String text) {
        Yaml yaml = new Yaml();
        Map dbm = (Map) yaml.load(text);
		return dbm
	}

	static public Map loadYamlFile(File confFile) {
        log.trace "DatabaseConfig.loadYamlFile confFile: $confFile"

        assert confFile.exists()
        String confText = confFile.text
        //log.trace "confText: $confText"

        Map conf = loadYaml(confText)
        //log.trace "conf: $conf"

        return conf
	}

	static public Map loadYamlFile(String filePath) {
        log.trace "DatabaseConfig.loadYamlFile filePath: $filePath"

        File confFile = new File(filePath)
        return loadYamlFile(confFile)
	}

	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
	abstract public Map getMap() 

	public String toString() {
		return "${getMap()}"
	}
	
}





