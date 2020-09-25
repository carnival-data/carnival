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
import groovy.transform.WithReadLock
import groovy.transform.WithWriteLock

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer




/**
 * GenericDataTable is a semi-generalized structure storing key/value data
 * associated with a single entity that has a unique identifier.  
 *
 * The core data structure of GenericDataTable is a Map from a unique identifier
 * to a Map of key/value data.  The unique identifier is described by the
 * keyType field, which is of type KeyType.
 *
 * GenericDataTables can be written to and read from files.  A single
 * GenericDataTable is represented by two files, a .csv file for the data and a
 * .yaml file for the other bits of information including keyType. The
 * GenericDataTable object itself keeps track of which files to which it has
 * been written and from which it has been read.
 *
 */
@ToString(excludes=['data'], includeNames=true)
class GenericDataTable extends DataTable {


    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

	/** carnival logger */
    static Logger log = LoggerFactory.getLogger(GenericDataTable)

	/** error logger */
	static Logger elog = LoggerFactory.getLogger('db-entity-report') 



    ///////////////////////////////////////////////////////////////////////////
    // STATIC CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Wrapper class for GenericDataTable meta-data.
     *
     */
    static class MetaData extends DataTable.MetaData {

        Map meta = [:]

        public MetaData() {
        }

        public MetaData(Map args) {
            setMeta(args)
        }

        public MetaData(GenericDataTable mdt) {
            assert mdt
            setMeta (
                name: mdt.name,
                queryDate:mdt.queryDate,
                dataSourceDateOfUpdate:mdt.dataSourceDateOfUpdate,
                vine:mdt.vine,
            )
        }

        protected void setMeta(Map args) {
            log.trace "GenericDataTable.setMeta args:${args?.keySet()}"

            assert args
            assert args.name
            if (args.containsKey('queryDate') && args.queryDate != null) {
                assert ((args.queryDate instanceof String) || (args.queryDate instanceof Date)) 
                assert (args.queryDate)
            }

            if (args.containsKey('dataSourceDateOfUpdate') && args.dataSourceDateOfUpdate != null) {
                assert ((args.dataSourceDateOfUpdate instanceof String) || (args.dataSourceDateOfUpdate instanceof Date)) 
                assert (args.dataSourceDateOfUpdate)
            }

            this.meta = [:]
            this.meta.name = args.name

            this.meta.queryDate = computeDataSetDate(args, 'queryDate')
            this.meta.dataSourceDateOfUpdate = computeDataSourceDate(args, 'dataSourceDateOfUpdate')

            if (args.containsKey('vine')) {
                this.meta.vine = args.vine
            }
        }

        public String getName() {
            return meta.name
        }

        public Date getQueryDate() {
            return meta.queryDate
        }

        public Date getDataSourceDateOfUpdate() {
            return meta.dataSourceDateOfUpdate
        }

        public Map getVine() {
            return meta.vine
        }

    }


