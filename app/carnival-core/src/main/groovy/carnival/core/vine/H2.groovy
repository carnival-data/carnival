package carnival.core.vine



import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager

import groovy.sql.Sql
import groovy.transform.Synchronized

import org.h2.tools.Server

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.util.Defaults



/**
 * Collection of static methods to provide H2 database functionality.
 *
 */
class H2 {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** used to create hash strings to uniquify file names */
	static MessageDigest MD5 = MessageDigest.getInstance("MD5")

	/** data inconsistency error log */
	static Logger elog = LoggerFactory.getLogger('db-entity-report')

	/** sql/cypher log */
	static Logger sqllog = LoggerFactory.getLogger('sql')

	/** console log */
	static Logger log = LoggerFactory.getLogger('carnival')

	/** strings that are considered to represent null values */
	static def NULL_VALUES = ['na', 'null']

	/** default oracle date format to use in queries */
	static def DEFAULT_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS'

	/** default oracle date format for converting strings to time-stamps */
	//static def DEFAULT_STRING_TO_TIMESTAMP_FORMAT = 'yyyy-MM-dd HH:mm:ss'
	static def DEFAULT_STRING_TO_TIMESTAMP_FORMAT = 'yyyy-MM-dd'

	/** The singleton server */
	static Server server

	/** hard coded user */
	static final String user = 'local'

	/** hard coded password */
	static final String pass = 'local'



	/** */
	static void quit() {
		if (server) server.stop()

	}


	/** */
	static boolean serverIsRunning() {
		return (server != null && server.isRunning(false))
	}


	/** */
	static void assertServerIsRunning() {
		if (!serverIsRunning()) {
			throw new RuntimeException('H2 server is not running')
		}
	}


	/** */
	static void withServer(Closure closure) {
		Server server = getServer()
		try {
			closure(server)
		} finally {
			if (server) server.stop()
		}
	}


	/** */
	@Synchronized
	static Server getServer() {
		log.trace "H2 getServer()"
		
		// if the server has been created, return it
		// otherwise, start a server and return it
		if (serverIsRunning()) return server
		else return startServer()

	}


	/** */
	@Synchronized
	static Server startServer() {
		log.trace "H2 startServer()"

        // create the TCP Server
        def baseDir = "${Defaults.dataDirectoryPath}/h2"
        log.trace "H2.getServer() baseDir:$baseDir"

        server = Server.createTcpServer(
            "-tcpPort", "9123",
            "-baseDir", baseDir
            //, "-tcpAllowOthers"
        )

        // start the TCP Server
        server.start()
        log.trace "h2 server status: ${server?.getStatus()}"

        return server
	}


	/** */
	static String getDbUrl() {
		assertServerIsRunning()
		"jdbc:h2:${server.getURL()}/carnival"
	}


	/** */
	static Connection getConnection() {
		assertServerIsRunning()
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.
            getConnection(getDbUrl(), user, pass);

        return conn
	}


	/** */
	static Map configMap() {
        [
            driver:'org.h2.Driver',
            url:getDbUrl(),
            user:user, 
            password:pass
        ]
	}


	/** */
	static Sql getSql() {
		log.trace "H2 getSql()"

		assertServerIsRunning()

        def db = configMap()
		log.trace "H2 db:$db"

        def sql = Sql.newInstance(db.url, db.user, db.password, db.driver)
        log.trace "H2 sql: $sql"

        return sql      
	}	
}



