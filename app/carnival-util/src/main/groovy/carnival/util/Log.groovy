package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import groovy.sql.GroovyRowResult



/**
 * A utility class to help with logging.
 *
 */
class Log {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC METODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Log the progress of a multi-step (chunked) process.
     *
     * @param log The Logger to which to write the statement.
     * @param logMethod The log method to use, trace, debug, info, etc.
     * @param msg The message prefix, excluding any counts or completion pcts.
     * @param total The total number of steps.
     * @param current The current step.
     *
     */
    static void progress(Logger log, String logMethod, String msg, Integer total, Integer current) {
        if (toal > 0) {
            log."$logMethod"("$msg $current of $total (${ Math.round(Math.floor((current*100)/total)) }%)")
        }
        else {
            log."$logMethod"("$msg $current of $total (-%)")   
        }

    }


    /**
     * Convenience method to call the more general progress(...) using the log
     * method 'info'.
     *
     * @see progress(Logger log, String logMethod, String msg, Integer total, Integer current)
     *
     */
    static void progress(Logger log, String msg, Integer total, Integer current) {
    	progress(log, 'info', msg, total, current)
    }

}



