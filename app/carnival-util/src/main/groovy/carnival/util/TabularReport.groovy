package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.yaml.snakeyaml.Yaml

import groovy.sql.*
import groovy.transform.ToString
import groovy.transform.Synchronized
import groovy.transform.WithReadLock
import groovy.transform.WithWriteLock
import static groovy.json.JsonOutput.*



/**
 * TabularReport is an extension of GenericDataTable that has convenience
 * methods for greating a tabular report.
 *
 */
@ToString(includeNames=true)
class TabularReport extends GenericDataTable {


    ///////////////////////////////////////////////////////////////////////////
    // FACTORY
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Create a TabularReport from a pair of data and meta-data files.
     *
     * @param dir The directory in which to look for the files.
     * @param name The name prefix for the data and meta-data files.
     * @return A new TabularReport.
     *
     */
    static public TabularReport createFromFiles(File dir, String name) {
        log.trace "createFromFiles dir:${dir?.canonicalPath} name:$name"

        assert dir
        assert dir.exists()
        assert dir.isDirectory()

        def metaFile = metaFile(dir, name)
        assert metaFile.exists()
        assert metaFile.length() > 0

        def dataFile = dataFile(dir, name)
        assert dataFile.exists()
        assert dataFile.length() > 0

        //log.trace "${dataFile.text}"
        def dataFileText = dataFile.text
        if (dataFileText) dataFileText = dataFileText.trim()

        def yaml = new org.yaml.snakeyaml.Yaml(new DataTableConstructor())
        def meta = yaml.load(metaFile.text)
        assert meta.name

        def dataTable = new TabularReport(
            name:meta.name, 
        )

        if (dataFileText) {
            def csvReader = CsvUtil.createReaderHeaderAware(dataFileText)
            if (!CsvUtil.hasNext(csvReader)) {
                log.warn "error in createFromFiles for file $dataFile. no data found."
            }
            dataTable.dataAddAll(csvReader)
            dataTable.readFrom = dataFile
        }

        return dataTable
    }



    ///////////////////////////////////////////////////////////////////////////
    // STATIC CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /** logger */
    static Logger log = LoggerFactory.getLogger(TabularReport)


    /**
     * Wrapper class for GenericDataTable meta-data.
     *
     */
    static class MetaData extends GenericDataTable.MetaData {

        /** Report descriptor as a map */
        Map reportDescriptor = [:]

        /**
         * Constructor from a map of args
         * @param args Map of args
         */
        public MetaData(Map args) {
            setFields(args)
        }

        /**
         * Constructor from a tabular report.
         * @param mdt Source tabular report
         */
        public MetaData(TabularReport mdt) {
            assert mdt
            setFields (
                name: mdt.name,
                reportDescriptor: mdt.reportDescriptor
            )
        }

        protected void setFields(Map args) {
            super.setFields(args)
            if (args.containsKey('reportDescriptor')) this.reportDescriptor = args.reportDescriptor
        }

    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** report descriptor */
    Map reportDescriptor = [:]

    /**
     * a started report can be built out horizontally, ie adding new columns
     * points, if not started, reports can be build vertically, ie adding new
     * rows.
     */
    boolean started = false


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /**
     * No-arg constructor.
     *
     */
    public TabularReport() {
        super()
    }


    /**
     * Make from MetaData.
     *
     */
    public TabularReport(MetaData args) {
        this()
        this.name = args.name
        this.reportDescriptor = args.reportDescriptor
    }


    /**
     * A map constructor.
     *
     */
    public TabularReport(Map args) {
        this(new MetaData(args))
    }



    ///////////////////////////////////////////////////////////////////////////
    // FILE READ/WRITE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Get the data as a list of maps where the map entries are ordered as per
     * the ordering of this object.
     *
     * @return A list of maps with ordered entries.
     *
     */
    public List<Map<String,String>> getOrderedData() {
        List<String> keys = this.keySet.toList()
        def keyComparator = new OrderBy([{ keys.indexOf(it) }])

        List<Map<String,String>> od = new ArrayList<TreeMap<String,String>>()
        
        data.iterator().each { rec ->
            SortedMap or = new TreeMap(keyComparator)
            or.putAll(rec)
            od << or
        }

        return od
    }


    /** 
     * Write a neat data file, which means NULL and MISSING values are rendered
     * as blanks.
     *
     * @param destDir the directory in which to write the file.
     * @param args Optional args which are modified to include:
     * [fileNameSuffix:'-neat', missingVal:"", nullVal:""]
     * @return The file that was written.
     *
     */
    @WithReadLock
    public File writeNeatDataFile(File destDir, Map args = [:]) {
        writeDataFile(
            destDir, 
            args + [fileNameSuffix:'-neat', missingVal:"", nullVal:""]
        )
    }




