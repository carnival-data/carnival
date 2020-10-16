package carnival.core.config



/**
 * A database configuration.
 *
 */
class RedcapConfig extends DatabaseConfig {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** */
	static public RedcapConfig getDatabaseConfigFromFile(String confFilePath, String prefix) {
        def conf = loadYamlFile(confFilePath)
        if (!conf.get('carnival.dataSourcesRedcap')) throw new RuntimeException("no dataSourcesRedcap configurations in: $confFilePath")
        if (!conf.carnival.dataSourcesRedcap.get(prefix)) throw new RuntimeException("no configuration in $confFilePath for $prefix")
        def db = new RedcapConfig(conf.carnival.dataSourcesRedcap.get(prefix))
		return db
	}




	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////
	String url
	String user
	String apiToken
	String idField
	Boolean ignoreAllSSLValidation
	Boolean trustAllSSLCertificates


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////////////
	public RedcapConfig(Map m) {
		assert m
		["url", "user", "apiToken", "idField"].each { f ->
			assert m[f]
		}
		url = m.url
		user = m.user
		apiToken = m.apiToken
		idField = m.idField
		ignoreAllSSLValidation = false
		trustAllSSLCertificates = false
	}


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
	public Map getMap() {
		return [url: url, user: user, apiToken: apiToken, idField:idField, ignoreAllSSLValidation:ignoreAllSSLValidation, trustAllSSLCertificates:trustAllSSLCertificates]
	}
}