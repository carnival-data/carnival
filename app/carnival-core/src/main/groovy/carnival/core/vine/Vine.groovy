package carnival.core.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import groovy.sql.*

import carnival.core.config.DatabaseConfig



/**
 * Vine is the superclass of objects that interact read and write data to
 * data sources.
 *
 */
abstract class Vine {

    /** */
	static Logger log = LoggerFactory.getLogger('carnival')

    /** */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')
    


	/**
	 * Write the list of rows (as from a Groovy SQL query) to a CSV file.
	 * Note that it lower-cases the first row, which contains the field
	 * names.  This done only so that code that consumes the file can 
	 * assume lower case keys, which generally look better in a text
	 * editor.
	 *
	 */
    static File writeToCsvFile(List<GroovyRowResult> rows, String filename) {
        assert rows
        assert rows.size() > 0

        File file = new File(filename)
        PrintWriter pw = new PrintWriter(file);

        try {
            def keys = rows.first().keySet().toArray()
            pw.println keys.collect({ "${it.toLowerCase()}" }).join(",")

            rows.each { row ->
                def orderedValues = []
                keys.each { orderedValues << row[it] }
                orderedValues = orderedValues.collect { 
                    (it != null) ? "$it" : "" 
                }
                pw.println orderedValues.join(",")
            }
        } catch (Exception e) {
            log.error "writeToFile exception!!!"
            def epw = new PrintWriter(new File("${filename}-stacktrace.txt"))
            try {
                e.printStackTrace(epw)
            } finally {
                if (epw) epw.close()
            }
        } finally {
            if (pw) pw.close()
        }

        return file
    }



    /**
     * Generic read from CSV file.
     *
     */
    static List<Map> readFromCsvFile(String filename) {
        File df = new File(filename)
        return readFromCsvFile(df)
    }


    /**
     * Generic read from CSV file.
     *
     */
    static List<Map> readFromCsvFile(File file) {
        CsvIterator csvIterator = parseCsv(file.text)
        List<Map> data = []
        csvIterator.each { PropertyMapper csvVals ->
            data << csvVals.toMap()
        }
        return data
    }




	///////////////////////////////////////////////////////////////////////////
	// INSTANCE
	///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    public Vine() {
        // no-op
    }

}



