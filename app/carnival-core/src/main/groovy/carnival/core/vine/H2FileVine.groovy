package carnival.core.vine



import static com.xlson.groovycsv.CsvParser.parseCsv
import static java.lang.System.err

import java.sql.PreparedStatement
import java.util.concurrent.atomic.AtomicInteger
import java.text.SimpleDateFormat

import groovy.transform.InheritConstructors
import groovy.sql.Sql

import org.ho.yaml.Yaml
import groovy.sql.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.http.impl.client.DefaultHttpClient

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import carnival.core.*
import carnival.core.config.RedcapConfig
import carnival.core.vine.RedcapVine
import carnival.core.vine.CachingVine
import carnival.core.vine.CachingVine.CacheMode
import carnival.core.vine.FileVine
import carnival.core.vine.VineMethod
import carnival.core.vine.GenericDataTableVineMethod
import carnival.core.vine.MappedDataTableVineMethod
import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTable
import carnival.util.SqlUtils



/**
 * H2FileVine is a superclass whose sub-classes are able to read/write to an H2
 * database.
 *
 */
@InheritConstructors
abstract class H2FileVine extends FileVine implements RelationalVine {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    static String toTableFieldName(String fieldName) {
        assert fieldName != null
        fieldName
            .replaceAll(" ", "")
            .replaceAll("-", "_")
            .replaceAll("\\.", "_")
            .replaceAll("\\)", "_")
            .replaceAll("\\(", "_")
            .replaceAll("_+\$", "")
            .toUpperCase()
    }



    /**
     * Drop the table if it exists.
     *
     */
    static void dropTableIfExists(Sql sql, String tableName) {
        log.trace "dropTableIfExists sql:$sql tableName$tableName"

        if (!tableExists(sql, tableName)) return
        def q = "drop table ${tableName}".toString()
        sqllog.info "$q"
        sql.execute(q) 
    }


    /**
     * Return true IFF the table exists.
     *
     */
    static boolean tableExists(Sql sql, String tableName) {
        log.trace "tableExists $sql: $sql"

        boolean te = false

        try {
            def q = "select count(*) from ${tableName}".toString()
            sqllog.info "$q"

            // this will throw an exception if the table does not exist
            sql.eachRow(q) { row -> /* do nothing */ }
            te = true
        } catch (org.h2.jdbc.JdbcSQLException e) {
            log.trace "${tableName} does not exist or is empty"
        }

        return te
    }


