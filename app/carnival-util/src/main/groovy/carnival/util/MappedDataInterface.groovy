package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper
import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.CSVReader

import groovy.sql.*
import groovy.transform.ToString
import groovy.transform.Synchronized



/**
 * MappedDataInterface is a bridge between CompoundFeature and DataTable.  The
 * purpose of this interface is to retro-fit legacy vine methods so they work
 * with both CompoundFeatures and DataTables.  It is expected that this 
 * interface will be deprecated after all legacy vine methods have been
 * migrated to the new VineMethod construction.
 *
 */
interface MappedDataInterface {

    ///////////////////////////////////////////////////////////////////////////
    // IDENTIFIERS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return true iff the given identifier exists in the data.
     *
     */
    public boolean containsIdentifier(String idVal)


    /**
     * Return a set of all distinct identifiers that exist in the data.
     *
     */
    public Set<String> allIdentifiers()


    ///////////////////////////////////////////////////////////////////////////
    // DATA ADD
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Matches DataTable method.
     *
     */
    public void dataAdd(java.sql.ResultSet row)


    /**
     * Matches DataTable method.
     *
     */
    public void dataAdd(PropertyMapper rec)


    /**
     * Matches DataTable method.
     *
     */
    public void dataAdd(GroovyRowResult row)


    /**
     * Matches DataTable method.
     *
     */
    public void dataAdd(Map<String,Object> vals)


    /**
     * dataAdd method that mimics an add method that exists in CompountFeature.
     * Will likely migrate away from this method.
     *
     */
    public void dataAdd(java.sql.ResultSet row, String idField, String fieldPrefix)


    ///////////////////////////////////////////////////////////////////////////
    // FILE I/O
    ///////////////////////////////////////////////////////////////////////////

    /**
     * writeToFile method that mimics the interface of the CompountFeature method.
     * Will likely migrate away from this method.
     *
     */
    public void writeToFile(Map args)

}



