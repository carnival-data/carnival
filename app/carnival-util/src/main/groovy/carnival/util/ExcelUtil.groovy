package carnival.util



import groovy.transform.ToString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.format.DateTimeParseException
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.extractor.XSSFExcelExtractor
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.ss.usermodel.CellType



/**
 * Utility class to read XML based Microsoft Excel files.
 *
 */
class ExcelUtil {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** */
    static Logger log = LoggerFactory.getLogger(ExcelUtil)

    /** */
    static enum EXCEL { ROW_NUM }


	///////////////////////////////////////////////////////////////////////////
	// READ ENTIRE FILE VIA XSSFExcelExtractor
	///////////////////////////////////////////////////////////////////////////

    /**
     * Read an Excel file into a list of maps.
     *
     * @param inputFile Th Excel file.
     * @return A list of maps containing the data in the input file.
     *
     */
    static List<Map> readExcelFile(File inputFile) {
    	InputStream inp = new FileInputStream(inputFile);
    	def data
        try {
            data = readExcelFile(inp)
        } finally {
            inp.close()
        }
        return data
    }


    /**
     * Read an Excel file in to a list of maps.
     *
     * @param input An input stream of the Excel file data.
     * @return A list of maps containing the data in the input file.
     *
     */
    static List<Map> readExcelFile(InputStream input) {
        XSSFWorkbook wb = new XSSFWorkbook(input);
        XSSFExcelExtractor extractor = new XSSFExcelExtractor(wb)

        extractor.setFormulasNotResults(true);
        extractor.setIncludeSheetNames(false);
        String text = extractor.getText();
        log.trace "text: $text"
        //def str = "$text".replaceAll('\t', ' XXXX ')
        //log.debug "str: $str"

        int ln=1
        List<String> cols = []
        def fileData = []
        int newColIdx = 1
        text.eachLine { line ->
            log.trace "${ln}: $line"

            def vals = line.split('\t')
            if (ln == 1) {

                cols.addAll(vals)
                log.trace "cols: $cols"

            } else {

                def m = [:]
                //def vn = 0
                vals.eachWithIndex { val, vn ->
            		for (int i=0; i<=vn-cols.size(); i++) {
            			cols << "AUTO${newColIdx++}"
            		}
                    def col = cols[vn]
                    m.put(col, val)
                }

                if (m) fileData << m

            }

            ln++
        }

        //log.debug "fileData: $fileData"

        return fileData    
    }


	///////////////////////////////////////////////////////////////////////////
	// READ AN INDIVIDUAL DATA SHEET
	///////////////////////////////////////////////////////////////////////////

    @ToString(includeNames=true)
    static class ExcelRow {
        int rowNum
        List<String> rawData
        Map mappedData
    }


    /** 
     * Read an individual sheet from an Excel file.  
     *
     * Reading from an Excel is not straightforward or easy.
     * These utility methods might might help you out if:
     *    - The Excel sheet is formatted like a data table where a header
     *      row contains the field names and the remainder of the sheet
     *      contains rows of data.
     *    - There is only a single format for dates in the sheet.
     *
     * The default behavior is to be tolerant of read failures, writing warning
     * messages to the log while ignoring the value. To fail on error, see the
     * failOnError parameter.
     *
     * @param inputFile The Excel file to read.
     * @param sheetName The name of the sheet.
     * @param params.dates Optional map of data parsing params.
     * @param params.dates.sourceFormat The format (DateFormat) used for dates
     *        in the Excel file.
     * @param params.dates.outputFormat The formatter (DateFormat) used to 
     *        output dates as Strings.
     * @param params.dates.fields A list of fields that should be parsed as dates.
     * @param failOnError If there is an error reading the file, throw a 
     *        ParseException.
     * @param firstRowNum Integer The first row to read. Use this parameter to skip 
     *        one or more rows at the start of the document before the header row.
     * @param skipRowNums Set<Integer> Rownums to skip. Use this parameter to  skip
     *        rows that are buried within data rows.
     * @param skipBlankRows Boolean If true, skip over rows that contain no data.
     * @param skipNullRows Boolean If true, skip over rowNums that have a null row.
     *        readExcelSheet attempts to read the entire sheet by accessing the
     *        rows by rowNum in sequential order. There is no guarantee that there
     *        will be no skipped rowNums in an Excel sheet, even if there are no
     *        apparent empty rows.  skipNullRows should probably be the default,
     *        but it is an explicit switch for now.
     *
     */
    static List<ExcelRow> readExcelSheet(File inputFile, String sheetName, Map params = [:]) {
        assert inputFile
        assert sheetName

    	InputStream inp
    	List<ExcelRow> data
        try {
            inp = new FileInputStream(inputFile)
            data = readExcelSheet(inp, sheetName, params)
        } finally {
            if (inp) inp.close()
        }
        return data
    }


