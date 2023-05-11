package carnival.util


import java.text.DateFormat
import java.text.SimpleDateFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
 * MappedDataTable is a semi-generalized structure storing key/value data
 * associated with a single entity that has a unique identifier.  
 *
 * The core data structure of MappedDataTable is a Map from a unique identifier
 * to a Map of key/value data
 *
 * MappedDataTables can be written to and read from files.  A single
 * MappedDataTable is represented by two files, a .csv file for the data and a
 * .yaml file for the other bits of information. The
 * MappedDataTable object itself keeps track of which files to which it has
 * been written and from which it has been read.
 *
 */
@ToString(excludes=['data'], includeNames=true)
class MappedDataTable extends DataTable {


    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

	/** carnival logger */
    static Logger log = LoggerFactory.getLogger(GenericDataTable)



    ///////////////////////////////////////////////////////////////////////////
    // STATIC CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Wrapper clas for MappedDataTable meta-data.
     *
     */
    @ToString
    static class MetaData extends DataTable.MetaData {

        /** The name of the id field name */
        String idFieldName

        /** The date format */
        DateFormat dateFormat

        /** Pattern of the date format */
        String dateFormatPattern

        /**
         * Constructor from map of args.
         * @param args A map of args
         */
        public MetaData(Map args) {
            setFields(args)
        }

        /**
         * Constructor from a mapped data table.
         * @param mdt Source mapped data table
         */
        public MetaData(MappedDataTable mdt) {
            assert mdt
            setFields (
                name: mdt.name,
                caseSensitive: mdt.caseSensitive,
                queryDate : mdt.queryDate,
                dataSourceDateOfUpdate : mdt.dataSourceDateOfUpdate,
                idFieldName: mdt.idFieldName,
                vine:mdt.vine,
                dateFormat:mdt.dateFormat,
                dateFormatPattern:mdt.dateFormatPattern
            )
        }

        /**
         * Set the fields of this object from the provided vals.
         * @param args Map of provided values.
         */
        protected void setFields(Map args) {
            super.setFields(args)

            assert args.idFieldName
            this.idFieldName = DataTable.fieldName(args.idFieldName)
            
            if (args.containsKey('caseSensitive')) {
                this.idFieldName = DataTable.fieldName(
                    args.idFieldName, 
                    [
                        caseSensitive:args.get('caseSensitive')
                    ]
                )
            }

            if (args.get('dateFormat') != null) {
                this.dateFormat = args.dateFormat
                this.dateFormatPattern = dateFormat.toPattern()
            }

            if (args.get('dateFormatPattern') != null) {
                this.dateFormatPattern = args.dateFormatPattern
                this.dateFormat = new SimpleDateFormat(args.dateFormatPattern)
            }
        }

    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS - CREATE
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Create a MappedDataTable from files in the given directory using the 
     * given base name.
     *
     * @param dir The directory in which to look for the MappedDataTable files.
     * @param name The name to use for the MappedDataTable and the base name
     * to use when searching for files.
     *
     */
    static public MappedDataTable createFromFiles(File dir, String name) {
        log.trace "createFromFiles dir:${dir?.canonicalPath} name:$name"

        // get the metadata from file
        def meta = loadMetaDataFromFile(dir, name)
        assert meta.idFieldName

        // construct a mapped data table object from 
        def mdt = new MappedDataTable(meta)

        // load the file data
        loadDataFromFile(dir, name, mdt)

        return mdt
    }



    /**
     * Create a mapped data table from cache files.
     * @param cacheFiles The cache files
     * @return A mapped data table
     */
    static public MappedDataTable createFromFiles(DataTableFiles cacheFiles) {
        def meta = loadMetaDataFromFile(cacheFiles.meta)
        def mdt = new MappedDataTable(meta)
        loadDataFromFile(cacheFiles.data, mdt)
        mdt
    }




    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the name of the id field */
    String idFieldName

    /** id -> map of vals */
    SortedMap<String,Map<String,String>> data = new TreeMap<String, Map<String,String>>()

    /** the formatter to use with date values */
    SimpleDateFormat dateFormat = SqlUtils.DEFAULT_TIMESTAMP_FORMATER


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /**
     * No-arg constructor.
     *
     */
    public MappedDataTable() {
        super()
        setOrderedKeysMappedDataTableDefault()
    }


