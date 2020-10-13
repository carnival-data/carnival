package carnival.core.vine



import java.security.MessageDigest

import groovy.time.TimeCategory

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.core.config.DatabaseConfig
import carnival.core.config.RelationalDatabaseConfig
import carnival.util.SingleValueDateComparator
import carnival.util.FirstOrLastDateComparator
import carnival.util.SummaryDateComparator
import carnival.util.MappedDataTable
import carnival.util.GenericDataTable
import carnival.util.SqlUtils
import carnival.core.vine.Vine
import carnival.core.vine.VineMethod
import carnival.core.vine.MappedDataTableVineMethod
import carnival.core.vine.GenericDataTableVineMethod
import carnival.core.vine.CachingVine

//import org.ccil.cowan.tagsoup.*

import groovy.transform.InheritConstructors



/**
 * Vine to pull data from a variety of file formats
 *
 */
abstract class FileVine extends Vine {


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
	static Logger log = LoggerFactory.getLogger(FileVine)

	/** strings that are considered to represent null values */
	static def NULL_VALUES = ['na', 'null']

	/** default oracle date format to use in queries */
	static def DEFAULT_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS'

	/** default oracle date format for converting strings to time-stamps */
	//static def DEFAULT_STRING_TO_TIMESTAMP_FORMAT = 'yyyy-MM-dd HH:mm:ss'
	static def DEFAULT_STRING_TO_TIMESTAMP_FORMAT = 'yyyy-MM-dd'


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** */
	private File file

	/** */
	public void setFile(File file) {
		if (file == null) throw new RuntimeException("${this.class?.simpleName} setFile() file is null")
		if (!file.exists()) throw new RuntimeException("${this.class?.simpleName} setFile() file ${file} does not exist")
		this.file = file
	}

	/** */
	public File getFile() {
		if (this.file == null) throw new RuntimeException("${this.class?.simpleName} getFile() file is null")
		this.file
	}


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Defualt constructor.  Default to cache mode of ignore.
	 *
	 */
	public FileVine() { 
		//cacheMode = CacheMode.IGNORE
	}

	
	/** */
	public FileVine(String filename) {
		this()
		assert filename
		def file = new File(filename)
		assert file.exists()
		this.file = file
	}


	/** */
	public FileVine(File file) {
		super()
		assert file
		assert file.exists()
		this.file = file
	}



    /** */
    public String toString() {
        "${this.class.simpleName}-${file.name}"
    }


}