    /** */
    static List<ExcelRow> readExcelSheet(InputStream input, String sheetName, Map params = [:]) {
        assert input
        assert sheetName

        List<ExcelRow> data
        XSSFWorkbook wb
        try {
            log.info "opening workbook..."
            wb = new XSSFWorkbook(input)

            log.info "reading sheet ${sheetName}..."
            data = readSheet(wb, sheetName, params)
        } finally {
            if (wb) wb.close()
        }
        data
    }


    /** */
    static List<ExcelRow> readSheet(XSSFWorkbook wb, String sheetName, Map params = [:]) {
        assert wb
        assert sheetName

        // open the sheet
        XSSFSheet sheet = wb.getSheet(sheetName)
        assert sheet

        // get the field names from the "first row"
        int firstRowNumOfSheet = sheet.getFirstRowNum()

        int firstRowNum
        if (params.containsKey('firstRowNum')) firstRowNum = Integer.valueOf(params.firstRowNum)
        else firstRowNum = firstRowNumOfSheet
        assert firstRowNum >= 0
        assert firstRowNum >= firstRowNumOfSheet

        for (int rowNum = firstRowNumOfSheet; rowNum < firstRowNum; rowNum++) {
            XSSFRow row = sheet.getRow(rowNum)
            assert row != null
            List<String> rowVals = readRow(row)
            log.trace "skipping header row ${rowNum}: ${rowVals}"
        }

        XSSFRow firstRow = sheet.getRow(firstRowNum)
        List<String> firstRowVals = readRow(firstRow)
        log.trace "firstRowVals: $firstRowVals"

        Map idxToColName = indexMap(firstRowVals)
        log.trace "idxToColName: $idxToColName"

        Map colNameToIdx = new HashMap<String,Integer>()
        idxToColName.each { Integer k, String v ->
            colNameToIdx.put(v, k)
        }
        log.trace "colNameToIdx: $colNameToIdx"
        params.colNameToIdx = colNameToIdx

        // put the row nums to skip in a set
        Set<Integer> rowNumsToSkip = new HashSet<Integer>()
        if (params.skipRowNums) {
            rowNumsToSkip = params.skipRowNums.toSet()
        }

        // the output
        List<ExcelRow> out = new ArrayList<ExcelRow>()

        int lastRowNum = sheet.getLastRowNum()
        (firstRowNum+1../*firstRowNum+10*/lastRowNum-1).each { rowIdx ->

            XSSFRow row = sheet.getRow(rowIdx)

            if (params.skipNullRows && row == null) {
                log.info "row with rowNum ${rowIdx} is null. skipping."
                return
            }

            // not sure why poi/excel does this, but rowNum() is not the same as the row number
            // you see in an excel sheet.  the number you see on the sheet is rowNum()+1. 
            // at least, it seems that way.  there is every chance that even that apparent
            // rule will be broken.  
            // when working with an excel sheet, it's easier to be able to reference the visible
            // number, so am using that here unless/until it proves ineffective.
            int displayedRowNum = row.getRowNum()+1

            if (row != null && rowNumsToSkip.contains(displayedRowNum)) {
                log.trace "skipping row ${rowIdx}"
                return
            }

            Map<String,String> rowValsMap = new HashMap<String,String>()

            List<String> cellVals = readRow(row, params)
            log.trace "cellVals(${rowIdx}): ${cellVals}"

            cellVals.eachWithIndex { cellVal, cellValIdx ->
                String ck = idxToColName.get(cellValIdx)
                if (ck == null) {
                    if ("$cellVal".toString().trim().length() > 0) {
                        def em = "could not get column name for column index ${cellValIdx} -- ${cellVal}"
                        if (params.failOnError) {
                            throw new DateTimeParseException(em)
                        } else {
                            log.warn em
                        }
                    }
                    return
                }
                rowValsMap.put(ck, cellVal)
            }

            def datum = rowValsMap.find { k, v ->
                String.valueOf(v).trim().length() > 0
            }

            if (params.skipBlankRows && datum == null) {
                log.info "skipping blank row ${rowIdx}"
                return
            }

            // record the output
            rowValsMap.put(EXCEL.ROW_NUM.name(), String.valueOf(displayedRowNum))
            ExcelRow excelRow = new ExcelRow(
                rowNum:displayedRowNum,
                mappedData:rowValsMap,
                rawData:cellVals
            )
            out << excelRow
        }

        return out
    }


