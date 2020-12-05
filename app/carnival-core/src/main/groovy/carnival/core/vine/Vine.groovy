package carnival.core.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.core.util.CoreUtil



/** 
 * Vine is is a trait that can be applied to a class to enable the
 * implementation of vine methods.  The Vine trait is not required to implement
 * vine methods.  It provides a convenient mechanism to aggregate related vine
 * methods and share common resources.
 *
 */
trait Vine {

    /** */
    static Logger log = LoggerFactory.getLogger(Vine)

    /** */
    Map<String,Object> _dynamicVineMethodResources = new HashMap<String,Object>()

    /** */
    def methodMissing(String name, def args) {
        log.trace "Vine invoke method via methodMissing name:$name args:${args?.class?.name}"

        // verify arguments
        if (args != null) {
            if (args.size() > 2) throw new IllegalArgumentException("there can be at most two arguments to a vine method call: ${name} ${args}")
            if (args.size() == 1) {
                if (!(args[0] instanceof Map)) throw new IllegalArgumentException("args must be a map: ${name} ${args}")
            }
            if (args.size() == 2) {
                if (!(args[0] instanceof CacheMode)) throw new IllegalArgumentException("the first argument must be a cache mode: ${name} ${args[0]}")
                if (!(args[1] instanceof Map)) throw new IllegalArgumentException("the second argument must be a map: ${name} ${args[1]}")
            }
        }

        // find and create the vine method instance
        VineMethod vmi = createVineMethodInstance(name)
        if (vmi == null) throw new MissingMethodException(name, this.class, args)

        // call the method
        VineMethodCall mc
        if (args == null) {
            mc = vmi.call()
        } else {
            if (args.size() == 1) {
                mc = vmi.call(args[0])
            } else if (args.size() == 2) {
                mc = vmi.call(args[0], args[1])
            }
        } 

        // return the result
        mc
    }


    /** */
    VineMethod method(String name) {
        assert name != null
        assert name.trim().length() > 0
        createVineMethodInstance(name)
    }



    ///////////////////////////////////////////////////////////////////////////
    // UTILITY - VINE METHOD CLASSES
    ///////////////////////////////////////////////////////////////////////////    

    /**
     * Get all vine method classes using introspection.
     *
     */
    public Set<Class> allVineMethodClasses() {
        Set<Class> subClasses = new HashSet<Class>()
        subClasses.addAll(Arrays.asList(this.class.getDeclaredClasses()));
        //log.debug "subClasses: ${subClasses}"
        
        Set<Class> vineMethodClasses = subClasses.findAll { cl -> VineMethod.isAssignableFrom(cl) }

        return vineMethodClasses
    }


    /**
     * Find a vine method class by case insensitive matching of the name.
     *
     */
    public Set<Class> findAllVineMethodClasses(String methodName) {
        def vmcs = allVineMethodClasses()
        vmcs.findAll { it.simpleName.toLowerCase() == methodName.toLowerCase() }
    }


    /**
     * Find a vine method class by case insensitive matching of the name.
     *
     */
    public Class findVineMethodClass(String methodName) {
        def matches = findAllVineMethodClasses(methodName)
        if (matches.size() > 1) throw new RuntimeException("multiple matches for $methodName: ${matches}")
        if (matches.size() < 1) return null

        def match = matches.first()
        return match
    }


    /** */
    public VineMethod createVineMethodInstance(String methodName) {
        log.trace "Vine.createVineMethodInstance methodName:${methodName}"

        // find the vine method class
        def vmc = findVineMethodClass(methodName)
        if (!vmc) throw new MissingMethodException(methodName, this.class)
        log.trace "vine method class: ${vmc.name}"

        // create a vine method instance
        def vm = vmc.newInstance(this)

        // should probably get rid of this
        _dynamicVineMethodResources.each { String name, Object value ->
            setResource(vm, name, value)
        }

        return vm
    }


    /** */
    void setResource(VineMethod vm, String name, Object value) {
        vm.metaClass."${name}" = value
    }


    /** */
    void vineMethodResource(String name, Object value) {
        _dynamicVineMethodResources.put(name, value)
    }

}