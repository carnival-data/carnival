package carnival.util


import groovy.util.logging.Slf4j





/**
 * IterativeCsvWriter is designed to work with iterators that operate over query
 * result sets.  We currently support only java.sql.ResultSet... which may be
 * all that we need ever to support.  Files written with this writer should work
 * with Vine.readFromCsvFile().
 *
 */
@Slf4j
class IterativeCsvWriter {


    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Factory: Create an IterativeCsvWriter for a provided file.
     * 
     * @param file The target CSV file.
     * @return A new IterativeCsvWriter.
     *
     */
    static public IterativeCsvWriter create(File file) {
        assert file

        IterativeCsvWriter icw = new IterativeCsvWriter()
        icw.file = file
        icw.pw = new PrintWriter(file)

        return icw
    }


    /**
     * Factory: Create an IterativeCsvWriter for a file in the current
     * directory with the given file name.
     *
     * @param fileName The name of the CSV file.
     * @return A new IterativeCsvWriter.
     *
     */
    static public IterativeCsvWriter create(String fileName) {
        assert fileName

        File file = new File(fileName)
        return create(file)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the CSV file */
    File file

    /** the current line number */
    int lineNum = 0

    /** the field names of the CSV file */
    String[] keys

    /** the writer used to write to file */
    PrintWriter pw


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Write a single record to the CSV file.  If this would be the first 
     * record written, write the header row of field names.
     *
     * @param row - the data wrapped in a java.sql.ResultSet interface
     *
     */
    public void write(java.sql.ResultSet row) {
        if (lineNum++ == 0) writeKeys(row)
        writeValues(row)
    }


    /**
     * Write the field names (keys) to the output file.  Expected to be called
     * only once to write the first line of the file.
     *
     * @param row - the data wrapped in a java.sql.ResultSet interface
     *
     */
    public void writeKeys(java.sql.ResultSet row) {
        def metaData = row.getMetaData()
        log.trace "IterativeCsvWriter.writeKeys metaData: $metaData"

        def numCols = metaData.getColumnCount()

        def kl = []
        for (int i=1; i<=numCols; i++) {
            kl << metaData.getColumnName(i)
        }
        keys = kl.toArray()
        //log.debug "keys: $keys"

        writeLine keys.collect({ "${it.toLowerCase()}" }).join(",")
    }


    /**
     * Write the values of the provided record to the CSV file.
     *
     * @param row - the data wrapped in a java.sql.ResultSet interface
     *
     */
    public void writeValues(java.sql.ResultSet row) {
        def orderedValues = []
        keys.each { 
            orderedValues << row[it] 
        }
        orderedValues = orderedValues.collect { 
            (it != null) ? "$it" : "" 
        }
        writeLine orderedValues.join(",")
    }


    /**
     * Write a line of text to the CSV file.
     *
     * @param line - A line of the CSV with the commas already in it
     *
     */
    public void writeLine(String line) {
        try {
            pw.println(line)
        } catch (Exception e) {
            def msg = "IterativeCsvWriter.writeLine exception!"
            def fname = "${file.getCanonicalPath()}-stacktrace.txt"
            log.error msg
            def epw = new PrintWriter(new File(fname))
            try {
                e.printStackTrace(epw)
            } finally {
                if (epw) epw.close()
            }
            throw new Exception("$msg See: $fname")
        }
    }


    /**
     * Wrapper method for the print writers close() method.
     *
     */
    public void close() {
        if (pw) pw.close()
    }

}



