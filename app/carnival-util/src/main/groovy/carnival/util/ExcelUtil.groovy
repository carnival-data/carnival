package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.extractor.XSSFExcelExtractor



/**
 * Utility class to read/write XML based Microsoft Excel files.
 *
 */
class ExcelUtil {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')


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

}



