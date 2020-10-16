package carnival.core.vineold


import groovy.sql.Sql

import carnival.core.config.DatabaseConfig



/**
 * Vine is the superclass of objects that interact read and write data to
 * data sources.
 *
 */
abstract class RelationalVineGeneric extends Vine implements RelationalVine {

	///////////////////////////////////////////////////////////////////////////
	// INSTANCE
	///////////////////////////////////////////////////////////////////////////
	DatabaseConfig databaseConfig


	public RelationalVineGeneric(DatabaseConfig databaseConfig) {
        super()
		this.databaseConfig = databaseConfig
	}


	public Map getDbmap() {
		return databaseConfig.map
	}


	public void withSql(Closure c) {
		Sql.withInstance(getDbmap(), c)
	}

}