    ///////////////////////////////////////////////////////////////////////////
    // DATA ADD VARIABLES
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * A dictionary designed to contain lookup maps of identifier fields to 
     * records, keyed by the name of the identifier field.
     *
     */
    Map<String,Map<String,Collection<Map>>> idvMappers = new HashMap<String,Map<String,Collection<Map>>>()


    /** 
     * Get the lookup map for the given identifier field.
     *
     * @param idField The name of the identifier field for which the lookup is
     * requested.
     * @return The identifier lookup for the given identifier field.
     *
     */
    @Synchronized
    public Map<String,Collection<Map>> getLookup(String idField) {
        def idf = toFieldName(idField)
        Map<String,Collection<Map>> idvMap = idvMappers.get(idf)
        if (!idvMap) {
            idvMap = buildLookup(idf)
            idvMappers.put(idf, idvMap)
        }
        return idvMap
    }


    /** 
     * Create a lookup map from identifier value to the record for the given
     * identifier field.
     *
     * @param idField The identifier field.
     * @return A newly created lookup map from identifier value to record.
     *
     */
    @Synchronized
    public Map<String,Collection<Map>> buildLookup(String idField) {
        log.trace "TabularReport.buildLookup idField:$idField buildLookup..."

        def idf = toFieldName(idField)

        // build a map of existing records
        Map<String,Collection<Map>> idvMap = new HashMap<String,Collection<Map>>()
        this.dataIterator().each { rec ->
            def recIdv = rec.get(idf)
            def mappedRecs = idvMap.get(recIdv)
            if (!mappedRecs) {
                mappedRecs = []
                idvMap.put(recIdv, mappedRecs)
            }
            mappedRecs << rec
        }
        log.trace "idField:$idField buildLookup done."
        return idvMap
    }


    /**
     * Start must be called before dataAddAttributes() is called.
     *
     * dataAddAttributes() will link data only to identifiers already present 
     * in the underlying generic data table.  This seems like a half-baked
     * idea, but I think adding a start() method helped by splitting the phases
     * of interraction with TabularReport objects.  Before start() the data are
     * built out vertically, with new identifiers being added.  After start(),
     * the data can be built out horizontally via dataAddAttributes().
     *
     */
    @Synchronized
    public start() {
        this.started = true
    }



    ///////////////////////////////////////////////////////////////////////////
    // DATA ADD VARIABLES
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Add the given key value pairs to the data for the given identifier. If
     * there is already data for the given identifier, ensure that the provided
     * data would not overwrite existing data.
     *
     * @param idField The field to use as an identifier to link the new attributes.
     *                For example, 'empi'.
     * @param vals The map of key value pairs to set as the data for the given id.
     *
     * @throws IllegalArgumentException if the given data would overwrite
     *         existing data.
     *
     */
    @WithWriteLock
    public void dataAddAttributes(String idField, Map<String,String> vals) {
        //log.debug "dataAddAttributes idField:$idField vals:$vals"

        if (!this.started) throw new IllegalStateException('not started')

        assert idField
        assert vals
        
        // propertly format the supplied id field
        def idf = toFieldName(idField)

        // get the id value from the supplied value map
        if (!vals.containsKey(idf)) throw new IllegalArgumentException("vals must contain a value for $idf")
        def idv = formatIdValue(vals.get(idf))

        // get idv map
        def idvMap = getLookup(idField)

        // find all existing records with the given identifier
        //def existingRecords = this.data.findAll { it.get(idf) == idv }
        def existingRecords = idvMap.get(idv)
        //if (!existingRecords) throw new IllegalArgumentException("cannot find records with ${idf}:${idv}")
        if (!existingRecords) {
            def msg = "TabularReport.dataAddAttributes ${this.name} cannot find any records with ${idf}:${idv}"
            log.info(msg)
            return
        }

        // ...
        def formattedVals = vals.collectEntries{ k, v -> 
            [(toFieldName(k)): v]
        }

        // check that the data in vals are disjoint by key from the existing
        // data.  if not, throw an error.
        existingRecords.each { rec ->
            if (rec && !rec.keySet().disjoint(formattedVals.keySet())) {
                def commonKeys = rec.keySet().intersect(formattedVals.keySet())
                commonKeys.each { k ->
                    if (rec.get(k) != formattedVals.get(k)) throw new IllegalArgumentException("rec $k:${rec.get(k)} != formattedVals $k:${formattedVals.get(k)}")
                }
            }
        }

        // insert the new attributes
        existingRecords.each { rec ->
            vals.each { k, v ->
                def fn = toFieldName(k)
                def cv = toCleanValue(fn, v)
                rec.put(fn, cv)
            } 
        }

        keySet.addAll(formattedVals.keySet())
    }

}