    /**
     * 
     *
     */
    static public GenericDataTable createFromCypher(Map args) {
        // deal with args
        assert args.graph
        def graph = args.graph
        assert args.cypherMatch
        def cypherMatch = args.cypherMatch
        assert args.cypherReturn
        def cypherReturn = args.cypherReturn

        def baseName = (args.name) ?: "cypher"
        def chunkLimit = (args.chunkLimit) ?: 0

        // get total records
        log.trace "$baseName - get the total number of records"

        def totalRecordsCypher = cypherMatch + '''
            RETURN COUNT(*) AS COUNT
        '''
        def totalRecords = graph.cypher(totalRecordsCypher).toList()[0]['COUNT'].toInteger()
        log.trace "$baseName - totalRecords: $totalRecords"

        // set up a generic data table
        log.trace "$baseName - get the records for $baseName"

        def now = DataTable.FILE_NAME_DATE_FORMAT.format(new java.util.Date())
        def dtArgs = [name:"${baseName}-${now}"]
        def dt = new GenericDataTable(dtArgs)

        // append SKIP and LIMIT clauses to cypher
        def cypher = cypherMatch + '\n' + cypherReturn +  '''
            SKIP $numToSkip
            LIMIT $chunkSize
        '''

        // set up chunk increments
        int chunkSize = 100000
        int logInterval = chunkSize.intdiv(10)
        int totalChunks = totalRecords.intdiv(chunkSize)+1
        int chunksCompleted = 0
        def results

        // get first chunk of results
        log.trace "cypher: $cypher"
        log.trace "chunkSize: $chunkSize"
        //log.debug "numToSkip: ${chunksCompleted++ * chunkSize}"
        log.trace "chunksCompleted: $chunksCompleted"
        results  = graph.cypher(
            cypher, 
            [
                chunkSize:chunkSize, 
                numToSkip:chunksCompleted++ * chunkSize
            ]
        ).toList()

        // while we have results, iterate through them
        while (results.size() > 0) {
            log.trace "$baseName - chunk: $chunksCompleted of $totalChunks"
            log.trace "$baseName - results: ${results.size()}"

            results.eachWithIndex { Map vals, int idx ->
                if (idx % logInterval == 0) log.info "$baseName - idx: $idx"

                dt.dataAdd(vals)
            }

            if (chunkLimit && chunksCompleted >= chunkLimit) break

            results  = graph.cypher(
                cypher, 
                [
                    chunkSize:chunkSize, 
                    numToSkip:chunksCompleted++ * chunkSize
                ]
            ).toList()
        }

        return dt  
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS - CREATE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Create a GenericDataTable from files in the given directory using the 
     * given base name.
     *
     * @param dir The directory in which to look for the GenericDataTable files.
     * @param name The name to use for the GenericDataTable and the base name
     * to use when searching for files.
     *
     */
    static public GenericDataTable createFromFiles(File dir, String name) {
        log.trace "GenericDataTable.createFromFiles dir:${dir?.canonicalPath} name:$name"

        assert dir
        assert dir.exists()
        assert dir.isDirectory()

        def metaFile = metaFile(dir, name)
        assert metaFile.exists()
        assert metaFile.length() > 0

        def dataFile = dataFile(dir, name)
        assert dataFile.exists()
        assert dataFile.length() > 0

        def yaml = new org.yaml.snakeyaml.Yaml(new DataTableConstructor())
        def meta = yaml.load(metaFile.text)
        assert meta.name
        assert meta.queryDate

        def mdt = new GenericDataTable(meta)

        def dataFileText = dataFile.text
        if (dataFileText) dataFileText = dataFileText.trim()
        log.trace "dataFileText: ${dataFileText.size()} characters"
        def numNewlines = dataFileText.findAll('\n').size()
        log.trace "numNewlines: ${numNewlines}"

        if (dataFileText) {
            log.trace "GenericDataTable.createFromFiles parseCsv"
            CsvIterator csvIterator = parseCsv(dataFileText)
            // TODO fix: this doesn't seem to work; hasNext() is returning true for a bad test file
            //log.trace "hasNext: ${csvIterator.hasNext()} "
            if (!csvIterator.hasNext()) {
                elog.warn "error in createFromFiles for file $dataFile. no data found."
                return
            }
            log.trace "GenericDataTable.createFromFiles add all data"
            mdt.dataAddAll(csvIterator)
        }

        return mdt
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** list of recs */
    List<Map<String,String>> data = new ArrayList<Map<String,String>>()

    /** date of the query **/
    Date queryDate

    /** vine info */
    Map vine

    /** if a query, the date the dataSource was last updated.  Default is null as set in metadata.setMeta **/
    Date dataSourceDateOfUpdate 

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /**
     * No-arg constructor.
     *
     */
    public GenericDataTable() {
        super()
        setKeySetComparator(
            new OrderBy(
                [
                    //{ !secondaryIdFieldMap.containsKey(it) }, 
                    { it }
                ]
            )
        )
    }

    /**
     * Make from MetaData.
     *
     */
    public GenericDataTable(MetaData args) {
        this()
        this.name = args.name
        this.queryDate = args.queryDate
        this.dataSourceDateOfUpdate = args.dataSourceDateOfUpdate
        if (args.vine) this.vine = args.vine
    }


    /**
     * A map constructor.
     *
     */
    public GenericDataTable(Map args) {
        this(new MetaData(args))
    }



    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Implementation of equals() that compares based on property values.
     *
     * @param obj The object to compare to this object.
     * @return True iff this object is equal to the provided object by field
     * comparison.
     *
     */
    public boolean equals(Object obj) {
        if (obj == null) return false
        if (!(obj instanceof MappedDataTable)) return false
        if (!areEqual(this.idFieldName, obj.idFieldName)) return false
        if (!areEqual(this.idKeyType, obj.idKeyType)) return false
        if (!areEqual(this.queryDate, obj.queryDate)) return false
        if (!areEqual(this.vine, obj.vine)) return false
        if (!areEqual(this.dataSourceDateOfUpdate, obj.dataSourceDateOfUpdate)) return false
        if (!Arrays.equals(this.data, obj.data)) return false
        return true
    }


    /**
     * Return a GenericDataTable.MetaData object for this GenericDataTable.
     *
     */
    public MetaData getMetaData() {
        new MetaData(
            name:name, 
            dataSourceDateOfUpdate:dataSourceDateOfUpdate,
            queryDate:queryDate)
    }


    /**
     * Return an iterator over all the data in this object.
     *
     * @return An iterator of records (maps) for all data in this object.
     *
     */
    @WithReadLock
    public Iterator<Map> dataIterator() {
        data.iterator()
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - ID KEYS COMPLETED
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Mark as completed the provided identifiers for the provided identifier
     * field.  Calls markIdKeysAsCompleted(String, Set<String>).
     *
     * @see markIdKeysAsCompleted(String, Set<String>)
     * @param idFieldName The name of the identifier field.
     * @param idKeys The identifier values that have been completed.
     *
     */
    public void markIdKeysAsCompleted(String idFieldName, List<String> idKeys) {
        assert idKeys != null
        assert idKeys.size() > 0
        markIdKeysAsCompleted(idFieldName, idKeys.toSet())
    }



    ///////////////////////////////////////////////////////////////////////////
    // METHODS - DATA ADD
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Add a single row of data from a java.sql.ResultSet.
     *
     * @param row The row of data
     *
     * @throws IllegalArgumentException if the row does not contain the a field
     *         named idKey.
     *
     */
    public void dataAdd(java.sql.ResultSet row) {
        def rowKeys = fieldNames(row)

        Map<String,Object> vals = new HashMap<String,Object>()
        rowKeys.each { k ->
            String keyVal = "${k}"
            
            //def v = row[k]
            def v = row.getObject(k)

            // hack: see dataAdd(GroovyRowResult row)
            if (v instanceof byte[] && v.length == 1) {
                vals[keyVal] = (char)v[0]
            } else {
                vals[keyVal] = v
            }
        }

        this.dataAdd(vals)
    }


    /**
     * Add the data from a single PropertyMapper (from CSVIterator.each).
     *
     * @param rec A PropertyMapper that must contain the idFieldName field.
     *
     */
    protected void dataAdd(PropertyMapper rec) {
        dataAdd(rec.toMap())
    }


    /**
     * Add a single row of data from a Groovy SQL query result.
     *
     * @param row The row of data
     *
     * @throws IllegalArgumentException if the row does not contain the a field
     *         named idFieldName.
     *
     */
    public void dataAdd(GroovyRowResult row) {
        Map<String,Object> vals = new HashMap<String,Object>()
        row.each { k,v ->
            String keyVal = toFieldName("${k}")
            this.keySet << keyVal

            // this is a hack to handle MySQL boolean fields that are binary(1)
            // they come back as byte arrays with a single value, either 48 or 49
            // for 0 and 1 respectively
            // the following code block maps [48,49] > [0,1]
            if (v instanceof byte[] && v.length == 1) {
                vals[keyVal] = (char)v[0]
            } else {
                vals[keyVal] = v
            }
        }

        dataAdd(vals)
    }


    /**
     * Add the given row of data.
     *
     * @param vals The map of key value pairs to add.
     *
     */
    @WithWriteLock
    public void dataAdd(Map<String,Object> vals) {
        //log.debug "dataAdd(Map<String,Object>): $vals"

        // create a new map for the new id
        def m = [:]

        // add each key value pair to this.data[id]
        vals.each { k, v ->
            // short cirtuit if v is MISSING
            if (String.valueOf(v) == MISSING) return

            // format the key as the field name
            def fn = toFieldName(k)
            
            // compute a clean value to add to this.data
            def cv = toCleanValue(fn, v)

            // the data cannot contain a reserved word
            def reservedWords = [DataTable.MISSING, DataTable.NULL]
            assert !(cv in reservedWords) : "Attempted to add value ${v.class} '$v', -> '$cv' which is one of the reserved words: $reservedWords"

            // if the value is one of the words that trigger warning, log the warning
            if (cv in DataTable.WARNINGS)  log.warn "WARNING: For field ${fn} adding value ${v.class} '$v', -> '$cv', which is one of the warning values"
   
            //println "id:$id k:$k v:$v fn:$fn idFieldName:${this.idFieldName} cv:$cv"

            // add the field -> clean data to this.data
            m.put(fn, cv)

            // add the field name to this.keySet 
            this.keySet << fn
        }

        if (m) this.data.add(m)
        else log.warn "no data to add in: $vals"
    }




    ///////////////////////////////////////////////////////////////////////////
    // FILE READ/WRITE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Write the meta-data file for this GenericDataTable in the given
     * directory.
     *
     * @return The meta-data file. 
     *
     */
    @WithReadLock
    public File writeMetaFile(File destDir, Map args = [:]) {
        assert destDir
        assert destDir.exists() : "${destDir.canonicalPath} does not exist"
        assert destDir.isDirectory()

        def fileName = fileName("${this.name}", ".yaml", args)
        def destFile = new File(destDir, fileName)
        PrintWriter pw = new PrintWriter(destFile)

        def md = [
            name:name,
            queryDate:queryDate,
            dataSourceDateOfUpdate:dataSourceDateOfUpdate,
            vine:vine
        ]

        try {
            def yaml = new org.yaml.snakeyaml.Yaml(new DataTableRepresenter())
            def metaAsYaml = yaml.dump(md)
            pw.println metaAsYaml
        } finally {
            if (pw) pw.close()
        }

        return destFile
    }

}



