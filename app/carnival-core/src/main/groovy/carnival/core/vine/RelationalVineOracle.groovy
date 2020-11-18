package carnival.core.vine


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.sql.*

import carnival.core.config.DatabaseConfig



/**
 * Vine is the superclass of objects that interact read and write data to
 * data sources.
 *
 */
abstract class RelationalVineOracle extends RelationalVineAutoRecoverable {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Clean an input string so it can work in an Oracle select statement
	 * as a value.
	 *
	 */
	static String cleanQueryString(String str) {
		str.replaceAll("'", "''")
			.replaceAll("\\[", "\\\\\\\\[")
			.replaceAll("\\]", "\\\\\\\\]")
			.replaceAll(/\n/, "")
			.replaceAll(/\r/, "")
	}


	///////////////////////////////////////////////////////////////////////////
	// INTERFACE IMPLEMENTATION
	///////////////////////////////////////////////////////////////////////////

	/** */
	Sql tryGetInstance(Map m) {
		log.trace "RelationalVineOracle.tryGetInstance"
		def sql
		try {
			sql = Sql.newInstance(m)
		} catch (java.sql.SQLRecoverableException e) {
			elog.warn "withSql SQLRecoverableException: ${e.message}"
			elog.warn "withSql url: ${m?.url}, user:${m?.user}"
		} catch (java.sql.SQLException e) {
			if (e.message.startsWith('ORA-02391') || e.message.startsWith('ORA-02396')) {
				elog.warn "withSql SQLException: ${e.message}"
			} else {
				throw e
			}
		}
		log.trace "RelationalVineOracle.tryGetInstance returning connection: $sql"
		sql
	}


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** */
	public RelationalVineOracle(DatabaseConfig databaseConfig) {
        super(databaseConfig)
	}

}