    /**
     * Make from MetaData.
     *
     */
    public MappedDataTable(MetaData args) {
        this()

        this.name = args.name
        this.idFieldName = args.idFieldName
        this.queryDate = args.queryDate
        this.dataSourceDateOfUpdate = args.dataSourceDateOfUpdate

        if (args.vine) this.vine = args.vine

        if (args.dateFormat) this.dateFormat = args.dateFormat
        
        if (args.dateFormatPattern) {
            this.dateFormat = new SimpleDateFormat(args.dateFormatPattern)
        }

        if (args.caseSensitive) this.caseSensitive = args.caseSensitive
    }


    /**
     * A map constructor.
     *
     */
    public MappedDataTable(Map args) {
        this(new MetaData(args))
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Retore the key (field) ordering to the default, which orders keys:
     *   id field
     *   secondary id fields
     *   all other fields
     *
     */
    public void setOrderedKeysMappedDataTableDefault() {
        setKeySetComparator(
            new OrderBy(
                [
                    { it != idFieldName },
                    { it }
                ]
            )
        )
    }


    /**
     * Return a MappedDataTable.MetaData object for this MappedDataTable.
     *
     */
    public MetaData getMetaData() {
        def m = [
            vine:vine,
            name:name, 
            queryDate:queryDate,
            idFieldName:idFieldName
        ]
        if (dataSourceDateOfUpdate != null) m.dataSourceDateOfUpdate = dataSourceDateOfUpdate
        
        new MetaData(m)
    }


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
        if (!areEqual(this.queryDate, obj.queryDate)) return false
        if (!areEqual(this.vine, obj.vine)) return false
        if (!areEqual(this.dataSourceDateOfUpdate, obj.dataSourceDateOfUpdate)) return false
        if (!areEqual(this.data, obj.data)) return false
        return true
    }




    ///////////////////////////////////////////////////////////////////////////
    // METHODS - CASE SENSITIVE METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Convert the provided value to an id value.
     * @param val The string value
     * @return The string value as an id value. 
     */
    String toIdValue(String val) {
        DataTable.toIdValue(val, stringHandlingArgs())
    }


