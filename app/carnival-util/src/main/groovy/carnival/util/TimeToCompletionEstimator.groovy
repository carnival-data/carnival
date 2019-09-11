package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

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



/**
 * TimeToCompletionEstimator keeps track up process updates and provided
 * estimated time to completion.
 *
 */
@ToString(includeNames=true)
class TimeToCompletionEstimator {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC 
    ///////////////////////////////////////////////////////////////////////////

    /** carnival log */
    static final Logger log = LoggerFactory.getLogger('carnival')

    /** log for completion updates */
    static final Logger qlog = LoggerFactory.getLogger('carnival-query-updates')


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the name of this object */
    String name = 'TimeToCompletionEstimator'

    /** the processes tracked by this estimator */
    Map<String,Map> procs = new HashMap<String,Map>()

    /** an synchronous update queue used to receive updates */
    DataflowReadChannel updateQueue

    /** if true, this estimator is cancelled */
    boolean isCanceled = false


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////

    /** for testing */
    protected TimeToCompletionEstimator() { }

    /** */
    protected TimeToCompletionEstimator(String name) {
    	this.name = name
    }

    /** */
    public TimeToCompletionEstimator(DataflowReadChannel updateQueue) {
    	this.updateQueue = updateQueue
    }

    /** */
    public TimeToCompletionEstimator(DataflowWriteChannel updateQueue) {
    	this.updateQueue = updateQueue.createReadChannel()
    }

    /** */
    public TimeToCompletionEstimator(String name, DataflowReadChannel updateQueue) {
    	this(name)
    	this.updateQueue = updateQueue
    }

    /** */
    public TimeToCompletionEstimator(String name, DataflowWriteChannel updateQueue) {
    	this(name)
    	this.updateQueue = updateQueue.createReadChannel()
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Start this time to completion estimator, ie set the start time.
     *
     */
    public TimeToCompletionEstimator start() {
    	task { monitorQueue() }
    	this
    }


    /** 
     * Stop this time estimator by setting cancelled to true.
     *
     */
    public void stop() {
    	this.isCanceled = true
    }


    /** 
     * Get the estimated time to completion.
     *
     * @return The time to completion in milliseconds.
     *
     */
    public long getTimeToCompletion() {
    	def subTimes = procs.values()*.timeToCompletion
    	long time = 0
    	subTimes.each { if (it > time) time = it }
    	return time
    }


    /** 
     * Get the estimated time to completion as a readable String.
     *
     * @return A String representation of the time to completion.
     *
     */
    public String getTimeToCompletionAsString() {
    	long timeMillis = getTimeToCompletion()
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

    	//log.debug "td: ${days} ${hours} ${minutes} ${seconds} ${millis}"
    	if (days) return "${days} days, ${hours} hours"
    	else if (hours) return "${hours} hours, ${minutes} minutes"
    	else if (minutes) return "${minutes} minutes"
    	else return "${seconds} seconds"
    }


    protected void monitorQueue() {
    	while (!isCanceled) {
    		def val = updateQueue.val
    		qlog.trace "(${this.name}) val: $val"

    		handleUpdate(val)
    		qlog.info "(${this.name}) time to completion : ${timeToCompletionAsString}"
    	}
    }



    /**
     * Handle a progress update.
     *
     * @param update.name Name of the update.
     * @param update.current The current step in the process.
     * @param update.total The total steps in the process.
     *
     */
    @Synchronized
    public void handleUpdate(Map update) {
    	//log.debug "TimeToCompletionEstimator handleUpdate update:$update procs:$procs"
    	if (update.name == null || update.current == null || update.total == null) return

		def now = new Date().time

    	def name = update.name
    	def proc = procs.get(name)
    	if (!proc) {
    		proc = [
    			startTime:now,
    			timeToCompletion:0
    		]
    		proc.putAll(update)
    		procs.put(name, proc)
    		return
    	}
    	
    	proc.putAll(update)
    	//log.debug "TimeToCompletionEstimator proc: $proc"
    	assert proc.current == update.current

    	def runTime = now - proc.startTime

    	if (runTime <= 0 || proc.current == 0) {
    		proc.timeToCompletion = 0
    		return
    	}

    	def timePerItem = runTime / proc.current
    	def itemsRemaining = proc.total - proc.current
    	proc.timeToCompletion = itemsRemaining * timePerItem
    }

}



