package carnival.core.graph.query



import java.text.SimpleDateFormat

import groovy.transform.ToString

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowReadChannel
import static groovyx.gpars.dataflow.Dataflow.task

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnore




/** */
@ToString(includeNames=true, excludes=['statusUpdateBroadcast', 'superProcess', 'allProcesses'])
class QueryProcess {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** sql log */
    static final Logger sqllog = LoggerFactory.getLogger('sql')

    /** error log */
    static final Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** carnival log */
    static final Logger log = LoggerFactory.getLogger(QueryProcess)

    /** progress update log */
    static final Logger qlog = LoggerFactory.getLogger('carnival-query-updates')


    /** */
    static QueryProcess create(String name) {
        new QueryProcess(name).start()
    }


    /** */
    static QueryProcess create(Map m) {
        assert m.name
        def qp = new QueryProcess(m.name)
        if (m.isTrackingProc) qp.isTrackingProc = m.isTrackingProc
        qp.start()
    }


    /** */
    static public String timeToCompletionAsString(QueryProcess qp) {
        if (qp.completed && qp.success) return "completed"
        else if (qp.completed && !qp.success) return "completed unsuccessful"
        else if (qp.timeEstimator.statusUpdates.size() == 0) return "not started"
        else if (qp.timeEstimator.timeToCompletion) return "${qp.timeEstimator.timeToCompletionAsString}"
        else return "unknown"
    }


    /** */
    static public void logTimeToCompletion(QueryProcess qp) {
        log.trace "logTimeToCompletion qp:${qp.name}"

        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)

        pw.println ""
        logTimeToCompletion(qp, pw, 0)

