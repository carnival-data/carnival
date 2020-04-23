package carnival.core.graph.query



import java.text.SimpleDateFormat

import groovy.transform.ToString

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnore



/** */
@ToString(includeNames=true, excludes=['queryProcess'])
class StatusUpdate {

    ///////////////////////////////////////////////////////////////////////////
    // LOGGING
    ///////////////////////////////////////////////////////////////////////////

    /** error log */
    static final Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** carnival log */
    static final Logger log = LoggerFactory.getLogger(StatusUpdate)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    @JsonIgnore
    QueryProcess queryProcess

    int current = 0

    int total = 0

    Date dateCreated = new Date()


    /** */
    public boolean isValid() {
        return (queryProcess != null && current >= 0 && total > 0)
    }

}