    /** 
     * Convenience method to call DataTable.findFieldName using the default
     * string handling args.
     * @see DataTable#findFieldName(String, Map, Map)
     * @see DataTable#stringHandlingArgs()
     */
    String findFieldName(String qFieldName, Map<String,String> vals) {
        DataTable.findFieldName(qFieldName, vals, stringHandlingArgs())
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - DATA GET
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Implementation of MappedDataInterface method.
     *
     */
    public Set<String> allIdentifiers() {
        return this.data.keySet()
    }


    /**
     * Implementation of MappedDataInterface method.
     *
     */
    public boolean containsIdentifier(String idVal) {
        //log.debug "containsIdentifier idVal: $idVal"
        return this.data.containsKey(
            toIdValue(idVal)
        )
    } 


    /**
     * Return an iterator over all the data in this object.
     *
     * @return An iterator of records (maps) for all data in this object.
     *
     */
    public Iterator<Map> dataIterator() {
        data.values().iterator()
    }


    /**
     * Convenience method that converts the given String to an id value and
     * returns the data for that id value.
     *
     * @param idVal The record identifier.
     * @param args.verbose Set to false to turn off verbose logging.
     * @return The record of data for the provided identifier.
     *
     */
    public Map dataGet(String idVal, Map args = [:]) {
        assert idVal != null
        def rec = data.get(
            toIdValue(idVal)
        )
        if (rec == null) {
            if (!args.containsKey('verbose') || args.verbose) log.warn "no data for key $idVal"
            //throw new IllegalArgumentException("no data for key $idVal")
        }
        return rec
    }


    /**
     * Get a single value given an identifier and field name.
     *
     * @param idVal The record identifier.
     * @param fieldName The name of the field for which to get the value.
     * @param args.verbose Verbose logging; default is [verbose:false].
     * @return The value of field fieldName for the record identified by idVal
     * as a String if it exists, otherwise null.
     * 
     *
     */
    public String dataGet(String idVal, String fieldName, Map args = [verbose:false]) {
        assert idVal != null
        assert fieldName != null
        def fn = toFieldName(fieldName)
        def rec = dataGet(idVal, args)
        return (rec != null) ? rec.get(fn) : null
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - DATA UTILITY
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Convenience method to call trimColumns() and trimRows().
     *
     */
    public void trim() {
        trimColumns()
        trimRows()
    }


    /**
     * Remove columns (fields) that have no data associated with them.
     *
     */
    @WithWriteLock
    public void trimColumns() {
        Map<String,Boolean> keysHaveData = new HashMap<String,Boolean>()
        keySet.each { keysHaveData.put(toFieldName(it), false) }
        dataIterator().each { rec ->
            if (!keysHaveData.find({ k, v -> !v})) return
            rec.each { k, v ->
                if (v == null) return
                keysHaveData.put(toFieldName(k), true)
            }
        }
        keysHaveData.each { k, v ->
            if (v) return
            keySet.remove(k)
        }
    }


    /**
     * Remove rows that have no data associated with them besides the ID value.
     *
     */
    @WithWriteLock
    public void trimRows() {
        Set<String> toRemove = new HashSet<String>()
        this.data.each { k, v ->
            if (v == null || v.size() <= 1) toRemove << k
        }
        toRemove.each { k ->
            this.data.remove(k)
        }
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
        def vals = toMap(row)
        this.dataAdd(vals)
    }


    /**
     * Convert a java.sql.ResultSet to a Map<String,Object>.
     *
     * @param row A row of data as a ResultSet.
     * @return The data as a map.
     *
     */
    protected Map<String,Object> toMap(java.sql.ResultSet row) {
        def rowKeys = fieldNames(row)

        Map<String,Object> vals = new HashMap<String,Object>()
        rowKeys.each { k ->
            String keyVal = "${k}"
            
            def v = row.getObject(k)

            // hack: see dataAdd(GroovyRowResult row)
            if (v instanceof byte[] && v.length == 1) {
                vals[keyVal] = (char)v[0]
            } else {
                vals[keyVal] = v
            }
        }

        return vals
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
            String fn = toFieldName("${k}")
            this.keySet << fn

            // this is a hack to handle MySQL boolean fields that are binary(1)
            // they come back as byte arrays with a single value, either 48 or 49
            // for 0 and 1 respectively
            // the following code block maps [48,49] > [0,1]
            if (v instanceof byte[] && v.length == 1) {
                vals[fn] = (char)v[0]
            } else {
                vals[fn] = v
            }
        }

        dataAdd(vals)
    }


    /**
     * Special dataAdd method that takes an optional map of modifications that 
     * will be performed on the vals map before passing them to the lower level
     * dataAdd(Map) method.
     *
     * @param @vals A record of data in Map form.
     * @param modifications.dataFieldPrefix If provided, prepend the provided
     * value to all field names. 
     *
     */
    public void dataAddWithModifications(Map<String,Object> vals, Map modifications = [:]) {
        Map<String,Object> valsModified = new HashMap<String,Object>()
        valsModified.putAll(vals)

        String dataFieldPrefix = modifications.dataFieldPrefix
        if (dataFieldPrefix) {
            Map<String,Object> vals2 = new HashMap<String,Object>()
            valsModified.each { k, v ->
                def nk = (toFieldName(k) == this.idFieldName) ? k : "${dataFieldPrefix}${k}"
                vals2.put(nk, v)
            }
            valsModified = vals2
        }

        dataAdd(valsModified)
    }



    /**
     * Add the given key value pairs to the data for the given identifier. If
     * there is already data for the given identifier, ensure that the provided
     * data would not overwrite existing data.
     *
     * @param vals The map of key value pairs to add.  A key value pair for the
     *        this.idFieldName must exist.
     *
     * @throws IllegalArgumentException if the given data would overwrite
     *         existing data.
     *
     */
    @WithWriteLock
    public void dataAdd(Map<String,Object> vals) {
        //log.debug "dataAdd vals: $vals"

        def idKey = findFieldName(this.idFieldName, vals)
        if (!idKey) throw new IllegalArgumentException("id not found idFieldName:${idFieldName} vals:${vals}")

        def id = toIdValue(vals.get(idKey))
        if (dataGet(id, [verbose:false])) throw new IllegalArgumentException("data for id $id already exists")

        //log.debug "dataAdd id: $id"

        if (id == null) throw new IllegalArgumentException("id cannot be null vals:$vals")

        // create a new map for the new id
        this.data[id] = [:]

        // add each key value pair to this.data[id]
        vals.each { k, v ->
            // short cirtuit if v is MISSING
            if (String.valueOf(v) == MISSING) return

            // format the key as the field name
            def fn = toFieldName(k)
            
            // compute a clean value to add to this.data
            def cv

            // special case the id field
            if (fn.equals(this.idFieldName)) {
                cv = toIdValue(v)
            }

            // if v is null, then explicitly write as null
            else if (v == null) {
                cv = null
            }

            // special cased types
            else if (v instanceof Date) {
                //cv = SqlUtils.timestampAsString(v) 
                cv = dateFormat.format(v)
            }

            // handle as string
            else {
                def str = String.valueOf(v)
                switch (str) {
                    case NULL: cv = null; break;
                    default: cv = str
                }
            }

            def reservedWords = [DataTable.MISSING, DataTable.NULL]
            assert !(cv in reservedWords) : "Attempted to add value ${v.class} '$v', -> '$cv' which is one of the reserved words: $reservedWords"
            if (cv in DataTable.WARNINGS)  log.warn "WARNING: For key ${id}.${fn} adding value ${v.class} '$v', -> '$cv', which is one of the warning values"
   
            // add the field -> clean data to this.data
            this.data[id].put(fn, cv)

            // add the field name to this.keySet 
            this.keySet << fn
        }
    }


    /**
     * Add a single key value pair for a given id.
     *
     * @param id The id to use as the key.
     * @param key The data key.
     * @param value The data value.
     *
     */
    @WithWriteLock
    public void dataAppend(String id, String key, String value) {
        //log.debug "dataAppend id:$id key:$key value:$value"
        assert id != null
        assert key != null
        assert value != null

        def idv = toIdValue(String.valueOf(id))
        def formattedKey = toFieldName(key)

        Map<String,String> m = this.data[idv]
        if (!m) {
            this.data[idv] = new HashMap<String,String>()
            m = this.data[idv]
            m.put(this.idFieldName, idv)
            this.keySet.add(this.idFieldName)
        }
        m.put(formattedKey, value)
        this.keySet.add(formattedKey)
    }


    /**
     * Add the given key value pairs to the data for the given identifier. If
     * there is already data for the given identifier, ensure that the provided
     * data would not overwrite existing data.
     *
     * @param id The id to use as the key
     * @param vals The map of key value pairs to set as the data for the given id.
     *
     * @throws IllegalArgumentException if the given data would overwrite
     *         existing data.
     *
     */
    @WithWriteLock
    public void dataAppend(String id, Map<String,String> vals) {
        //log.debug "dataAppend id:$id vals:$vals"
        def idv = toIdValue(String.valueOf(id))
        def existingData = this.data[idv]

        def formattedVals = vals.collectEntries{ k, v -> 
            [(toFieldName(k)): v]
        }

        // check that the data in vals are disjoint by key from the existing
        // data.  if not, throw an error.
        if (existingData && !existingData.keySet().disjoint(formattedVals.keySet())) {
            def commonKeys = existingData.keySet().intersect(formattedVals.keySet())
            commonKeys.each { k ->
                if (existingData.get(k) != formattedVals.get(k)) throw new IllegalArgumentException("existingData $k:${existingData.get(k)} != formattedVals $k:${formattedVals.get(k)}")
            }
        }

        // make sure that vals does not contain the id key
        def idKey = findFieldName(this.idFieldName, formattedVals)
        if (idKey) throw new IllegalArgumentException("id cannot be in vals: idFieldName:${idFieldName} formattedVals:${formattedVals}")

        if (!this.data[idv]) {
            this.data[idv] = new HashMap<String,String>()
            this.data[idv].put(this.idFieldName, idv)
            this.keySet.add(this.idFieldName)
        }
        

        this.data[idv].putAll(formattedVals)
        keySet.addAll(formattedVals.keySet())
    }


    ///////////////////////////////////////////////////////////////////////////
    // FILE READ/WRITE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Write the meta-data file for this MappedDataTable in the given
     * directory.
     *
     * @see #fileName(String, String, Map)
     * @param destDir The directory in which to write the file.
     * @param args Optional args passed to fileName().
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
            idFieldName:idFieldName,
            vine:vine,
            dateFormatPattern:dateFormat.toPattern(),
            //dateFormat:dateFormat
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