    /** */
    static List<String> readRow(XSSFRow row, Map params = [:]) {
        assert row != null

        List<String> out = new ArrayList<String>()

        Set<Integer> dateIndexes = new HashSet<Integer>()
        if (params.dates) {
            assert params.colNameToIdx
            assert params.dates.sourceFormat
            assert params.dates.outputFormat
            assert params.dates.fields

            params.dates.fields.each { fn ->
                assert params.colNameToIdx.containsKey(fn)
                dateIndexes << params.colNameToIdx.get(fn)
            }
        }

        short fcn = row.firstCellNum
        short lcn = row.lastCellNum
        (fcn..lcn-1).each { cn ->
            if (cn < 0) {
                log.warn "cell number is less than 0: $cn"
                return
            }

            XSSFCell cell = row.getCell(cn)
            if (cell == null) {
                //log.warn "null cell at index ${cn}. inserting blank value"
                out << ""
                return
            }

            String valAsString
            CellType ct = cell.getCellType()
            boolean isDateField = dateIndexes.contains(cn)

            //log.debug "cell: $cell"
            //log.debug "ct: ${ct}"
            //log.debug "isDateField: ${isDateField}"

            if (ct == CellType.NUMERIC && isDateField) {

                try {
                    Date d = cell.getDateCellValue()
                    LocalDate ld = Instant.ofEpochMilli(d.time)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    valAsString = ld.format(params.dates.outputFormat)
                } catch (IllegalArgumentException e) {
                    def em = "count not parse numeric date ${valAsString}."
                    if (params.failOnError) {
                        throw new DateTimeParseException(em, e)
                    } else {
                        valAsString = ""
                        log.warn "${em} inserting empty value. ${e.message}."
                    }
                }
            
            } else if (ct == CellType.NUMERIC && !isDateField) {

                valAsString = String.valueOf(cell.getNumericCellValue())

            } else if (isDateField) {

                String cellStr = cell.richStringCellValue.toString()
                if ("$cellStr".toString().length() > 0) {
                    try {
                        LocalDate d = LocalDate.parse(
                            cellStr,
                            params.dates.sourceFormat
                        )
                        valAsString = d.format(params.dates.outputFormat) 
                    } catch (DateTimeParseException e) {
                        def msg = "could not parse string date: ${cellStr}."
                        if (params.failOnError) {
                            log.error msg
                            throw e
                        } else {
                            valAsString = ""
                            log.warn "${msg} inserting empty value. ${e.message}"
                        }
                    }
                } else {
                    valAsString = ""
                }

            } else if (ct == CellType.FORMULA) {

                // assume the result will be numeric?
                // no perfect solution. not named POI for nothing.
                valAsString = String.valueOf(cell.getNumericCellValue())

            } else {

                valAsString = cell.richStringCellValue.toString()

            }

            out << valAsString
        }

        return out
    }


    /** */
    static Map indexMap(List<String> vals) {
        assert vals
        
        Map<String,Integer> im = new HashMap<String,Integer>()
        vals.eachWithIndex { val, valIdx ->
            if (val == null || val.trim().length() == 0) return
            im.put(valIdx, val)
        }

        im
    }



}



