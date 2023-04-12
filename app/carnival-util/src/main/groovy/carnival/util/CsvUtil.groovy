package carnival.util



import java.io.Writer
import java.io.FileWriter
import java.io.FileReader
import java.io.Reader
import java.io.StringReader

import com.opencsv.enums.CSVReaderNullFieldIndicator
import com.opencsv.ICSVParser
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVParser

import com.opencsv.CSVReaderHeaderAwareBuilder
import com.opencsv.CSVReaderHeaderAware
import com.opencsv.CSVReader

import com.opencsv.ICSVWriter
import com.opencsv.CSVWriterBuilder
import com.opencsv.CSVWriter
import com.opencsv.CSVParserWriter



/**
 * A set of static utility methods applicable to CSV data.
 */
class CsvUtil {

    /** A singleton CSV parser that can be shared */
    static final ICSVParser DEFAULT_PARSER = new CSVParserBuilder()
        .withSeparator(ICSVParser.DEFAULT_SEPARATOR)
        .withQuoteChar(ICSVParser.DEFAULT_QUOTE_CHARACTER)
        .withEscapeChar(ICSVParser.DEFAULT_ESCAPE_CHARACTER)
        //.withLineEnd(ICSVWriter.DEFAULT_LINE_END)
        .withIgnoreQuotations(false)
        .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
    .build()


    /**
     * Create and return a header aware reader for the provided text.
     * @param text The text to parse
     * @return The CSV reader
     */
    static CSVReaderHeaderAware createReaderHeaderAware(String text) {
        assert text != null
        def reader = new StringReader(text)
        createReaderHeaderAware(reader)
    }


    /**
     * Create and return a header aware CSV reader for the provided text file.
     * @param file The text file to parse
     * @return The CSV reader
     */
    static CSVReaderHeaderAware createReaderHeaderAware(File file) {
        def reader = new FileReader(file)
        new CSVReaderHeaderAwareBuilder(reader)
            .withCSVParser(DEFAULT_PARSER)
        .build()
    }


    /**
     * Create and return a header aware CSV reader for the provided Java 
     * reader.
     * @param reader A Java reader
     * @return The CSV reader
     */
    static CSVReaderHeaderAware createReaderHeaderAware(Reader reader) {
        new CSVReaderHeaderAwareBuilder(reader)
            .withCSVParser(DEFAULT_PARSER)
        .build()
    }


    /**
     * Create and return a header aware CSV writer for the provided target 
     * file.
     * @param file The target file
     * @return The CSV writer
     */
    static CSVWriter createWriterHeaderAware(File file) {
        assert file != null
        def writer = new FileWriter(file)
        createWriterHeaderAware(writer)
    }


    /**
     * Create and return a header aware CSV writer for the provided target
     * writer.
     * @param writer The target writer
     * @param The CSV writer
     */
    static CSVParserWriter createWriterHeaderAware(Writer writer) {
        new CSVWriterBuilder(writer)
            .withParser(DEFAULT_PARSER)
            .withLineEnd(ICSVWriter.DEFAULT_LINE_END)
        .build()
    }


    /**
     * Return true if the provided CSV reader has another token to read.
     * @param csvReader The CSV reader to test
     * @return True if there is another read available
     */
    static boolean hasNext(CSVReader csvReader) {
        assert csvReader != null
        return csvReader.peek() != null
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Generic read from CSV file.
     * @param filename The full path to the CSV file
     * @return A list of maps containing the CSV data
     */
    static List<Map> readFromCsvFile(String filename) {
        assert filename != null
        File df = new File(filename)
        return readFromCsvFile(df)
    }


    /**
     * Generic read from CSV file.
     * @param file The CSV file
     * @return A list of maps containing the CSV data
     */
    static List<Map> readFromCsvFile(File file) {
        assert file != null
        assert file.exists()
        def csvReader = CsvUtil.createReaderHeaderAware(file.text)
        List<Map> data = []
        while (CsvUtil.hasNext(csvReader)) {
            def rm = csvReader.readMap()
            data << rm
        }
        return data
    }


}