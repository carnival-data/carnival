package carnival.core.graph



import java.lang.reflect.Field

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex

import carnival.core.util.CoreUtil
import carnival.graph.VertexDefTrait
import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait



/** */
abstract class Reaper {
	
	///////////////////////////////////////////////////////////////////////////
	// STATIC FIELDS
	///////////////////////////////////////////////////////////////////////////
	static Logger sqllog = LoggerFactory.getLogger('sql')
	static Logger elog = LoggerFactory.getLogger('db-entity-report')
	static Logger log = LoggerFactory.getLogger(Reaper)

    /** */
    static enum VX implements VertexDefTrait {
        REAPER_PROCESS_CLASS
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * TODO: remove this?
     * temporary till MappedDataTable update
     *  
     */
    static protected String graphSafeString(def value) {
        return value?.toString() ?: ""
    }


    ///////////////////////////////////////////////////////////////////////////
    // UTLITY
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static void setProps(Vertex targetV, Map rec, Collection<PropertyDefTrait> propDefs) {
        propDefs.each { pdef -> setProp(targetV, rec, pdef) }
    }

    /** */
    static void setProp(Vertex targetV, Map rec, PropertyDefTrait pdef) {
        def pv = rec.get(pdef.name())
        if (pv && !targetV.property(pdef.label).isPresent()) targetV.property(pdef.label, pv)
    }

    /** */
    static void setProps(Vertex targetV, Vertex recV, Collection<PropertyDefTrait> propDefs) {
        propDefs.each { pdef -> setProp(targetV, recV, pdef) }
    }

    /** */
    static void setProp(Vertex targetV, Vertex recV, PropertyDefTrait pdef) {
        def pv = pdef.valueOf(recV)
        if (pv && !pdef.of(targetV).isPresent()) pdef.set(targetV, pv)
    }

    /** */
    static void setProps(Vertex targetV, Vertex recV, Map<PropertyDefTrait,PropertyDefTrait> propDefs) {
        propDefs.each { recPdef, targetPdef ->
            setProp(targetV, targetPdef, recV, recPdef)
        }
    }

    /** */
    static void setProp(Vertex targetV, PropertyDefTrait targetPdef, Vertex recV, PropertyDefTrait recPdef) {
        def pv = recPdef.valueOf(recV)
        if (pv == null) return
        if (!targetPdef.of(targetV).isPresent()) targetPdef.set(targetV, pv)
    }

    /** */
    static Map<PropertyDefTrait,PropertyDefTrait> pxMap(PropertyDefTrait... defArgs) {
        List<PropertyDefTrait> defs = defArgs.toList()
        def pairs = defs.collate(2)
        Map<PropertyDefTrait,PropertyDefTrait> m = new HashMap<PropertyDefTrait,PropertyDefTrait>()
        pairs.each { p ->
            m.put(p[0], p[1])
        }
        m
    }

    /** */
    static Map<PropertyDefTrait,PropertyDefTrait> pxMap(Class enumFrom, Class enumTo) {
        Map<PropertyDefTrait,PropertyDefTrait> m = new HashMap<PropertyDefTrait,PropertyDefTrait>()
        EnumSet.allOf(enumFrom).each { f ->
            t = Enum.valueOf(enumTo, f.name())
            m.put(f,t)
        }
        m
    }



    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    abstract protected Graph getGraph()


    /** */
    abstract protected GraphTraversalSource traversal()



	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

    /** */
    public Map ensure(String methodName, Map methodArgs = [:]) {
        optionallyRunSingletonProcess(methodName, methodArgs)
    }


    /** */
    public Map ensure(Class reaperMethodClass, Map methodArgs = [:]) {
        optionallyRunSingletonProcess(reaperMethodClass, methodArgs)
    }


    /**  */
    public Map ensure(ReaperMethod reaperMethodInstance, Map methodArgs = [:]) {
        optionallyRunSingletonProcess(reaperMethodInstance, methodArgs)       
    } 


    /** */
    public Map optionallyRunSingletonProcess(String methodName, Map methodArgs = [:]) {
        assert methodName

        def rmi = createReaperMethodInstance(methodName)
        if (rmi == null) throw new RuntimeException("Could not find reaper method '${methodName}' of ${this.class.name}")
        optionallyRunSingletonProcess(rmi, methodArgs)
    }


    /** */
    public Map optionallyRunSingletonProcess(Class reaperMethodClass, Map methodArgs = [:]) {
        assert reaperMethodClass

        def rmi = createReaperMethodInstance(reaperMethodClass)
        optionallyRunSingletonProcess(rmi, methodArgs)
    }


    /**  */
    public Map optionallyRunSingletonProcess(ReaperMethod reaperMethodInstance, Map methodArgs = [:]) {
        assert reaperMethodInstance

        def res = [:]
        def numRuns = reaperMethodInstance.getAllSuccessfulTrackedProcesses(traversal()).size()
        if (numRuns == 0) {
            res = call(reaperMethodInstance, methodArgs)
            log.info "Reaper.optionallyRunSingletonProcess ${reaperMethodInstance.class.simpleName} res: $res"
        } else {
            log.info "Reaper.optionallyRunSingletonProcess ${reaperMethodInstance.class.simpleName} already run ${numRuns} times"
        }

        return res        
    }    


    /**
     * Call a reaper method by name and arguments and return the result.
     *
     */
    public Map call(String methodName, Map methodArgs = [:]) {
        log.trace "Reaper.call methodName:${methodName}"

        // get reaper method class singleton instance
        def rmi = createReaperMethodInstance(methodName)
        if (!rmi) throw new MissingMethodException(methodName, this.class, methodArgs)
        log.trace "reaper method class: ${rmi.class.name}"

        call(rmi, methodArgs)
    }


