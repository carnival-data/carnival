package carnival.util



import java.text.DateFormat
import java.text.SimpleDateFormat

import groovy.transform.Memoized

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper
import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.CSVReader

import org.yaml.snakeyaml.Yaml

import groovy.sql.*
import groovy.transform.ToString
import groovy.transform.Synchronized
import groovy.transform.WithReadLock
import groovy.transform.WithWriteLock
import static groovy.json.JsonOutput.*




/**
 * FeatureReport provides functionality to aggregate data into a single data
 * table with facilities to output those data in representation suitable for
 * input into a machine learning or other statistical computation process.
 *
 * FeatureReport extends MappedDataTable, so there can be only one row per
 * subject.  It is expected that the first step in building a FeatureReport
 * will be adding all the unique subject identifiers.  Proceeding steps will
 * iteratively add feature sets.
 *
 */
//@ToString(excludes=['data'], includeNames=true)
@ToString(includeNames=true)
class FeatureReport extends MappedDataTable {


    ///////////////////////////////////////////////////////////////////////////
    // STATIC FACTORY
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a FeatureReport from previously saved CSV data and YAML meta
     * files. 
     *
     */
    static public FeatureReport createFromFiles(File dir, String name) {
        log.trace "FeatureReport.createFromFiles dir:${dir?.canonicalPath} name:$name"

        // get the metadata from file
        def meta = loadMetaDataFromFile(dir, name)

        // construct a mapped data table object from 
        def mdt = new FeatureReport(meta)

        // load the file data
        loadDataFromFile(dir, name, mdt)

        return mdt
    }



    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')

    /** */
    static final String DATE_SHIFT_SUFFIX = '_SHIFTED'



    ///////////////////////////////////////////////////////////////////////////
    // STATIC CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Wrapper class for MappedDataTable meta-data.
     *
     */
    static class MetaData extends MappedDataTable.MetaData {

        public MetaData(Map args) {
            setMeta(args)
        }

        public MetaData(FeatureReport mdt) {
            assert mdt
            setMeta (
                name: mdt.name,
                secondaryIdFieldMap: mdt.secondaryIdFieldMap,
                reportDescriptor: mdt.reportDescriptor
            )
        }

        protected void setMeta(Map args) {
            super.setMeta(args)
            if (args.containsKey('reportDescriptor')) this.meta.reportDescriptor = args.reportDescriptor
        }

        public Map getReportDescriptor() {
            return meta.reportDescriptor
        }
    }


    /**
     * An enum describing the permissible ways data can be added.
     * 
     */
    static final enum DataMode {
        /**
         * New subjects can be added, which in terms of the report means that
         * new rows can be added.
         *
         */
        ADD_SUBJECT,

        /**
         * New features can be added, which in terms of the report means that
         * new columns can be added.
         *
         */
        ADD_FEATURE,

        /**
         * Existing features can be modified.
         *
         */
        SET_FEATURE
    }


