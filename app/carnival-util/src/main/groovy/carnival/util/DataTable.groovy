package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.sql.GroovyRowResult
import groovy.transform.EqualsAndHashCode
import groovy.transform.Synchronized
import groovy.transform.WithReadLock
import groovy.transform.WithWriteLock

import com.opencsv.CSVReaderHeaderAware
import com.opencsv.CSVWriter

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.*
import org.yaml.snakeyaml.*
import org.yaml.snakeyaml.DumperOptions.FlowStyle
import org.yaml.snakeyaml.constructor.*



/**
 * DataTable is a generalized superclass that provides a basic framework for
 * tabular data structure that can be written and read from disk.
 *
 * The core data structure of DataTable is not specified and is left to
 * implementing sub-classes.
 *
 * DataTables can be written to and read from files.  A single DataTable is 
 * represented by two files, a .csv file for the data and a.yaml file for the 
 * other bits of information that describe the data. 
 *
 * The DataTable object provides a framework for keeping track of the files to 
 * which it has been written and from which it has been read.
 *
 */
abstract class DataTable {
    
    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

	/** carnival logger */
    static Logger log = LoggerFactory.getLogger(DataTable)


    ///////////////////////////////////////////////////////////////////////////
    // STATIC INNER CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * See ExtensionModule.
     *
     */
    static class FieldNameExtensions {

        /** */
        static FieldName fn(String str) {
            return FieldName.create(str)
        }

    }


    /**
     * Each DataTable class will habe an associated MetaData class.
     *
     */
    static abstract class MetaData {

        String name
        Boolean caseSensitive = false
        Date queryDate
        Date dataSourceDateOfUpdate
        Map vine

        protected void setFields(Map args) {
            assert args != null

            assert args.name
            this.name = args.name

            if (args.containsKey('caseSensitive')) {
                this.caseSensitive = args.get('caseSensitive')
            }

            if (args.get('queryDate') != null) {
                assert ((args.queryDate instanceof String) || (args.queryDate instanceof Date)) 
                assert (args.queryDate)
            }
            this.queryDate = computeDataSetDate(args, 'queryDate')

            if (args.get('dataSourceDateOfUpdate') != null) {
                assert ((args.dataSourceDateOfUpdate instanceof String) || (args.dataSourceDateOfUpdate instanceof Date)) 
                assert (args.dataSourceDateOfUpdate)
            }
            this.dataSourceDateOfUpdate = computeDataSourceDate(args, 'dataSourceDateOfUpdate')

            if (args.get('vine') != null) this.vine = args.vine
        }
        
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * a date format precise to the day for use in representing query dates
     */
    static protected SimpleDateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-M-d")

    /** 
     * a date format with no special characters that should work as part of a
     * file name
     */
    static protected SimpleDateFormat FILE_NAME_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss")

    /** canonical string representing missing data */
    static public final String MISSING = 'MDT_MISSING'

    /** canonical string representing null data */
    static public final String NULL = 'MDT_NULL'

    /** strings representing values that should trigger warnings */
    static final Collection<String> WARNINGS = ['null', 'NULL']



    ///////////////////////////////////////////////////////////////////////////
    // STATIC METODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return a string representation of the current time formatted as per
     * FILE_NAME_DATE_FORMAT.
     * 
     */
    static public String currentTimeFormatted() {
        DataTable.FILE_NAME_DATE_FORMAT.format(new java.util.Date())
    }
    

    /**
     * Convert the given date to a string representation formatted as per
     * QUERY_DATE_FORMAT.
     *
     * @param d A Date object
     * @return A string representation of the date.
     * 
     */
    static public String dataSetDateToString(Date d) {
        if (d == null) return null
        return QUERY_DATE_FORMAT.format(d)
    }


