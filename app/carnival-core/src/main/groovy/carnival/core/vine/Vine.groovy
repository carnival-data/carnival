package carnival.core.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.core.util.MethodsHolder






/** 
 * Vine is is a trait that can be applied to a class to enable the
 * implementation of vine methods.  The Vine trait is not required to implement
 * vine methods.  It provides a convenient mechanism to aggregate related vine
 * methods and share common resources.
 *
 */
trait Vine extends MethodsHolder {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger(this.class)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    Map<String,Object> _dynamicVineMethodResources = new HashMap<String,Object>()

    /** */
    VineConfiguration vineConfiguration = VineConfiguration.defaultConfiguration()


    ///////////////////////////////////////////////////////////////////////////
    // CLIENT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    VineMethod method(String name) {
        assert name != null
        assert name.trim().length() > 0
        createVineMethodInstance(name)
    }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////    

    /** */
    public Set<Class> allVineMethodClasses() {
        allMethodClasses(VineMethod)
    }

    /** */
    public Class findVineMethodClass(String name) {
        findMethodClass(VineMethod, name)
    }

    /** */
    public VineMethod createVineMethodInstance(String methodName) {
        log.trace "Vine.createVineMethodInstance methodName:${methodName}"

        // find the vine method class
        def vmc = findMethodClass(VineMethod, methodName)
        if (!vmc) throw new MissingMethodException(methodName, this.class)
        log.trace "vine method class: ${vmc.name}"

        // create a vine method instance
        def vm = vmc.newInstance(this)
        vm.vineConfiguration = this.vineConfiguration

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



    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE
    ///////////////////////////////////////////////////////////////////////////    

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
        def vmi = createVineMethodInstance(name)
        if (vmi == null) throw new MissingMethodException(name, this.class, args)

        // call the method
        def mc
        if (args == null) {
            mc = vmi.call()
        } else {
            if (args.size() == 1) {
                mc = vmi.call(args[0])
            } else if (args.size() == 2) {
                mc = vmi.call(args[0], args[1])
            } else {
                throw new IllegalArgumentException("Unexpected number of arguments: ${args.size()} ${name} ${args}")
            }
        } 

        // return the result
        mc
    }

}