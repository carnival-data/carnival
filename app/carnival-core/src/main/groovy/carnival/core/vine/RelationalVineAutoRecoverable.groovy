package carnival.core.vine


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.sql.*

import carnival.core.config.DatabaseConfig



/** */
abstract class RelationalVineAutoRecoverable extends Vine implements RelationalVine {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** */
	static int WITH_INSTANCE_SLEEP_INTERVAL = 60000


	///////////////////////////////////////////////////////////////////////////
	// INTERFACE
	///////////////////////////////////////////////////////////////////////////

	/** */
	abstract public Sql tryGetInstance(Map m)


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** */
	DatabaseConfig databaseConfig


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

	/** */
	void tryCloseSql(Sql sql) {
		try {
			sql.close()
		} catch (Exception e) {
			elog.error("could not close sql", e)
		}
	}


	/** */
	public RelationalVineAutoRecoverable(DatabaseConfig databaseConfig) {
        super()
		this.databaseConfig = databaseConfig
	}


	/** */
	public Map getDbmap() {
		return databaseConfig.map
	}


	/** */
	public void withSql(Closure c) {
		def sql = tryGetInstance(getDbmap())

		while (!sql) {
			log.trace "withSql sleep $WITH_INSTANCE_SLEEP_INTERVAL"
			sleep WITH_INSTANCE_SLEEP_INTERVAL
			sql = tryGetInstance(getDbmap())
		}

		try {
			c(sql)
		} finally {
			tryCloseSql(sql)
		}
	}

}