    /** */
    static final enum DateWords {
        DATE
    }



    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Shift a date by the provided number of skew days.
     *
     * @param dateStr The input date as a String in the format: 
     * yyyy-MM-dd HH:mm:ss.S
     * @param skewDays The number of days to skew the input date.
     * @return The shifted date as a String in the input format.
     * @throws Assertion If the skewDays is 0.
     *
     */
    @Memoized
    static public String shiftDate(String dateStr, int skewDays) {
        assert skewDays != 0

        SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss.S')
        df.setLenient(false)

        def year2000 = Calendar.getInstance()
        year2000.clear()
        year2000.set(2000, 0, 1)
        df.set2DigitYearStart(year2000.time)

        def parsedDate
        try {
            parsedDate = df.parse(dateStr)
        } catch (Exception e) {
            log.warn "WARN: shiftDate could not parse date $dateStr"
            return null
        }

        def cal = Calendar.instance
        cal.setTime(parsedDate)
        cal.roll(Calendar.DAY_OF_YEAR, skewDays)

        def skewedDate = cal.time
        return df.format(skewedDate)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** report descriptor */
    Map reportDescriptor = [:]

    /** set of currently active data modes */
    Set<DataMode> dataModes = new HashSet<DataMode>()

    /** map of field name to feature descriptor */
    Map<String,FeatureSetDescriptor> featureDescriptors = new HashMap<String,FeatureSetDescriptor>()


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /**
     * No-arg constructor.
     *
     */
    public FeatureReport() {
        super()
        setOrderKeysByInsertion()
    }


    /**
     * Make from MetaData.
     *
     */
    public FeatureReport(FeatureReport.MetaData args) {
        super(args)
        setOrderKeysByInsertion()
        this.reportDescriptor = args.reportDescriptor
    }


    /**
     * A map constructor.
     *
     */
    public FeatureReport(Map args) {
        super(args)
        setOrderKeysByInsertion()
        if (args.containsKey('reportDescriptor')) this.reportDescriptor = args.reportDescriptor
    }




    ///////////////////////////////////////////////////////////////////////////
    // DATA MODE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Set the currently active data modes to just the single data mode
     * provided.
     *
     * @param dm The DataMode.
     *
     */
    public void dataMode(DataMode dm) {
        dataModes.removeAll()
        dataMode(dm, true)
    }


    /** 
     * Set the provided data mode to active or inactive based on the input
     * boolean value.
     *
     * @param dm The DataMode.
     * @param val The boolean value indicating whether the data mode should be
     * active or inactive.
     *
     */
    public void dataMode(DataMode dm, boolean val) {
        if (val) dataModes.add(dm)
        else dataModes.remove(dm)
    }


    /** 
     * Set the currently active data modes to just the data modes provided.
     *
     * @param dms The collection of data modes that will be the currently
     * active data modes.
     *
     */
    public void dataModes(Collection<DataMode> dms) {
        dataModes.removeAll()
        dataModes.addAll(dms)
    }


    /** 
     * Execute the provided closure encuring that the that provided data mode
     * is true for the closure's execution.
     *
     * @param dm The DataMode.
     * @param cl The closure to execute.
     *
     */
    public void withDataMode(DataMode dm, Closure cl) {
        def curMode = dataModes.contains(dm)
        def curDelegate = cl.getDelegate()
        cl.delegate = this
        dataMode(dm, true)
        try {
            cl()
        } finally {
            dataMode(dm, curMode)
            cl.delegate = curDelegate
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // FILE READ/WRITE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Write the data file in "neat" form, using blanks for missing and null
     * data.
     *
     * @param destDir The directory in which to write the file.
     * @param args The args to be passed to superclass writeDataFile, with the
     * following automatically set: 
     * [fileNameSuffix:'-neat', missingVal:"", nullVal:""]
     * @return The File that was written.
     *
     */
    @WithReadLock
    public File writeNeatDataFile(File destDir, Map args = [:]) {
        writeDataFile(
            destDir, 
            args + [fileNameSuffix:'-neat', missingVal:"", nullVal:""]
        )
    }


    /** 
     * Write the meta file.
     * 
     * @param destDir The directory in which to write the file.
     * @param args The args to be passed to superclass writeMetaFile.
     * @return The File that was written.
     *
     */
    @Override @WithReadLock
    public File writeMetaFile(File destDir, Map args = [:]) {
        super.writeMetaFile(destDir, args)
    }


    /** 
     * Write the data file.
     *
     * @param destDir The directory in which to write the file.
     * @param args The args to be passed to superclass writeDataFile.
     * @return The File that was written.
     *
     */
    @Override @WithReadLock
    public File writeDataFile(File destDir, Map args = [:]) {
        super.writeDataFile(destDir, args)
    }



    ///////////////////////////////////////////////////////////////////////////
    // DATA DICTIONARY
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Convenience method to create the data dictionary and write it to file.
     *
     * @see dataDictionary()
     * @param destDir The directory in which to write the file.
     * @param args The args to be passed to writeDataFile of the created data
     * dictionary.
     * @return The File that was written.
     *
     */
    @WithWriteLock
    public File writeDataDictionary(File destDir, Map args = [:]) {
        log.trace "writeDataDictionary destDir:$destDir args:$args"
        def dd = dataDictionary()
        dd.writeNeatDataFile(destDir, args)
    }


    /** 
     * Create a data dictionary, which is a FeatureReport, for this 
     * FeatureReport.
     *
     * @return The data dictionary of this object in FeatureReport form.
     *
     */
    @WithWriteLock
    public FeatureReport dataDictionary() {
        log.trace "dataDictionary() featureDescriptors: $featureDescriptors"

        def dd = new FeatureReport(
            name:"${this.name}-dd", 
            idFieldName:'FEATURE_NAME', 
            idKeyType:KeyType.GENERIC_STRING_ID
        )
        dd.setOrderedKeysMappedDataTableDefault()

        dd.withDataMode(DataMode.ADD_SUBJECT) {
            featureDescriptors.each { fn, fdesc ->
                dd.addSubject(fn)
            }
        }

        dd.withDataMode(DataMode.ADD_FEATURE) {
            featureDescriptors.each { fn, fdesc ->
                dd.addMultiFeature(fn, 'DATA_TYPE', fdesc.dataTypes.toList()*.name())
                //dd.addFeature(fn, 'NAME', fdesc.name)
                if (fdesc.description != null) dd.addFeature(fn, 'DESCRIPTION', fdesc.description)
                //if (fdesc.query != null) dd.addFeature(fn, 'QUERY', fdesc.query)
            }
        }

        return dd        
    }



    ///////////////////////////////////////////////////////////////////////////
    // FEATURE SET RECIPE
    ///////////////////////////////////////////////////////////////////////////

    /** */


    ///////////////////////////////////////////////////////////////////////////
    // KEY SET
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Add a new key before an existing key in the key (feature set name) set.
     *
     * @param keyToAdd The new key (feature set name).
     * @param existingKey The existing key.
     * @throws Assertion Assert that the inputs are not null, that existingKey
     * already exists, and that the keyToAdd does not.
     *
     */
    public void addKeyBefore(String keyToAdd, String existingKey) {
        assert keyToAdd
        assert existingKey
        assert keySet.contains(existingKey)
        assert !keySet.contains(keyToAdd)

        List<String> orderedKeys = new ArrayList<String>()
        orderedKeys.addAll(keySet)
        def existingKeyIdx = orderedKeys.indexOf(existingKey)
        assert existingKeyIdx >= 0
        orderedKeys.add(existingKeyIdx, keyToAdd)
        keySet.clear()
        keySet.addAll(orderedKeys)
    }


    ///////////////////////////////////////////////////////////////////////////
    // DE-IDENTIFICATION
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Convenience method that acccepts dateFormat as a String, which will be
     * used to create a SimpleDateFormat object.
     *
     * @param dateFormat The date format as a String used to parse and write 
     * dates.
     *
     */
    public Map shiftDates(int calendarUnit, Map<String,Integer> shiftVals, String dateFormat, Set<String> dateFields = []) {
        SimpleDateFormat df = new SimpleDateFormat(dateFormat)        
        shiftDates(calendarUnit, shiftVals, df, dateFields)
    }



    /** 
     * Shift all the date features in this FeatureReport as per the input
     * parameters.
     *
     * @param calendarUnit The calendar unit by which to shift the dates, 
     * expected to be Calendar.DAY_OF_YEAR most of the time.
     * @param shiftVals A map of subject ID to number of calendar units by
     * which all dates associated with the subject ID should be shifted.
     * @param dateFormat The date format used to parse and write dates.
     * @param dateFields Optional list of date fields to shift. If not
     * provided, all field that contain any of DateWords will be shifted.
     * @return A map result containing the date fields that were shifted
     * along with any error conditions that were encountered.
     *
     */
    public Map shiftDates(int calendarUnit, Map<String,Integer> shiftVals, DateFormat dateFormat, Set<String> dateFields = []) {
        shiftDates(calendarUnit:calendarUnit, shiftVals:shiftVals, dateFormat:dateFormat, dateFields:dateFields)
    }




    @Memoized
    static tryParseDate(List<DateFormat> dateFormats, String dateStr) {
        def parsedDate
        def parsedDateFormat
        dateFormats.each { dateFormat ->
            if (parsedDate != null) return
            try {
                parsedDate = dateFormat.parse(dateStr)
                parsedDateFormat = dateFormat
            } catch (Exception e) {
                // ignore
            }
        }
        if (parsedDate == null) return null
        else return [parsedDate:parsedDate, parsedDateFormat:parsedDateFormat]
    }




    /** 
     * Shift all the date features in this FeatureReport as per the input
     * parameters.
     *
     * @param calendarUnit The calendar unit by which to shift the dates, 
     * expected to be Calendar.DAY_OF_YEAR most of the time.
     * @param shiftVals A map of subject ID to number of calendar units by
     * which all dates associated with the subject ID should be shifted.
     * @param dateFormat The date format used to parse and write dates.
     * @param dateFields Optional list of date fields to shift. If not
     * provided, all field that contain any of DateWords will be shifted.
     * @param valuesToIgnore Optional list of string values to ignore, eg. 'NULL'.
     * @return A map result containing the date fields that were shifted
     * along with any error conditions that were encountered.
     *
     */
    public Map shiftDates(Map args) {
        assert args.calendarUnit
        assert args.shiftVals
        assert args.dateFormat || args.dateFormats

        def calendarUnit = args.calendarUnit
        def shiftVals = args.shiftVals
        def dateFormats = args.dateFormats ?: [args.dateFormat]
        def valuesToIgnore = args.valuesToIgnore ? args.valuesToIgnore.toSet() : new HashSet<String>()

        // remove date leniencey from format
        //dateFormats = dateFormats.collect { it.clone().setLenient(false) }
        dateFormats.each { it.setLenient(false) }

        // if not provided with date fields, look for date fields by field name
        def dateFields = args.dateFields
        if (!dateFields) {
            dateFields = []
            DateWords.each { dw -> 
                dateFields.addAll(
                    keySet.findAll({ 
                        it.toUpperCase().contains(dw.name())
                    })
                )
            }
        }
        dateFields = dateFields.collect { DataTable.toFieldName(it) }
        log.debug "dateFields: $dateFields"

        // if given fields to ignore, remove them from dateFields
        if (args.fieldsToIgnore) {
            assert args.fieldsToIgnore instanceof Collection
            args.fieldsToIgnore.each { dateFields.remove(DataTable.toFieldName(it)) }
        }

        Set<String> errorFields = new HashSet<String>()
        Set<String> errorMessages = new HashSet<String>()
        Calendar cal = Calendar.instance

        // logging
        final int totalRecs = dateFields.size()
        //final int LOG_INTERVAL = Math.floorDiv(totalRecs, 5) ?: 1
        //final int LOG_INTERVAL = 100
        //final String progressMsg = "shiftDates ${name} ${dateFieldIdx} ${dateField}"
        //int totalCompleted = 0

        dateFields.eachWithIndex { dateField, dateFieldIdx ->

            Log.progress(log, 'trace', "shiftDates ${name} ${dateFieldIdx} ${dateField}", totalRecs, dateFieldIdx)

            def shiftedFieldName = dateField + DATE_SHIFT_SUFFIX
            addKeyBefore(shiftedFieldName, dateField)
            //keySet.add(shiftedFieldName)

            data.each { k, v ->
                // check that we have a date
                def dateStr = v.get(dateField)
                //log.trace "dateField:$dateField dateStr:$dateStr"
                if (dateStr == null) return

                // optionally ignore date string
                if (valuesToIgnore && valuesToIgnore.contains(dateStr)) return

                // check that there is a shift value for the given key
                if (!shiftVals.containsKey(k)) {
                    errorMessages << "No shift value for $k"
                    errorFields << dateField
                    return
                }

                // chech for null or zero
                def shiftVal = shiftVals.get(k)
                if (shiftVal == null) {
                    errorMessages << "Null shift value for $k"
                    errorFields << dateField
                    return
                }
                if (shiftVal == 0) {
                    errorMessages << "Zero shift value for $k"
                    errorFields << dateField
                    return
                }

                def tryParseDate = tryParseDate(dateFormats, dateStr)
                if (tryParseDate == null) {
                    log.warn "ShiftDates could not parse date - dateStr:$dateStr field:$dateField"
                    errorMessages << "ShiftDates could not parse date - dateStr:$dateStr field:$dateField"
                    errorFields << dateField
                    return
                }

                // put the successful date format first in the list
                if (dateFormats[0] != tryParseDate.parsedDateFormat) {
                    dateFormats = dateFormats.minus(tryParseDate.parsedDateFormat)
                    dateFormats = dateFormats.plus(0, tryParseDate.parsedDateFormat) 
                }

                //def parsedDate
                //def parsedDateFormat
                //while (parsedDate == null) {
                //    dateFormats.each { dateFormat ->
                //        try {
                //            parsedDate = dateFormat.parse(dateStr)
                //            parsedDateFormat = dateFormat
                //        } catch (Exception e) {
                //            // ignore
                //        }
                //    }
                //}

                //if (parsedDate == null) {
                //    errorMessages << "ShiftDates could not parse date - dateStr:$dateStr field:$dateField"
                //    errorFields << dateField
                //    return
                //}

                //try {
                //    parsedDate = dateFormat.parse(dateStr)
                //} catch (Exception e) {
                //    errorMessages << "ShiftDates could not parse date $dateStr - field:$dateField record:$v"
                //    errorFields << dateField
                //}
                //if (parsedDate == null) return

                //log.debug "dateStr: $dateStr"
                //log.debug "parsedDate: ${tryParseDate.parsedDate}"
                //log.debug "parsedDateFormat: ${tryParseDate.parsedDateFormat.toPattern()}"

                cal.setTime(tryParseDate.parsedDate)
                cal.add(calendarUnit, shiftVal)
                def shiftedDate = cal.time
                def shiftedDateStr = tryParseDate.parsedDateFormat.format(shiftedDate)

                v.put(shiftedFieldName, shiftedDateStr)
            }
        }

        dateFields.each { dateField ->
            if (!errorFields.contains(dateField)) removeFeatureSet(dateField)
        }

        return [
            dateFields: dateFields,
            errorFields: errorFields,
            errorMessages: errorMessages
        ]
    }


    ///////////////////////////////////////////////////////////////////////////
    // FEATURE EXTRACTION
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Sets a boolean feature to true if a value with the given feature name is 
     * present in the data for a particular subject.
     *
     * @param data The data that should contain the subject ID.
     * @param subjectIdKey The key of the input data that should contain the 
     * subject ID.
     * @param featureNameKey The key of the input data that is used to name the
     * new feature set.
     *
     * Data example:
     *   EMPI   ICD
     *   1      C1
     *   2      C2
     *
     * Example call:
     *   extractBooleanFeatures(data, 'EMPI', 'ICD')
     *
     * Results:
     *   EMPI  C1     C2
     *   1     true   false
     *   2     false  true
     * 
     *
     */
    public void extractBooleanFeatures(Collection<Map> data, String subjectIdKey, String featureNameKey) {
        // check mode
        if (!dataModes.contains(DataMode.ADD_FEATURE)) throw new IllegalStateException('Data mode ADD_FEATURE required to call extractBooleanFeatures()')

        // extract feature
        boolean foundAtLeastOneSubject = false
        boolean foundAtLeastOneFeature = false
        data.each { rec ->
            if (!rec.containsKey(subjectIdKey)) return
            foundAtLeastOneSubject = true

            if (!rec.containsKey(featureNameKey)) return
            foundAtLeastOneFeature = true

            def subjectId = rec.get(subjectIdKey)
            if (!this.containsIdentifier(subjectId)) throw new IllegalArgumentException("Could not find subjectId: $subjectId")

            def featureName = rec.get(featureNameKey)

            this.addFeature(subjectId, featureName, String.valueOf(true))
        }

    }


    /** 
     * @param data Collection<Map> matrix of data
     * @param subjectIdKey String data key for subjectId
     * @param featureName String name of feature
     * @param featureIdKey String data key for field that identifies the linked entity
     * @param featureValueKey String data key for the feature value pertaining to the linked entity
     *
     * Data example:
     *   EMPI   STAGE   DATE
     *   1      3a      1/1/2019
     *   1      3       2/1/2019
     *   2      1       5/1/2017
     *   3      4       11/8/2016
     *
     * Example call:
     *   extractCompoundMultiFeatures(
     *     data:data, 
     *     subjectIdKey:'EMPI', 
     *     featureName:'CANCER_SEVERITY',
     *     featureValueKeys:['STAGE','DATE']
     *   )
     *
     * Multi-feature results:
     *   EMPI   CANCER_SEVERITY_1_STAGE   CANCER_SEVERITY_1_DATE   CANCER_SEVERITY_2_STAGE   CANCER_SEVERITY_2_DATE
     *   1      3a                        1/1/2019                 3                         2/1/2019
     *   2      1                         5/1/2017
     *   3      4                         11/8/2016
     *
     */
    public void extractCompoundMultiFeatures(Map args) {
        // check mode
        if (!dataModes.contains(DataMode.ADD_FEATURE)) throw new IllegalStateException('Data mode ADD_FEATURE required to call extractCompoundMultiFeatures()')

        // verify arguments
        ['data', 'subjectIdKey', 'featureValueKeys'].each {
            assert args.containsKey(it) : "args must contain $it"
        }        
        assert args.containsKey('featureName') || args.containsKey('featureDescriptor')

        // marshall arguments
        Collection<Map> data = args.get('data')
        String subjectIdKey = args.get('subjectIdKey')
        List<String> featureValueKeys = args.get('featureValueKeys')

        String featureName
        if (args.containsKey('featureDescriptor')) featureName = args.get('featureDescriptor').name
        else featureName = args.get('featureName')
        assert featureName != null : "featureName is null"

        // record the feature descriptor if present
        if (args.containsKey('featureDescriptor')) {
            def fd = args.get('featureDescriptor')
            featureValueKeys.each { fvk ->
                def fdn = "${fd.name}_${fvk}"
                this.featureDescriptors.put(fdn, fd)
            }
        }

        // compute feature names
        def featureNames = multiFeatureNames(
            data:data, 
            subjectIdKey:subjectIdKey, 
            featureValueKeys:featureValueKeys,
            featureName:featureName
        )

        // logging
        final int totalRecs = data.size()
        final int LOG_INTERVAL = Math.floorDiv(totalRecs, 10) ?: 1
        final String progressMsg = "extractCompoundMultiFeatures ${name} ${subjectIdKey} ${featureValueKeys}"
        int totalCompleted = 0

        // add features to report
        def groups = data.groupBy { it.get(subjectIdKey) }
        groups.each { subjectId, recs ->
            if (args.includeNum) {
                def fn = "${featureName}_COUNT"
                def fv = recs.size().toString()
                this.addFeature(subjectId, fn, fv)
            }
            recs.eachWithIndex { rec, recIdx ->
                if (totalCompleted++%LOG_INTERVAL == 0) Log.progress(log, 'trace', progressMsg, totalRecs, totalCompleted-1)

                def fn = featureNames[recIdx]
                featureValueKeys.each { fvk ->
                    def fnk = "${fn}_${fvk}"
                    def fv = rec.get(fvk)
                    if (fv != null) this.addFeature(subjectId, fnk, fv)    
                }
            }
        }
    }


    /** 
     * @param data Collection<Map> matrix of data
     * @param subjectIdKey String data key for subjectId
     * @param featureName String name of feature
     * @param featureIdKey String data key for field that identifies the linked entity
     * @param featureValueKey String data key for the feature value pertaining to the linked entity
     *
     * Data example:
     *   EMPI   STAGE
     *   1      3a
     *   1      3
     *   2      1
     *   3      4
     *
     * Example call:
     *   extractMultiFeature(
     *     data:data, 
     *     subjectIdKey:'EMPI', 
     *     featureName:'CANCER_SEVERITY',
     *     featureValueKey:'STAGE'
     *   )
     *
     * Multi-feature results:
     *   EMPI   CANCER_SEVERITY_1   CANCER_SEVERITY_2   CANCER_SEVERITY_COUNT
     *   1      3a                  3                   2
     *   2      1                                       1
     *   3      4                                       1
     *
     */
    public void extractMultiFeatures(Map args) {
        // check mode
        if (!dataModes.contains(DataMode.ADD_FEATURE)) throw new IllegalStateException('Data mode ADD_FEATURE required to call extractMultiFeatures()')

        // verify arguments
        ['data', 'subjectIdKey', 'featureValueKey'].each {
            assert args.containsKey(it) : "args must contain $it"
        }        

        // marshall arguments
        Collection<Map> data = args.get('data')
        String subjectIdKey = args.get('subjectIdKey')
        String featureValueKey = args.get('featureValueKey')

        //String featureName = args.containsKey('featureName') ? args.get('featureName') : featureValueKey
        String featureName
        if (args.containsKey('featureDescriptor')) featureName = args.get('featureDescriptor').name
        else if (args.containsKey('featureName')) featureName = args.get('featureName')
        else featureName = featureValueKey

        // record the feature descriptor if present
        if (args.containsKey('featureDescriptor')) {
            def fd = args.get('featureDescriptor')
            this.featureDescriptors.put(fd.name, fd)
        }

        // compute feature names
        def featureNames = multiFeatureNames(
            data:data, 
            subjectIdKey:subjectIdKey,
            featureValueKeys:[featureValueKey],
            featureName:featureName
        )

        // add features to report
        def groups = data.groupBy { it.get(subjectIdKey) }

        // check for multiplicity
        boolean multiplicity = groups.find { subjectId, recs -> recs.size() > 1 }

        groups.each { subjectId, recs ->
            if (args.includeNum) {
                def fn = "${featureName}_COUNT"
                def fv = recs.findAll({it.get(featureValueKey) != null}).size().toString()
                this.addFeature(subjectId, fn, fv)
            }
            recs.eachWithIndex { rec, recIdx ->
                def fn = multiplicity ? featureNames[recIdx] : featureName
                def fv = rec.get(featureValueKey)
                if (fv != null) this.addFeature(subjectId, fn, fv)
            }
        }
    }


    /** 
     * Given some data, a subject ID key, and a feature name, construct a list
     * of multi-feature names that cover the number of discrete 
     *
     * @param args.data The input data that must contain the subject ID key.
     * @param args.subjectIdKey The key of args.data that contains the subject
     * IDs.
     * @param args.featureName The name used as a prefix for constructed multi-
     * feature names.
     * @return A list of multi-feature names.
     *
     * Data example:
     *   EMPI   TUMOR_ID   OTHER_FEATURE
     *   1      1          a
     *   1      2          b
     *   2      3          c
     *   3      4          d
     *   4                 e
     *   4                 f
     *   4                 g
     *
     * Example call:
     *   multiFeatureNames(
     *     data,
     *     subjectIdKey:'EMPI', 
     *     featureValueKeys:['TUMOR_ID']
     *     featureName:'SEVERITY', 
     *   )
     *
     * Results:
     *   ['SEVERITY_1', 'SEVERITY_2']
     *
     */
    public List<String> multiFeatureNames(Map args) {
        Collection<Map> data = args.get('data')
        String subjectIdKey = args.get('subjectIdKey')
        String featureName = args.get('featureName')
        Collection<String> featureValueKeys = args.get('featureValueKeys')

        assert data
        assert subjectIdKey
        assert featureName
        assert featureValueKeys

        Set<String> featureValueKeySet = featureValueKeys.toSet()
        //log.debug "featureValueKeySet:$featureValueKeySet"

        // group by subjectId
        // count max number of linked entities per subject
        def groups = data.groupBy { it.get(subjectIdKey) }
        int maxCount = 0
        groups.each { subjectId, recs ->
            //log.debug "subjectId:$subjectId recs:$recs"
            def relevantRecs = recs.findAll { 
                it.keySet().intersect(featureValueKeySet).size() > 0 
            }
            //log.debug "relevantRecs:$relevantRecs"
            maxCount = Math.max(maxCount, relevantRecs.size()) 
        }

        if (maxCount == 0) return []

        List<String> out = new ArrayList<String>()
        (1..maxCount).each { out <<  multiFeatureName(featureName, it) }

        return out
    }


    /** 
     * Given an integer and a feature name, return a String like:
     *   SOME_NAME_1.
     *
     * @param featureName The feature name.
     * @param num The number to append to feature name.
     * @return The string that combines featureName and num.
     *
     */
    public String multiFeatureName(String featureName, int num) {
        "${featureName}_${num}"
    }


    /** 
     * Convenience method to call (String featureName, int num) on a collection
     * of feature values.
     *
     * @param featureName The base feature name.
     * @param featureValues A collection of feature values, which will be
     * counted and used to generate the numeric suffixes.
     * @return A list of multi-feature names.
     *
     */
    public List<String> multiFeatureNames(String featureName, Collection<String> featureValues) {
        List<String> out = new ArrayList<String>()
        (1..featureValues.size()).each { out << multiFeatureName(featureName, it) }
        return out
    }


    ///////////////////////////////////////////////////////////////////////////
    // DATA ADD
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Add a new subject.  Since FeatureReport is a MappedDataTable, attemtping
     * to add a subject that already exists will cause an Exception.
     *
     * @param subjectId The subject identifier.
     * @throws IllegalStateException If data mode ADD_SUBJECT is not active.
     *
     */
    @WithWriteLock
    public void addSubject(String subjectId) {
        // check mode
        if (!dataModes.contains(DataMode.ADD_SUBJECT)) throw new IllegalStateException('Data mode ADD_SUBJECT required to call addSubject()')

        // add the subject with no data
        Map data = [:]
        data.put(idFieldName, subjectId)
        this.dataAdd(data)
    }


    /** 
     * Add a new subject along with some features associated with that subject.
     *
     * @param subjectId The subject identifier.
     * @param features Features of the subject in Map form.
     * @throws IllegalStateException If data mode ADD_SUBJECT is not active.
     *
     */
    @WithWriteLock
    public void addSubject(String subjectId, Map<String,String> features) {
        // check mode
        if (!dataModes.contains(DataMode.ADD_SUBJECT)) throw new IllegalStateException('Data mode ADD_SUBJECT required to call addSubject()')

        // add the subject with no data
        features.put(idFieldName, subjectId)
        this.dataAdd(features)
    }



    /** 
     * Add a multi-feature to a subject.
     *
     * Example call:
     *   addMultiFeature('id1', 'ICD', ['140', '141'])
     *
     * Results:
     *   SUBJECT_ID  ICD_1  ICD_2
     *   id1         140    141
     *
     * @param subjectId The subject identifier.
     * @param featureName The name prefix of the multi-feature.
     * @param featureValues A list of feature values for the multi-feature.
     *
     */
    @WithWriteLock
    public void addMultiFeature(String subjectId, String featureName, List<String> featureValues) {
        def fnames = multiFeatureNames(featureName, featureValues)
        fnames.eachWithIndex { fn, fnIdx ->
            addFeature(subjectId, fn, featureValues[fnIdx])
        }
    }


    /** 
     * Convenience method that accepts feature descriptor arguments in map form
     * and calls addFeature(String, FeatureSetDescriptor, String) to add the
     * feature.
     *
     * @param subjectId The subject identifier.
     * @param featureDescriptorArgs A map of arguments that will be used to
     * create a FeatureSetDescriptor.
     * @param featureValue The feature value.
     *
     */
    @WithWriteLock
    public void addFeature(String subjectId, Map featureDescriptorArgs, String featureValue) {
        def fd = new FeatureSetDescriptor(featureDescriptorArgs)
        this.addFeature(subjectId, fd, featureValue)
    }


    /** 
     * Add a feature to the subject identified by subjectId.
     *
     * @param subjectId The subject identifier.
     * @param featureDescriptor The feature descriptor.
     * @param featureValue The feature value.
     *
     */
    @WithWriteLock
    public void addFeature(String subjectId, FeatureSetDescriptor featureDescriptor, String featureValue) {
        this.addFeature(subjectId, featureDescriptor.name, featureValue)
        this.featureDescriptors.put(featureDescriptor.name, featureDescriptor)
    }


    /** 
     * Add a feature to the subject identified by subjectId.
     *
     * @param subjectId The subject identifier.
     * @param featureName The feature name.
     * @param featureValue The feature value.
     *
     * @throws IllegalStateException if a feature with name featureName already exists for the subject.
     *
     */
    @WithWriteLock
    public void addFeature(String subjectId, String featureName, String featureValue) {
        assert subjectId != null
        assert featureName != null
        assert featureValue != null

        // check mode
        if (!dataModes.contains(DataMode.ADD_FEATURE)) throw new IllegalStateException('Data mode ADD_FEATURE required to call addFeature()')

        // check if feature already exists
        def fv = this.dataGet(subjectId)
        if (fv == null) throw new RuntimeException("no feature vector for ${subjectId}")

        def ffn = DataTable.toFieldName(featureName)
        if (fv.containsKey(ffn)) {
            throw new IllegalStateException("feature vector for $subjectId already contains a feature with name $featureName and value ${fv.get(ffn)}")
        }

        // add the feature
        this.dataAppend(subjectId, featureName, featureValue)
    }


    /** 
     * Add a set of features to the subject identified by subjectId.
     *
     * @param subjectId The subject identifier.
     * @param features Features in map form to associate with the subject.
     *
     * @throws IllegalStateException if any of the features in features already exist for the subject.
     *
     */
    @WithWriteLock
    public void addFeatures(String subjectId, Map<String,String> features) {
        // check mode
        if (!dataModes.contains(DataMode.ADD_FEATURE)) throw new IllegalStateException('Data mode ADD_FEATURE required to call addFeatures()')

        // check if feature already exists
        Map<String,String> conflicts = new HashMap<String,String>()
        
        def fv = this.dataGet(subjectId)
        if (fv == null) throw new IllegalArgumentException("Subject not found: ${subjectId}")

        features.each { k, v ->
            def ffn = DataTable.toFieldName(k)
            if (fv.containsKey(ffn)) conflicts.put(k, fv.get(ffn))
        }
        if (conflicts.size() > 0) throw new IllegalStateException("feature vector for $subjectId already contains the following features: ${conflicts}")

        // add features
        this.dataAppend(subjectId, features)
    }



    /** 
     * Remove a feature set by name from this report.
     *
     * @param featureName The name of the feature set to remove.
     *
     */
    @WithWriteLock
    public void removeFeatureSet(String featureName) {
        assert featureName

        def fn = DataTable.toFieldName(featureName)
        data.each { k, v ->
            v.remove(fn)
        }
        keySet.remove(fn)
    }


    /*@WithWriteLock
    public void reatainFeatureSets(Closure cl) {
        keySet.retainAll(cl)
        data.each { k, v ->
            def keys = v.keySet()
            if (!keySet.contains(v)) v.remove(fn)
        }

    }*/

}





/** 
 * Encapsulates the description of a feature, including its name, long form
 * description, the query that was used to generate it, and a set of
 * FeatureDataTypes.
 *
 */
/*class FeatureDescriptor {
    Set<FeatureDataType> dataTypes = new HashSet<FeatureDataType>()
    String name
    String description
}*/