    /**
     * Call a reaper method instance and arguments and return the result.
     *
     */
    public Map call(ReaperMethod reaperMethodInstance, Map methodArgs = [:]) {
        log.trace "Reaper.call reaperMethodInstance:${reaperMethodInstance}"

        // check args
        assert reaperMethodInstance

        // will return a map result
        def res = [
            reaperMethodInstance:reaperMethodInstance,
        	success:false,
        	checkPreConditions:[],
        	reap:[:],
        	checkPostConditions:[]
        ]

        // reaper process
        def procV = reaperMethodInstance.createReaperProcess()
        log.trace "Reaper.call reaper process vertex: $procV" 
        if (procV) reaperMethodInstance.setTrackedProcessVertex(procV)

        // pre-condition check
        def preCheck = reaperMethodInstance.checkPreConditions(methodArgs)
        if (preCheck) {
            res.checkPreConditions = preCheck
            return res
        }

        // reap
        res.reap = reaperMethodInstance.reap(methodArgs)
        assert res.reap != null, "${reaperMethodInstance.name}() with arguments ${methodArgs} returned a null result"
        //assert res.reap.get('graphModified') != null, "${reaperMethodInstance.name}() with arguments ${methodArgs} returned a null graphModified boolean value"

        // post-condition check
        res.checkPostConditions = reaperMethodInstance.checkPostConditions(methodArgs, res)

        // data set descriptor
        if (reaperMethodInstance.respondsTo('saveDataSetDescriptor', Map)) {
            def descriptorVs = reaperMethodInstance.saveDataSetDescriptor(res.reap)
            def g = traversal()
            try {
                descriptorVs.each { dv ->
                    Core.EX.IS_OUTPUT_OF.relate(g, dv, procV)
                }
            } finally {
                if (g) g.close()
            }
        }

        // compute success
        res.success = !(res.checkPreConditions || res.checkPostConditions)
        if (res.reap.success != null && !res.reap.success) res.success = false

        // process is a success
        procV.property(Core.PX.SUCCESS.label, res.success)

        // return the result
        return res
    }


    /** */
    public Set<Vertex> getAllTrackedProcesses(Class reaperMethodClass) {
        assert reaperMethodClass
        def rm = createReaperMethodInstance(reaperMethodClass)
        rm.getAllTrackedProcesses(traversal())
    }


    /** */
    public Set<Vertex> getAllSuccessfulTrackedProcesses(Class reaperMethodClass) {
        assert reaperMethodClass
        def rm = createReaperMethodInstance(reaperMethodClass)
        rm.getAllSuccessfulTrackedProcesses(traversal())
    }



    ///////////////////////////////////////////////////////////////////////////
    // METHOD MISSING - CONVENIENCE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    def methodMissing(String name, def args) {
        //log.trace "methodMissing name:$name args:$args ${args.class.name}"

        if (findReaperMethodClass(name)) {
            //def firstArg = (args != null && args.length > 0) ? args[0] : [:]
            if (args != null && args.length > 1) {
                throw new MissingMethodException(name, this.class, args)
            } else if (args != null && args.length == 1) {
                def firstArg = args[0]
                //log.trace "firstArg: $firstArg ${firstArg.class}"
                return call(name, firstArg)
            } else {
                return call(name)
            }
        }

        throw new MissingMethodException(name, this.class, args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY - REAPER METHOD CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get all reaper method classes using introspection.
     *
     */
    public Collection<Class> getAllReaperMethodClasses() {
        def allSubClasses = []
        allSubClasses.addAll(Arrays.asList(this.class.getDeclaredClasses()));
        def allReaperMethodClasses = allSubClasses.findAll { cl -> ReaperMethod.isAssignableFrom(cl) }
        return allReaperMethodClasses
    }


    /**
     * Find a reaper method class by case insensitive matching of the name.
     *
     */
    public Class findReaperMethodClass(String methodName) {
        def vmcs = getAllReaperMethodClasses()
        def matches = vmcs.findAll { it.simpleName.toLowerCase() == methodName.toLowerCase() }
        if (matches.size() > 1) throw new RuntimeException("multiple matches for $methodName: ${matches}")
        return (matches.size() == 1) ? matches.first() : null
    }


    /** */
    public List<Class> getVertexDefClasses(Class reaperMethodClass) {
        List<Class> innerClasses = reaperMethodClass.getDeclaredClasses()
        //log.debug "innerClasses: $innerClasses"

        def vertexDefClasses = innerClasses.findAll { cl -> VertexDefTrait.isAssignableFrom(cl) }
        //log.debug "vertexDefClasses: $vertexDefClasses"

        return vertexDefClasses
    }


    /** */
    public ReaperMethod method(String methodName) {
        createReaperMethodInstance(methodName)
    }


    /**
     * Convenience method to get the singleton instance of a reaper method class.
     *
     */
    public ReaperMethod createReaperMethodInstance(String methodName) {
        def rmc = findReaperMethodClass(methodName)
        if (!rmc) return null
        createReaperMethodInstance(rmc)
    }


    /**
     * Convenience method to get the singleton instance of a reaper method class.
     *
     */
    public ReaperMethod createReaperMethodInstance(Class rmc) {
        // validate argument
        assert rmc
        def allReaperMethodClasses = getAllReaperMethodClasses()
        assert allReaperMethodClasses.contains(rmc)

        // create reaper method instance
        def vm = rmc.newInstance(this)

        return vm
    }

}