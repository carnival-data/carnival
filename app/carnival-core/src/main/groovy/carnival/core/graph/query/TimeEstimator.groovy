package carnival.core.graph.query



import groovy.util.logging.Slf4j

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.time.TimeDuration
import groovy.time.TimeCategory

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import groovy.transform.ToString
import groovy.transform.Synchronized
import static groovyx.gpars.dataflow.Dataflow.task
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnore



/** */
@ToString(includeNames=true, excludes=['statusUpdateChannel', 'queryProcess', 'timeToCompletion', 'timeToCompletionAsString', 'statusUpdates'])
@Slf4j
class TimeEstimator {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the parent query process for this time estimator */
    @JsonIgnore
    QueryProcess queryProcess

    /** */
    Long startTime

    /** */
    Long timeToCompletionValue

    /** */
    @JsonIgnore
    List<StatusUpdate> statusUpdates = new ArrayList<StatusUpdate>()

    /** */
    @JsonIgnore
    DataflowReadChannel statusUpdateChannel

    /** */
    boolean isCanceled = false


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////

    /** for testing */
    protected TimeEstimator() { }


    /** */
    public TimeEstimator(QueryProcess qp) {
        assert qp
        this.queryProcess = qp
        this.statusUpdateChannel = qp.createStatusUpdateReadChannel()
    }



    ///////////////////////////////////////////////////////////////////////////
    // CONTROL METHODS
    ///////////////////////////////////////////////////////////////////////////

    public TimeEstimator start() {
    	task { monitorQueue() }
    	this
    }


    public void stop() {
    	this.isCanceled = true
    }


    ///////////////////////////////////////////////////////////////////////////
    // UPDATE MONITOR
    ///////////////////////////////////////////////////////////////////////////

    protected void monitorQueue() {
        log.trace "TimeEstimator ${queryProcess.name} monitorQueue..."

        while (!isCanceled) {
            def val = statusUpdateChannel.val

            try {
                handleUpdate(val)
            } catch (Throwable t) {
                log.error "handleUpdate val:$val", t
            }
        }
    }


    public void handleUpdate(Map args) {
        log.trace "TimeEstimator ${queryProcess.name} handleUpdate(Map) : ($args)"

        def su = new StatusUpdate(args)
        handleUpdate(su)
    }


    public void handleUpdate(QueryProcess qp, int current, int total) {
        log.trace "TimeEstimator ${queryProcess.name} handleUpdate(QueryProcess, current, total) : (${queryProcess.name}, $current, $total)"

        def su = new StatusUpdate(
            queryProcess:qp,
            current:current,
            total:total
        )

        handleUpdate(su)
    }


    @Synchronized
    public void handleUpdate(StatusUpdate update) {
        log.trace "TimeEstimator ${queryProcess.name} handleUpdate(StatusUpdate) : ($update)"

        // if the update is not valid, log a warning and skip
        if (!update.isValid()) {
            log.warn "TimeEstimator invalid update: $update"
            return
        }

        // check that the status update is relevant to this estimator
        if (update.queryProcess != this.queryProcess) {
            log.trace "TimeEstimator ${queryProcess.name} ignoring irrelevant update: $update"
            return
        }

        // get the curren time
        long now = new Date().time

        // record this update
        statusUpdates << update

        // if we have no previous updates, then just record that we have received
        // an update and set the start time.  we cannot perfom a time estimation
        // because the start time was just set this instant
        if (statusUpdates.size() == 1) {
            this.startTime = new Long(now)
            return
        }

        // if the update shows no items completed, just record the update and
        // move on
        if (update.current == 0) return

        // get the start time as a long
        assert this.startTime != null
        long st = this.startTime.longValue()

        // if now is less than the start time, which should never happen, log
        // the warning and move on
        if (now < st) {
            log.warn "now < st now:$now st:$st"
            return
        }

        // compute the time to completion estimation
        def runTime = now - this.startTime
        def timePerItem = runTime / update.current
        def itemsRemaining = update.total - update.current
        this.timeToCompletionValue = itemsRemaining * timePerItem
    }


    /**
     *
     *
     */
    public StatusUpdate getMostRecentlyAddedStatusUpdate() {
        if (statusUpdates.size() == 0) return null
        else return statusUpdates.last()
    }


    ///////////////////////////////////////////////////////////////////////////
    // TIME TO COMPLETION METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    public Long getTimeToCompletion() {
        // get all sub procs
        def subs = queryProcess.allSubProcesses

        // if we have a sub-process with no time to completion estimate, then
        // we return null
        def incompleteNoEstimate = subs.find { 
            !it.completed && it.timeEstimator?.timeToCompletion == null 
        }
        if (incompleteNoEstimate) return null

        // return the max time estimate
        Long ttc = null

        if (this.timeToCompletionValue != null) ttc = this.timeToCompletionValue

        subs.each { qp ->
            def qpttc = qp.timeEstimator?.timeToCompletion
            if (qpttc != null) {
                if (ttc == null) ttc = qpttc
                else if (qpttc > ttc) ttc = qpttc
            }
        }

    	return ttc
    }


    /**
     *
     *
     */
    public String getTimeToCompletionAsString() {
        Long timeLong = getTimeToCompletion()
        if (timeLong == null) return null
    	
        long timeMillis = timeLong.longValue()
    	long time = new Long(timeMillis).intValue()
    	
    	int days = Math.floor(time / (24 * 60 * 60 * 1000))
    	if (days > 0) time -= days * 24 * 60 * 60 * 1000

    	int hours = Math.floor(time / (60 * 60 * 1000))
    	if (hours > 0) time -= hours * 60 * 60 * 1000

    	int minutes = Math.floor(time / (60 * 1000))
    	if (minutes > 0) time -= minutes * 60 * 1000

    	int seconds = Math.floor(time / 1000)
    	if (seconds > 0) time -= seconds * 1000

    	int millis = time

    	if (days) return "${days} days, ${hours} hours"
    	else if (hours) return "${hours} hours, ${minutes} minutes"
    	else if (minutes) return "${minutes} minutes"
    	else return "${seconds} seconds"
    }







}



