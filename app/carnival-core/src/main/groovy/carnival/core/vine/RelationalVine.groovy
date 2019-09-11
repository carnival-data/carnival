package carnival.core.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import groovy.sql.*

import carnival.core.config.DatabaseConfig



/**
 *
 */
interface RelationalVine {

	public void withSql(Closure c)

}



