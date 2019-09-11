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
abstract class RelationalVinePostgres extends RelationalVineAutoRecoverable {

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
		log.info "RelationalVinePostgres.tryGetInstance m:$m"
		def sql
		try {
			sql = Sql.newInstance(m)
		} catch (java.sql.SQLRecoverableException e) {
			elog.warn "withSql SQLRecoverableException: ${e.message}"
		}
		log.trace "RelationalVinePostgres.tryGetInstance returning connection: $sql"
		sql
	}


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** */
	public RelationalVinePostgres(DatabaseConfig databaseConfig) {
        super(databaseConfig)
	}

}



