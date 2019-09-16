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
import carnival.util.KeyType
import carnival.core.vine.Vine
import carnival.core.vine.VineMethod
import carnival.core.vine.MappedDataTableVineMethod
import carnival.core.vine.GenericDataTableVineMethod
import carnival.core.vine.CachingVine

import org.ccil.cowan.tagsoup.*
import groovy.util.XmlSlurper

import groovy.transform.InheritConstructors



/**
 * Vine to pull data from a variety of file formats
 *
 */
abstract class MultiFileVine extends Vine implements CachingVine {


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


	/**
	 * Utility method to convert a collection of file names to a list of files.
	 *
	 */
	static public List<File> toFiles(Collection<String> filenames) {
		assert filenames
		assert filenames.size() > 0
		List<File> files = new ArrayList<File>()
		filenames.each { 
			files << new File(it) 
		}
		return files
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

	List<File> files


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////////////

	/**
	 *
	 *
	 */
	public MultiFileVine(Collection<File> files) {
		super()
		assert files
		assert files.size() > 0
		files.each { assert it.exists() }
		this.files = files.toList()
	}



}