    /**
     *
     *
     */
    static void withSql(Sql sql, Closure closure) {
        try {
            closure(sql)
        } finally {
            sql.close()
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Default construct defaults to cache mode of optional, not ignore, which
     * is the default of file vines.  The default cache mode for all fines is
     * optional, so this brings H2 vines back to where they were previously to
     * the change in default in file vines.
     *
     */
    public H2FileVine() {
        this.cacheMode = CacheMode.OPTIONAL
    }


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the name of the H2 table which this vine will create.
     *
     */
    abstract String getTableName()


    /**
     * Return a table create statement.
     *
     */
    abstract String getCreateTableSql()


    /**
     * Return the list of columns that should be indexed
     *
     */
    Collection<String> getIndexedColumnNames()
    {
        return []
    }


    /**
     * Return an insert statemenet for a single record that pairs with the 
     * closure returned by getInsertClosure().
     *
     */
    abstract String getInsertSql()


    /**
     * Return a closure that sets the values of a prepared statment generated
     * using the SQL from getInsertSql().  The closure should accept a single
     * parameter, which is a single row of data as a Map.
     *
     */
    abstract Closure getInsertClosure(PreparedStatement ps)


    /**
     * Return a generic data table with all data from the file.
     *
     */
    // abstract GenericDataTable allDataFromFile()


    /**
     * Return an iterator of GenericDataTable objects that when exhausted
     * covers all the data from the file.  Originally, there was an abstract
     * method allDataFromFile().  However, this turned out to cause memory
     * issues with large files.  The iterator approach allows the data to be
     * broken into manageable chunks.
     *
     */
    abstract Iterator<GenericDataTable> allDataFromFileIterator()



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the log interval used when operating over all data */
    int logInterval = 10000



    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get a groovy Sql object connected to a populated H2 database respecting
     * the provided cache mode.
     *
     */
    Sql getSql(CachingVine.CacheMode cm) {
        log.info "H2FileVine.getSql(CacheMode cm) cm: $cm"
        H2.getServer()
        def sql = H2.getSql()
        log.trace "sql: $sql"

        def tableExists = tableExists(sql)
        log.trace "tableExists: $tableExists"

        if (cm == CachingVine.CacheMode.REQUIRED) {
            if (!tableExists) {
                sql.close()
                throw new RuntimeException("cache required, but table $tableName does not exist")
            }
        } else if (cm == CachingVine.CacheMode.OPTIONAL) {
            if (!tableExists) loadTableData(sql)
        } else if (cm == CachingVine.CacheMode.IGNORE) {
            loadTableData(sql)
        } else {
            throw new RuntimeException("unrecognized cache mode: $cm")
        }
        addIndexes(sql)
        return sql
    }

    /** */
    void addIndexes(Sql sql)
    {
        getIndexedColumnNames().each {
            column ->
                def columnName = "IDX_" + column
                def str = "CREATE INDEX IF NOT EXISTS " + columnName + " ON " + tableName + "(" + column + ")"
                sql.executeUpdate(str)
        }
    }

    /**
     * Return a Groovy Sql object to use in a vine method.
     *
     */
    Sql getSql() { 
        log.trace "H2FileVine.getSql()"

        def cacheMode = this.getCacheMode()
        log.trace "cacheMode: $cacheMode"
        
        getSql(cacheMode) 
    }


    /**
     * Convenience method to run a SQL operation in a try/catch block.
     *
     */
    void withSql(Closure closure) {
        log.info "H2FileVine.withSql(closure)"

        def sql = getSql()
        log.info "H2FileVine.withSql(closure) sql:$sql"

        try {
            closure(sql)
        } finally {
            log.trace "H2FileVine.withSql(closure) sql:$sql closing sql connection..."
            sql.close()
            log.trace "H2FileVine.withSql(closure) sql:$sql done."
        }
    }


    /**
     * Loads the table data using the abstract implementation.
     *
     */
    void loadTableData(Sql optSql = null) {
        log.info "loadTableData(Sql optSql = null) optSql:$optSql"

        def cl = { sql ->
            // drop the table if it exists
            dropTableIfExists(sql)

            // create the table
            def q = getCreateTableSql()
            sqllog.info(q)
            sql.execute(q)

            // get a database connection
            // we do not close this connection when we are finished with it
            // as we cannot guarantee that it is no longer needed
            // see the Javadoc for groovy.sql.Sql
            def conn = sql.getConnection()
            // set up a prepared statement for the insert sql
            def isql = getInsertSql()
            def ps = conn.prepareStatement(isql)

            // set up the insert closure
            def iscl = getInsertClosure(ps)
            // run the insert closure for each row of data
            def allDataIterator = allDataFromFileIterator()
            allDataIterator.each { DataTable mdt ->
                def total = mdt.data.size()
                if (total < logInterval) logInterval = total / 5
                def idx = 0
                mdt.dataIterator().each { row ->
                    if (idx++ % logInterval == 0) log.info "H2FileVine.loadTableData ${idx-1} of $total (${Math.round((idx-1)/total*100)}%)"
                    iscl(row)
                }
            }
        }
        if (optSql) cl(optSql)
        else withSql(cl)
    }


    /**
     * Drop the table if it exists.
     *
     */
    void dropTableIfExists(Sql optSql = null) {
        log.trace "dropTableIfExists optSql:$optSql"
        if (!optSql) optSql = H2.getSql()
        dropTableIfExists(optSql, tableName)
    }


    /**
     * Return true IFF the table exists.
     *
     */
    boolean tableExists(Sql optSql = null) {
        log.trace "tableExists $optSql: $optSql"
        if (!optSql) optSql = H2.getSql()
        tableExists(optSql, tableName)
    }


}