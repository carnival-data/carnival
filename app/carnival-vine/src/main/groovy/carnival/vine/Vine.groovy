package carnival.vine



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.util.MethodsHolder






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

    /** The log object to use. */
    static Logger log = LoggerFactory.getLogger(this.metaClass.theClass)


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Key value pairs of dynamic resources that will be provided to vine 
     * methods.
     */
    Map<String,Object> _dynamicVineMethodResources = new HashMap<String,Object>()

    /** 
     * The VineConfiguration object that will be used to configure vine methods.  
     */
    VineConfiguration vineConfiguration = VineConfiguration.defaultConfiguration()


    ///////////////////////////////////////////////////////////////////////////
    // CLIENT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Look up a vine method with the given name and return it.
     * @param Name The name of the vine method to call.
     * @return Return the VineMethod object with the given name.
     *
     */
    VineMethod method(String name) {
        assert name != null
        assert name.trim().length() > 0
        createVineMethodInstance(name)
    }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////    

    /** 
     * Return the set of all vine method objects contained in this vine.
     * @return The set of all vine method objects contained in this vine.
     *
     */
    public Set<Class> allVineMethodClasses() {
        allMethodClasses(VineMethod)
    }

    /** 
     * Find a vine method class by name.
     * @param name The name of the vine mthod.
     * @return The class of the vine method with the given name.
     */
    public Class findVineMethodClass(String name) {
        findMethodClass(VineMethod, name)
    }

    /** 
     * Create a VineMethod object for the vine with the given name.
     * @param methodName The name of the vine method.
     * @return The VineMethod object.
     *
     */
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


    /** 
     * Set a key value resource of the vine mathod. 
     * @param vm The VineMethod object
     * @param name The name of the resource
     * @param value The value of the resource
     */
    void setResource(VineMethod vm, String name, Object value) {
        vm.metaClass."${name}" = value
    }


    /** 
     * Set a vine method resource.
     * @param name The name of the resource
     * @value The value of the resource
     */
    void vineMethodResource(String name, Object value) {
        _dynamicVineMethodResources.put(name, value)
    }



    ///////////////////////////////////////////////////////////////////////////
    // CONVENIENCE
    ///////////////////////////////////////////////////////////////////////////    

    /** 
     * methodMissing() is a Groovy language feature that catches unknown method
     * calls. <a href="https://groovy-lang.org/metaprogramming.html">Groovy metaprogramming</a>
     * Carnival uses methodMissing() to support calling vine methods with
     * convenience methods.  Ex. myVineObject.myFunMethod(a:1) would call the 
     * vine method named "MyFunMethod" with an argument named "a" with value 1.
     * @param name The name of the missing method.
     * @param args The provided arguments.
     *
     */
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