    /**
     * Compute the data set date given a map of args and the key that should
     * contain the data set date in args.  If args does not contain the key,
     * return a date based on the current date.
     *
     * @param args The map of args
     * @param key The key within args whose value should contain the data
     *            set date.
     * 
     */
    static public Date computeDataSetDate(Map args, String key) {
        if (!args.containsKey(key) || args.get(key) == null) {
            return removeTimeFromDate(new Date())
        }

        def val = args.get(key)
        return dataDate(val)
    }


    /**
     * Compute the data source date given a map of args and the key that should
     * contain the data source date.  If args does not contain the key of
     * contains a null value for the key, return null.  Otherwise, return
     * the value from args.
     *
     * @param args The map of args
     * @param key The key within args whose value should contain the data
     *            source date.
     * 
     */
    static public Date computeDataSourceDate(Map args, String key) {
        if (!args.containsKey(key) || args.get(key) == null) return null

        def val = args.get(key)
        return dataDate(val)
    }


    /**
     * Given a Date object, return a new Date object that appropriately
     * represents the "data" date, which means stripping out the time component
     * of the Date.
     *
     * @param val A Date object.
     * @return A Data object that represents the "data" date of the input.
     * 
     */
    static protected Date dataDate(Date val) {
        return removeTimeFromDate(val)
    }


    /**
     * Given a string, return a Date object that represents the "data" date,
     * which means parsing the input date to a Date, then removing the time
     * component of the Date.
     *
     * @param val A string in QUERY_DATE_FORMAT.
     * @return A Date object that prepresents the "data" date of the input.
     * 
     */
    static protected Date dataDate(String val) {
        if (val == null) return null

        String dstr = val.trim()
        Date parsedDate
        try {
            parsedDate = QUERY_DATE_FORMAT.parse(dstr)
        } catch (java.text.ParseException e) {
            log.error "could not parse date: '${dstr}'", e
            throw e
        }

        return removeTimeFromDate(parsedDate)
    }


    /**
     * Given a Date object, remove the time components of the Date, which
     * means setting hour, minute, second, and millisecond to zero.
     *
     * @param date A Date object.
     * @return A new Date object based in the input with the time components
     *         set to zero.
     * 
     */
    static public Date removeTimeFromDate(Date date) {
 
        if (date == null) return null

        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.getTime()
    }


    /**
     * Generic read from CSV file.
     *
     * @param filename The full path to the input CSV file.
     * @return A list of maps containing the CSV data.
     *
     */
    static public List<Map> readDataFromCsvFile(String filename) {
        CsvUtil.readFromCsvFile(filename)
    }


    /**
     * Generic read from CSV file.
     *
     * @param file The CSV File.
     * @return A list of maps containing the CSV data.
     *
     */
    static public List<Map> readDataFromCsvFile(File file) {
        CsvUtil.readFromCsvFile(file)
    }


    /**
     * Find the files required to build a DataTable of the given name in
     * the given directory.
     *
     * @param dir The directory in which to look.
     * @param name The base name of the DataTable.
     *
     * @return A map containing entries for the meta-data and data files iff
     * both files exist.  If either does not exist, an empty map is returned.
     *
     */
    static public Map findFiles(File dir, String name) {
        def meta = findMetaFile(dir, name)
        def data = findDataFile(dir, name)
        if (meta && data) return [meta:meta, data:data]
        else return [:]
    }


    /**
     * Find the files required to build a DataTable of the given name in
     * the given directory.
     *
     * @param dir The directory in which to look.
     * @param name The base name of the DataTable.
     *
     * @return A DataTableFiles object containing the meta and data files if
     * both files exist.  If either does not exist, null is returned.
     *
     */
    static public DataTableFiles findDataTableFiles(File dir, String name) {
        def meta = findMetaFile(dir, name)
        def data = findDataFile(dir, name)
        if (meta && data) return new DataTableFiles(meta:meta, data:data)
        else return null
    }
    

