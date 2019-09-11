package carnival.util



import static com.xlson.groovycsv.CsvParser.parseCsv
import static java.lang.System.err



/**
 * Command line tool that accepts a list of elements and returns a
 * unified SQL OR clause with chunked clauses.
 *
 * gradle whereClauseChunker -Pfieldname=EMPI -Pfile=unique_patient_pmbbPdsIdentifiers.csv -Pchunksize=1000
 *
 */
class WhereClauseChunker {
	
    static void main(String[] args) {
    	if (!(args && args.length >= 2)) {
    		usage()
    		System.exit(1)
    	}

    	// args
    	def fieldName = args[0]
    	def filePath = args[1]
    	def chunkSize = args[2] ? Integer.valueOf(args[2]) : 1000

		// load data file
        File dataFile = new File(args[1])
      	def dataParsed = parseCsv(dataFile.text)
      	def rows = []
      	rows.addAll(dataParsed)

      	// get where clause
      	def whereClause = "where " + SqlUtils.chunkedWhereClause(fieldName, rows*."$fieldName", chunkSize)

      	File out = new File("chunkedWhereClause.sql")
      	out.text = whereClause
   	}


   	static void usage() {
   		err.println "USAGE:"
   		err.println "groovy PmbbPdsExample.groovy <field-name> <csv-data-file> <chunksize>"
   	}

}





