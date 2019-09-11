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
abstract class RelationalVineSqlServer extends RelationalVineAutoRecoverable {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/**
	 * Clean an input string so it can work in an SqlServer select statement
	 * as a value.
	 *
	 */
	static String cleanQueryString(String str) {
		str.replaceAll("'", "''")
			.replaceAll("\\[", "\\\\\\\\[")
			.replaceAll("\\]", "\\\\\\\\]")
	}


	///////////////////////////////////////////////////////////////////////////
	// INTERFACE IMPLEMENTATION
	///////////////////////////////////////////////////////////////////////////

	/** */
	Sql tryGetInstance(Map m) {
		log.info "RelationalVineSqlServer.tryGetInstance m:$m"
		def sql
		try {
			sql = Sql.newInstance(m)
		} catch (java.sql.SQLRecoverableException e) {
			elog.warn "withSql SQLRecoverableException: ${e.message}"
		}
		log.trace "RelationalVineSqlServer.tryGetInstance returning connection: $sql"
		sql
	}


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTOR
	///////////////////////////////////////////////////////////////////////////

	/** */
	public RelationalVineSqlServer(DatabaseConfig databaseConfig) {
        super(databaseConfig)
	}


}



