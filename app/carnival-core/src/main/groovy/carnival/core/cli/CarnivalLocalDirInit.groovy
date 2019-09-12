package carnival.core.cli



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.util.Defaults



/**
 * Create the directory structure in $env.CARNIVAL_HOME
 *
 * Run:
 * gradle carnivalLocalDirInit
 */
class CarnivalLocalDirInit {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** error log */
	static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** log */
    static Logger log = LoggerFactory.getLogger('carnival')


    /**
     * Create the directory structure in $env.CARNIVAL_HOME
     *
     */
    static void main(String[] args) {
        Defaults.initDirectories()
    }
}