    /**
     * Find the meta-data file for a DataTable of the given name in the
     * given directory.
     *
     * @param dir The directory in which to look.
     * @param name The base name of the DataTable.
     *
     * @return A File object for the meta-data file if it exists, null
     * otherwise.
     *
     */
    static public File findMetaFile(File dir, String name) {
        assert name
        assert dir
        assert dir.exists()
        assert dir.isDirectory()

        def metaFile = metaFile(dir, name)
        return metaFile.exists() ? metaFile : null
    }


    /**
     * Find the data file for a DataTable of the given name in the given
     * directory.
     *
     * @param dir The directory in which to look.
     * @param name The base name of the DataTable.
     *
     * @return A File object for the data file if it exists, null
     * otherwise.
     *
     */
    static public File findDataFile(File dir, String name) {
        assert name
        assert dir
        assert dir.exists()
        assert dir.isDirectory()

        def dataFile = dataFile(dir, name)
        return dataFile.exists() ? dataFile : null
    }


    /**
     * Return a map of the files required to build a DataTable of the
     * given name in the given directory, whether or not the files exist.
     *
     * @param dir The directory where the files are to reside.
     * @param name The name of the data table.
     *
     */
    static public Map files(File dir, String name) {
        def meta = metaFile(dir, name)
        def data = dataFile(dir, name)
        return [meta:meta, data:data]
    }


    /**
     * Return a File object reference for the meta-data file of a
     * DataTable of the given name in the given directory, whether or not
     * the file exists.
     *
     * @param dir The directory in which to get or create the meta file.
     * @param name The name prefix of the meta file.
     * @return The meta file.
     *
     */
    static public File metaFile(File dir, String name) {
        assert name
        assert dir

        def metaFileName = "${name}.yaml"
        return new File(dir, metaFileName)
    }


    /**
     * Return a File object reference for the data file of a DataTable of
     * the given name in the given directory, whether or not the file exists.
     *
     * @param dir The directory in which to get or greate the data file.
     * @param name The name prefix of the data file.
     * @return The data file.
     *
     */
    static public File dataFile(File dir, String name) {
        assert name
        assert dir

        def dataFileName = "${name}.csv"
        return new File(dir, dataFileName)
    }


    /**
     * Get the field names from a java.sql.ResultSet.
     *
     * @param row A java.sql.ResultSet
     * @return The field namesin the ResultSet as a list of Strings.
     *
     */
    static public List<String> fieldNames(java.sql.ResultSet row) {
        def metaData = row.getMetaData()
        def numCols = metaData.getColumnCount()
        List<String> kl = []
        for (int i=1; i<=numCols; i++) {
            // use columnLabel instead of columnName to honor field aliasing.  For example, for (select uuid as packet_uuid):
            //     columnName = 'uuid', columnLabel = 'packet_uuid', the ResultSet row key is 'packet_uuid'
            kl << metaData.getColumnLabel(i)
        }
        return kl
    }


    /**
     * Load a meta file and return it as a map.
     *
     * @param metaFile The meta file.
     * @return The data of the meta file as a map.
     *
     */
    static public Map loadMetaFileData(File metaFile) {
        def yaml = new org.yaml.snakeyaml.Yaml(new DataTableConstructor())
        def meta = yaml.load(metaFile.text)
        assert meta.name
        assert meta.queryDate
        return meta
    }


    /** 
     * Given a file name, apply modifications to it as per the args.
     *
     * @param fileName The input file name as a String.
     * @param args The args that specifify which modifications to apply.
     * @param args.appendDateSuffix If true, append the current date.
     * @param args.fileNameSuffix Append the value of fileNameSuffix.
     * @return The modified file name.
     *
     */
    static public String applyFilenameModifications(String fileName, Map args) {
        assert fileName != null
        assert args != null

        if (args.appendDateSuffix) {
            def now = FILE_NAME_DATE_FORMAT.format(new java.util.Date())
            fileName += "-${now}"
        }
        if (args.fileNameSuffix) fileName += args.fileNameSuffix

        return fileName
    }


