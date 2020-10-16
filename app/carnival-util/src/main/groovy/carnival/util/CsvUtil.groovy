package carnival.util



import java.io.FileWriter
import java.io.Writer
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



class CsvUtil {

    static final ICSVParser DEFAULT_PARSER = new CSVParserBuilder()
        .withSeparator(ICSVParser.DEFAULT_SEPARATOR)
        .withQuoteChar(ICSVParser.DEFAULT_QUOTE_CHARACTER)
        .withEscapeChar(ICSVParser.DEFAULT_ESCAPE_CHARACTER)
        //.withLineEnd(ICSVWriter.DEFAULT_LINE_END)
        .withIgnoreQuotations(false)
        .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_SEPARATORS)
    .build()


    static CSVReaderHeaderAware createReaderHeaderAware(String text) {
        assert text != null
        def reader = new StringReader(text)
        createReaderHeaderAware(reader)
    }


    static CSVReaderHeaderAware createReaderHeaderAware(Reader reader) {
        new CSVReaderHeaderAwareBuilder(reader)
            .withCSVParser(DEFAULT_PARSER)
        .build()
    }


    static CSVWriter createWriterHeaderAware(File file) {
        assert file != null
        def writer = new FileWriter(file)
        createWriterHeaderAware(writer)
    }


    static CSVParserWriter createWriterHeaderAware(Writer writer) {
        new CSVWriterBuilder(writer)
            .withParser(DEFAULT_PARSER)
            .withLineEnd(ICSVWriter.DEFAULT_LINE_END)
        .build()
    }


    static boolean hasNext(CSVReader csvReader) {
        assert csvReader != null
        return csvReader.peek() != null
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE METHODS
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Generic read from CSV file.
     *
     */
    static List<Map> readFromCsvFile(String filename) {
        File df = new File(filename)
        return readFromCsvFile(df)
    }


    /**
     * Generic read from CSV file.
     *
     */
    static List<Map> readFromCsvFile(File file) {
        def csvReader = CsvUtil.createReaderHeaderAware(file.text)
        List<Map> data = []
        while (CsvUtil.hasNext(csvReader)) {
            def rm = csvReader.readMap()
            data << rm
        }
        return data
    }


}