        def str = sw.toString()
        qlog.info(str)
    }


    /** */
    static public void logTimeToCompletion(QueryProcess qp, PrintWriter pw, int indent) {
        // print indent
        def istr = ""
        (0..indent).each { istr += "    " }
        pw.print istr

        // all sub processes
        def subs = qp.subProcesses

        // special case the fully complete situation
        /*if (!subs.find({!it.completed})) pw.println "${qp.name}: completed"
        else pw.println "${qp.name}: ${timeToCompletionAsString(qp)}"*/
        
        // message for this process
        pw.println "${qp.name}: ${timeToCompletionAsString(qp)}"

        // increase the indend and print sub proc times
        indent++
        subs.each { logTimeToCompletion(it, pw, indent) }
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    String name

    boolean completed = false

    boolean success = false

    boolean isTrackingProc = false

    @JsonIgnore
    DataflowBroadcast statusUpdateBroadcast

    TimeEstimator timeEstimator

    List<QueryProcess> subProcesses = []

    @JsonIgnore
    QueryProcess superProcess

    Throwable exception


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public QueryProcess(String name) {
        this.name = name
        this.statusUpdateBroadcast = new DataflowBroadcast()
        this.timeEstimator = new TimeEstimator(this)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public QueryProcess start() {
        this.timeEstimator.start()
        this
    }


    /** */
    public void stop() {
        this.completed = true
        this.success = true

        log.trace "queryProcess.stop() name:${this.name} completed:${this.completed} success:${this.success}"

        this.timeEstimator.stop()
        if (superProcess) superProcess.notifySubProcCompletion()
    }


    /** */
    public void fail() {
        this.timeEstimator.stop()
        this.completed = true
        this.success = false
        if (superProcess) superProcess.notifySubProcCompletion()
    }


    /** */
    public void fail(Throwable e) {
        this.timeEstimator.stop()
        this.completed = true
        this.success = false
        this.exception = e
        if (superProcess) superProcess.notifySubProcCompletion()
    }


    /** */
    public DataflowReadChannel createStatusUpdateReadChannel() {
        return statusUpdateBroadcast.createReadChannel()
    }


    /** */
    public void statusUpdate(int current, int total) {
        log.trace "statusUpdate $name $current $total"
        def su = new StatusUpdate(queryProcess:this, current:current, total:total)
        statusUpdateBroadcast << su
        if (superProcess) superProcess.notifySubProcStatusUpdate(su)
    }


    /** */
    public String getTimeToCompletionAsString() {
        timeToCompletionAsString(this)
    }


    ///////////////////////////////////////////////////////////////////////////
    // SUB PROCESS METHODS
    ///////////////////////////////////////////////////////////////////////////
    
    /** */
    @JsonIgnore
    List<QueryProcess> getAllProcesses() {
        List<QueryProcess> procs = []
        recursiveGetAllProcesses(procs, this)
        return procs
    }


    /** */
    @JsonIgnore
    List<QueryProcess> getAllSubProcesses() {
        List<QueryProcess> procs = []
        recursiveGetAllProcesses(procs, this)
        assert procs.remove(this) : "this not found in proc list this:$this procs:$procs"
        procs
    }


    /** */
    @JsonIgnore
    private void recursiveGetAllProcesses(List<QueryProcess> procs, QueryProcess proc) {
        procs.add(proc)
        def subProcs = proc.subProcesses
        for (sp in subProcs) {
            recursiveGetAllProcesses(procs, sp)
        }
    }


    /** */
    public StatusUpdate getMostRecentStatusUpdate() {
        def aps = getAllProcesses()
        Set<StatusUpdate> ups = new HashSet<StatusUpdate>()
        aps.each { p -> ups.addAll(p.timeEstimator.statusUpdates) }
        ups = ups.toSorted { a, b -> a.dateCreated <=> b.dateCreated }
        if (ups.size() == 0) return null
        else return ups.last()
    }


    /** */
    public QueryProcess createSubProcess(String name) {
        def qp = create(name)
        qp.superProcess = this
        this.subProcesses << qp
        return qp
    }


    /** */
    private void notifySubProcStatusUpdate(StatusUpdate su) {
        log.trace "notifySubProcStatusUpdate su:$su"
        statusUpdateBroadcast << su
        if (superProcess) superProcess.notifySubProcStatusUpdate(su)
    }


    /** */
    private void notifySubProcCompletion() {
        log.trace "notifySubProcCompletion"

        // there is special behavior only if this process is a tracking
        // process
        if (!isTrackingProc) return

        boolean allSubProcsComplete = true
        boolean allSubProcsSuccessful = true
        for (sub in allSubProcesses) {
            if (!sub.completed) allSubProcsComplete = false
            if (!sub.success) allSubProcsSuccessful = false
        }
        if (allSubProcsComplete && allSubProcsSuccessful) this.stop()
        if (allSubProcsComplete && !allSubProcsSuccessful) this.fail()
    }


    ///////////////////////////////////////////////////////////////////////////
    // MONITOR THREAD
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public void startMonitorThread(Map args = [:]) {
        log.trace "startMonitorThread args:$args"

        int sleepIntervalMillis = args.sleepIntervalMillis ?: 10000
        boolean logTtc = args.logTimeToCompletion ?: true
        boolean autoStopOnCompletion = args.autoStopOnCompletion ?: true

        log.trace "launching monitor thread for query process ${this.name}"
        task {
            def procs = this.getAllProcesses()
            log.trace "monitor thread (${this.name}) completed procs: ${procs*.completed}"
            
            boolean alldone = false
            while (!alldone) {
                try {
                    sleep sleepIntervalMillis
                    if (logTtc) logTimeToCompletion(this)
                    
                    def f = procs.find({ !it.completed })
                    log.trace "monitor thread name:${this.name} procs:${procs*.name} f:${f?.name} ${f?.completed}"
                    
                    alldone = (f==null)
                } catch (Exception e) {
                    elog.error("monitor thread (${this.name})", e)
                    log.error("monitor thread (${this.name})", e)
                }
            }

            if (autoStopOnCompletion) this.stop()
        }
        log.trace "monitor thread (${this.name}) has launched."
    }


}