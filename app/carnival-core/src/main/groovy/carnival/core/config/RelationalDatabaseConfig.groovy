package carnival.core.config


import org.slf4j.Logger
import org.slf4j.LoggerFactory



/**
 * A database configuration.
 *
 */
class RelationalDatabaseConfig extends DatabaseConfig {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////
	static final Logger log = LoggerFactory.getLogger('carnival')

	
	static public RelationalDatabaseConfig getDatabaseConfigFromFile(String fileName, String prefix) {
        File dsConfigFile = new File(fileName)
        assert dsConfigFile.exists()
        Map dsConfigVals = loadYaml(dsConfigFile.text)
        def db = new RelationalDatabaseConfig(dsConfigVals['dataSources'][prefix])
		return db
	}

    /**
     *
     *
     */
    static public Map getDatabaseConfigVals(String confFilePath, String tag) {
        log.trace "CarnivalDbConfig.getDatabaseConfigVals"

        File dbConfFile = new File(confFilePath)
        assert dbConfFile.exists()
        String dbConfigText = dbConfFile.text
        log.trace "dbConfigText: $dbConfigText"

        Map dbm = loadYaml(dbConfigText)
        log.trace "dbm: ${dbm.dataSources}"

        return dbm.dataSources.get(tag)        
    }



	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////
	String url
	String user
	String password
	String driver


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////////////
	public RelationalDatabaseConfig(Map m) {
		["url", "user", "password", "driver"].each { f ->
			assert m[f]
		}
		url = m.url
		user = m.user
		password = m.password
		driver = m.driver
	}


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
	public Map getMap() {
		return [url: url, user: user, password: password, driver: driver]
	}
}