    /** 
     * Given a file name base, extension, and args, which will be passed to
     * applyFilenameModifications, return a file name.
     *
     * @param base File name base.
     * @param ext File name extension.
     * @param args Args for applyFilenameModifications.
     * @return The fully constructed file name.
     *
     */
    static public String fileName(String base, String ext, Map args) {
        assert base != null
        assert ext != null
        assert args != null

        def fileName = base
        fileName = applyFilenameModifications(fileName, args)
        if (!ext.startsWith('.')) ext += '.'
        fileName += ext

        return fileName
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS - FIELD NAME
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Given a string, return the field name compliant version of that string,
     * which currently is just trimmed and upper cased.
     *
     * @param val The val to be converted to a field name.
     * @return The input as a field name.
     *
     */
    static String fieldName(String val, Map args = [:]) {
        //log.debug "fieldName $val $args"
        assert val != null
        String out = val.trim()
        if (!(args.get('caseSensitive'))) out = out.toUpperCase()
        if (out.length() < 1) throw new IllegalArgumentException("maps to empty field name. val:$val")
        //log.debug "out: |$out|"
        return out
    }


    /**
     * Get the key of the given map that corresponds to the field name of this
     * DataTable.
     *
     * @param qFieldName The field name to look for.
     * @param vals A map in whose keySet to find the field name.
     * @return The id field key found in the given map or null if none are found.
     * @throws RuntimeException if more than one match is found.
     *
     */
    static String findFieldName(String qFieldName, Map<String,String> vals, Map args = [:]) {
        assert vals != null
        DataTable.findFieldName(qFieldName, vals.keySet(), args)
    }


    /**
     * Get the value of the given set that corresponds to the field name of this
     * DataTable.
     *
     * @param @qFieldName The field name to look for.
     * @param vals A set of Strings in which to look for the field name.
     * @return The id field key found in the given set or null if none are found.
     * @throws RuntimeException if more than one match is found.
     *
     */
    static String findFieldName(String qFieldName, Set<String> vals, Map args = [:]) {
        def matches = vals.findAll { DataTable.fieldName(it, args) == DataTable.fieldName(qFieldName, args) }
        if (matches.size() > 1) throw new RuntimeException("multiple matches for $qFieldName found in vals: $vals")
        if (matches.size() == 1) return matches.first()
        return null
    }


    /**
     * Find dupliace field names among a list of field names.  Fields names a 
     * and b are considered duplicates if toFieldName(a) == toFieldName(b).
     *
     * @param names The set of strings in which to look for duplicates.
     * @return A list of lists that contain the duplicates found.
     *
     */
    /*static List<List<String>> findDuplicateFieldNames(Set<String> names, Map args = [:]) {
        if (names == null) return []
        if (names.size() < 2) return []

        def elements = names.toList()
        def out = []
        elements.each { el ->
            names.removeElement(el)
            def fnEl = toFieldName(el, args)
            names.each { name ->
                def fnName = toFieldName(name, args)
                if (fnName == fnEl) out << [name, el]
            }
        }

        return out
    }    */


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS - ID VALUES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Given a string, return the id key compliant version of that string,
     * trimmed and to lower case.
     *
     * If the string is empty or null, return null.
     *
     * @param val The val to be formatted.
     * @return The formatted value.
     *
     */
    static String formatIdValue(String val, Map args = [:]) {
        if (val == null) return null
        if (val.trim().length() == 0) return val.trim()
        return DataTable.toIdValue(val, args)
    }


    /**
     * Convenience method to format a BigDecimal to an id value.
     *
     * @param val The BigDecimal to be formatted.
     * @return The BigDecimal formatted as an ID value String.
     *
     */
    static String formatIdValue(java.math.BigDecimal val, Map args = [:]) {
        assert val != null
        return DataTable.formatIdValue(String.valueOf(val), args)
    }


    /**
     * Given a string, return the id key compliant version of that string,
     * trimmed and to lower case.  This is a more strict version of formatIdValue
     * that never returns a null value.
     *
     * @param val The input value.
     * @param args.caseSensitive If true, then the case of the ID value is not changed.
     * @throws Assertion That val != null
     * @throws IllegalArgumentException If the input is all white space.
     * @return The input formatted as an ID value, non-null.
     *
     */
    static String toIdValue(String val, Map args = [:]) {
        assert val != null
        def out = val.trim()
        if (!args.get('caseSensitive')) out = out.toLowerCase()
        if (out.length() < 1) throw new IllegalArgumentException("maps to empty id value. val:$val")
        return out
    }


    /**
     * Convenience method to convert a BigDecimal to an id value by first
     * converting it to a String and then passing it to toIdValue(String)
     *
     * @param val A BigDecimal.
     * @return The BigDecimal formatted as an ID value.
     *
     */
    static String toIdValue(java.math.BigDecimal val, Map args = [:]) {
        assert val != null
        return DataTable.toIdValue(String.valueOf(val), args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS - GENERAL UTILITY
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return a String representation of v suitable to be added to the
     * data as a value.
     *
     * @param fn A property formatted field name, ie from toFieldName()
     * @param v A value of any type.
     * @return A propertly formatted String representation of v.
     *
     */
    static protected String toCleanValue(String fn, Object v) {
        // compute a clean value to add to this.data
        def cv

        // if v is null, then explicitly write as null
        if (v == null) {
            cv = null
        }

        // special cased types
        else if (v instanceof Date) {
            cv = SqlUtils.timestampAsString(v) 
        }

        // handle as string
        else {
            def str = String.valueOf(v)
            switch (str) {
                case NULL: cv = null; break;
                default: cv = str
            }
        }        
    }


    /** 
     * Returns true iff:
     *     a and be are both null
     *     a.equals(b)
     *
     * @param a An Object.
     * @param b An Object.
     * @return True if the two objects are "equal". 
     *
     */
    static boolean areEqual(Object a, Object b) {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return a.equals(b)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** name of this data table */
    String name

    /** files to which these data have been written */
    List<File> writtenTo = new ArrayList<File>()

    /** if this object was created by reading from a file, the file */
    File readFrom

    /** sorted set of keys (columns) */
    Set<String> keySet = new TreeSet<String>()

    /** case sensitivity */
    boolean caseSensitive = false

    /** date of the query */
    Date queryDate

    /** vine info */
    Map vine = [:]

    /** if a query, the date the dataSource was last updated.  Default is null as set in metadata.setMeta */
    Date dataSourceDateOfUpdate 


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /**
     * No argument constructor that provides a default date-based name, which
     * hopefully will be unique enough during any production run and cover
     * cases where a name is not provided.
     *
     */
    public DataTable() {
        this.name = FILE_NAME_DATE_FORMAT.format(new java.util.Date())
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - CASE SENSITIVE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Map stringHandlingArgs() {
        [caseSensitive:this.caseSensitive]
    }


    /** */
    String toFieldName(String val) {
        DataTable.fieldName(val, stringHandlingArgs())
    }

    

    ///////////////////////////////////////////////////////////////////////////
    // METHODS KEY SET
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return true iff the key set of this data table contains the input string
     * formatted as a field name.
     *
     * @param k The field name to search for.
     * @return True iff the field name is found.
     *
     */
    public boolean keySetContains(String k) {
        this.keySet.contains(toFieldName(k))
    }


    /**
     * Set the comparator for keySet.
     *
     * This method has a side efect of replacing the existing keySet with a
     * TreeSet.
     *
     * @param comp A comparator to use when comparing keys (fiel names) for 
     * ordering, etc.
     *
     */
    public void setKeySetComparator(Comparator<String> comp) {
        // set up a new key set with all the existing content
        Set<String> ks = new TreeSet<String>(comp)
        ks.addAll(this.keySet)

        // replace keyset with a linkedhashset
        this.keySet = ks
    }


    /**
     * Set the comparator for keySet to an OrderBy comprised of the given
     * list of closures.  Note that the delegate of the closures is set to
     * 'this' with a delegate strategy of DELEGATE_FIRST.
     *
     * This method has a side efect of replacing the existing keySet with a
     * TreeSet.
     *
     * @param cs a list of closures that will be used to create an OrderBy
     * object, which will be used as the new key set comparator.
     *
     */
    public void setKeySetComparator(List<Closure> cs) {
        cs.each { 
            it.delegate = this
            it.resolveStrategy = Closure.DELEGATE_FIRST 
        }
        this.setKeySetComparator(new OrderBy(cs))
    }


    /** 
     * Convenience method to order keys by alphabetical order.
     *
     */
    public void setOrderedKeysAlphabetical() {
        setKeySetComparator([{it}])
    }


    /** */
    public void setOrderKeysByInsertion() {
        def existingKeys = this.keySet
        this.keySet = new LinkedHashSet<String>()
        this.keySet.addAll(existingKeys)
    }


    /**
     * Set the keySet to be a LinkedHashSet with the keys provided in the
     * given order.  If this data table contains keys that are not provided
     * in the input, they will be appended to the end of the list.
     *
     * This method has a side effect of replacing the existing keySet with a
     * LinkedHashSet.
     *
     * @param keys A list of keys (field names), which will be used to order
     * the keys (field names) of this data table.
     *
     */
    public void setOrderedKeys(List<String> keys = []) {
        //assert keys : "no keys provided: $keys"

        def existingKeys = this.keySet
        this.keySet = new LinkedHashSet<String>()
        addFieldNamesToKeySet(keys)

        existingKeys.removeAll(this.keySet)
        addFieldNamesToKeySet(existingKeys.toList())
    }


    /**
     * Convenience method to setOrderedKeys by variable arguments.
     *
     * @see setOrderedKeys(List<String>)
     * @param keys Keys (field names) as individual ordered arguments.
     *
     */
    public void setOrderedKeys(String... keys) {
        setOrderedKeys(keys.toList())
    }


    /** */
    public void setOrderedKeysBooleanCriteria(List<Closure> booleanClosures) {
        List<String> keys = this.keySet.toList()
        List<String> oks = new ArrayList<String>()
        booleanClosures.each { bcl ->
            List<String> chunk = keys.findAll(bcl)
            oks.addAll(chunk)
            keys.removeAll(chunk)
        }
        setOrderedKeys(oks)
    }


    /** */
    public void setOrderedKeysBooleanCriteria(Closure... booleanClosures) {
        setOrderedKeysBooleanCriteria(booleanClosures.toList())
    }


    /**
     * Add/append ordered keys to the existing keySet via addFieldNamesToKeySet().
     *
     * This method has a side effect of replacing the existing keySet with a
     * LinkedHashSet.
     *
     * @see addFieldNamesToKeySet()
     * @param fieldNameStrs The list of keys (field names).
     *
     */
    public void addOrderedKeys(List<String> fieldNameStrs) {
        if (fieldNameStrs == null) throw new IllegalArgumentException("fieldNameStrs is null")

        // set up a new key set with all the existing content
        Set<String> ks = new LinkedHashSet<String>()
        ks.addAll(this.keySet)

        // replace keyset with a linkedhashset
        this.keySet = ks

        addFieldNamesToKeySet(fieldNameStrs)
    }


    /**
     * Add the provided keys (field names) to the key set of this data table.
     *
     * @param fieldNameStrs The list of keys (field names).
     * @throws Assertion If the provided list of field names are not unique
     * case insensitive.
     *
     */
    protected addFieldNamesToKeySet(List<String> fieldNameStrs) {
        // cast field name strings to proper field names
        def fns = fieldNameStrs.collect { toFieldName(it) }
        fns = fns.unique()
        assert fns.size() == fieldNameStrs.size() : "field names must be unique case insensitive"
        this.keySet.addAll(fns)
    }


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT METHODS - DATA READ/WRITE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return an iterator for each 'row' of data.
     *
     * @return An iterator that covers all data in row (map) form.
     *
     */
    abstract public Iterator<Map> dataIterator()


    /**
     * Add the provided individual record.
     *
     * @param row The record wrapped in a java.sql.ResultSet, such as is the
     * case with the iterator groovy.Sql.eachRow() { row -> ... }
     *
     */
    abstract public void dataAdd(java.sql.ResultSet row)


    /**
     * Add the provided individual record.
     *
     * @param row The record wrapped in a GroovyRowResult.
     *
     */
    abstract public void dataAdd(GroovyRowResult row)


    /**
     * Add the provided individual record.
     *
     * @param vals The record as a Map.
     *
     */
    abstract public void dataAdd(Map<String,Object> vals)


    /**
     * Write the meta-data file in the given directory.
     *
     * @param destDir the directory in which to write the file.
     *
     */
    abstract public File writeMetaFile(File destDir, Map args)



    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILE I/O
    ///////////////////////////////////////////////////////////////////////////



    /**
     * Write the data file for this DataTable object in the given
     * directory.  Calls writeDataToCsvFile().
     *
     * @param destDir The directory in which to write the file.
     * @param args Optional map of arguments massed to helper methods.
     * @return The data file.
     * @see writeDataToCsvFile()
     *
     */
    @WithReadLock
    public File writeDataFile(File destDir, Map args = [:]) {
        assert destDir
        assert destDir.exists()
        assert destDir.isDirectory()

        def fileName = fileName("${this.name}", ".csv", args)

        def destFile = new File(destDir, fileName)
        return writeDataToCsvFile(destFile, args)
    }


    /**
     * Helper method to write the data to a specific file.
     *
     * @param destFile The File where the CSV data will be writen.
     * @param args.missingVal The String to use for missing data.
     * @param args.nullVal The String to use for null data.
     * @return The data file.
     *
     */
    @WithReadLock
    protected File writeDataToCsvFile(File destFile, Map args = [:]) {
        assert destFile

        String missingVal = args.containsKey("missingVal") ? args.missingVal : MISSING
        String nullVal = args.containsKey("nullVal") ? args.nullVal : NULL

        CSVWriter writer

        try {
            writer = new CSVWriter(new FileWriter(destFile));

            String[] line
            List<String> lineList

            // headers
            List keys = this.keySet.toList()
            if (args.keys) {
                List filteredKeys = []
                keys.each { k -> if (args.keys.contains(k)) filteredKeys.add(k)}
                //keys.retainAll(args.keys)
                keys = filteredKeys
            }
            line = keys.toArray()
            writer.writeNext(line)

            // rows          
            dataIterator().each { Map vals ->
                lineList = []
                
                keys.each { k ->
                    def v = null
                    if (vals.containsKey(k)) {
                        def rv = vals.get(k)
                        v = (rv == null) ? nullVal : String.valueOf(rv)
                    } else {
                        v = missingVal
                    }
                    //def v = vals.get(k)
                    //lineList << (v) ? v : ""
                    lineList << v
                }

                line = lineList.toArray()
                writer.writeNext(line)
            }
        } finally {
            try {
                if (writer) writer.close()    
            } catch (Throwable st) {
                log.error "DataTable.writeDataToCsvFile failed to close writer destFile: $destFile"
            }
            
        }

        return destFile
    }



    ///////////////////////////////////////////////////////////////////////////
    // METHODS - DATA ADD ALL
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Add all data from a list of lists.
     *
     * @param dataToAdd The data to add as a list of lists where the first list
     * entry is a list of field names and the remaining lists are the data.
     *
     */
    public void dataAddAllListList(List<List<String>> dataToAdd) {
        if (dataToAdd == null) throw new IllegalArgumentException("fieldNameStrs is null")
        if (dataToAdd.size() < 2) throw new IllegalArgumentException("dataToAdd must be of size >= 2") 

        // assume first row has field names
        def fieldNameStrs = dataToAdd[0]

        // cast field name strings to proper field names
        def fns = fieldNameStrs.collect { toFieldName(it) }
        fns = fns.unique()
        assert fns.size() == fieldNameStrs.size() : "field names must be unique case insensitive"
        keySet.addAll(fns)
        def numFields = fns.size()

        // add the data from the remaining rows
        (1..dataToAdd.size()-1).each { i ->
            def vals = dataToAdd[i]
            assert vals.size() == numFields : "different number of vals than field names: $numFields $vals"
            
            def valMap = [:]
            vals.eachWithIndex { val, j ->
                valMap.put(fns[j], val)
            }

            dataAdd(valMap)
        }
    }


    /**
     * Add all provided data.
     *
     * @param maps Data in a collection of maps.
     *
     */
    public void dataAddAllListOfMaps(Collection<Map> maps) {
        assert maps
        maps.each { dataAdd(it) }
    }


    /**
     * Add all provided data.
     *
     * @param dataToAdd Data in a CSVIterator.
     *
     */
    public void dataAddAll(CSVReaderHeaderAware csvReader) {
        assert csvReader != null

        def firstRec = true
        while (CsvUtil.hasNext(csvReader)) {
            def rec = csvReader.readMap()
            if (firstRec) {
                def fieldNames = rec.keySet().collect { toFieldName(it) }
                this.keySet.addAll(fieldNames)
                firstRec = false
            }
            dataAdd(rec) 
        }
    }


    /**
     * Add all provided data.
     *
     * @param dataToAdd A collection of GroovyRowResult objects, such as is
     * returned from groovy.Sql.rows().
     *
     */
    public void dataAddAllGroovyRowResults(Collection<GroovyRowResult> rows, Boolean emptyOk = false) {
        if (!emptyOk) assert rows
        rows.each { dataAdd(it) }
    }    



    ///////////////////////////////////////////////////////////////////////////
    // FILE READ/WRITE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Find the files that represent this object in the given directory.  See
     * the static findFiles() for further details.
     *
     * @param dir The directory in which to look.
     *
     */
    public Map findFiles(File dir) {
        return findFiles(dir, name)
    }


    /**
     * Find the meta-data file for this object in the given directory.  See the
     * static findMetaData() for further details.
     *
     * @param dir The directory in which to look.
     *
     */
    public File findMetaFile(File dir) {
        return findMetaFile(dir, name)
    }


    /**
     * Find the data file for this object in the given directory.  See the
     * static findDataFile() for further details.
     *
     * @param dir The directory in which to look.
     *
     */
    public File findDataFile(File dir) {
        return findDataFile(dir, name)
    }


    /**
     * Write this DataTable to disk.  Two files are written, the .csv
     * data file and the .yaml meta-data file.
     *
     * @param args - possible keys: [missingVal, nullVal]
     *
     * @return A map with two entries.  Map.metaFile contains the meta-data
     * file and Map.dataFile the data file.
     *
     */
    
    public List<File> writeFiles(File destDir, Map args = [:]) {
        log.trace "DataTable.writeFiles: ${this.name} ${destDir?.canonicalPath}"
        writeDataTableFiles(destDir, args).toList()
    }


    @WithReadLock
    public DataTableFiles writeDataTableFiles(File destDir, Map args = [:]) {
        assert destDir != null
        assert destDir.exists()
        assert destDir.canWrite()
        assert destDir.isDirectory()

        File meta = writeMetaFile(destDir, args)
        File data = writeDataFile(destDir, args)

        DataTableFiles dtf = new DataTableFiles(meta:meta, data:data)
        writtenTo.addAll(dtf.toList())
        dtf
    }


}



