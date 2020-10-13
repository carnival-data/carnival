package carnival.core.vine



import java.security.MessageDigest

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.codec.digest.DigestUtils

import carnival.util.CsvUtil
import carnival.util.GenericDataTable
import carnival.core.vine.Vine
import carnival.core.vine.VineMethod
import carnival.core.vine.GenericDataTableVineMethod
import carnival.core.vine.CachingVine
import carnival.util.DataTable

import groovy.transform.InheritConstructors



/**
 * Generic vine to pull data from a CSV vile.
 *
 */
class CsvFileVine extends FileVine {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** data inconsistency error log */
	static Logger elog = LoggerFactory.getLogger('db-entity-report')

	/** sql/cypher log */
	static Logger sqllog = LoggerFactory.getLogger('sql')

	/** console log */
	static Logger log = LoggerFactory.getLogger(CsvFileVine)



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
	 * Use the no-arg constructor at your own risk.
	 *
	 */
	public CsvFileVine() { }

	
	/** 
	 * Create a vine for the file defined by the given file path.
	 *
	 */
	public CsvFileVine(String filename) {
		assert filename
		def file = new File(filename)
		assert file.exists()
		this.file = file
	}


	/** 
	 * Create a vine for the given file.
	 *
	 */
	public CsvFileVine(File file) {
		super()
		assert file
		assert file.exists()
		this.file = file
	}



    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return a String representation of this object.
     *
     */
    public String toString() {
        "${this.class.simpleName}-${file.name}"
    }


    /** */
    public GenericDataTable getAllRecords() {
        call('AllRecords')
    }



    ///////////////////////////////////////////////////////////////////////////
    // VINE METHODS
    ///////////////////////////////////////////////////////////////////////////

	/** */
	static class AllRecords implements GenericDataTableVineMethod {

        GenericDataTable.MetaData meta(Map args = [:]) {
        	if (args) throw new IllegalArgumentException("AllRecords does not accept any arguments")

            def hstr = "${getEnclosingVine().file.canonicalPath}"
            def inputHash = DigestUtils.md5(hstr.bytes).encodeHex().toString()

            new GenericDataTable.MetaData(
                name:"csv-file-${inputHash}"
            ) 
        }

		GenericDataTable fetch(Map args) {
			log.trace "CsvFileVine ${getEnclosingVine().file.canonicalPath} AllRecords.fetch()"

			def csvReader = CsvUtil.createReaderHeaderAware(getEnclosingVine().file.text)
			assert CsvUtil.hasNext(csvReader) : "no data: ${getEnclosingVine().file.canonicalPath}"

			def mdt = createEmptyDataTable(args)
			while (CsvUtil.hasNext(csvReader)) {
				def rm = csvReader.readMap()
        		mdt.dataAdd(rm)
			}

			return mdt
		}
	}


}