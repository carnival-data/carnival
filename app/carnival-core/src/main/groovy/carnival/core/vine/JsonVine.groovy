package carnival.core.vine



import java.lang.reflect.Field

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.core.util.CoreUtil



/** */
trait JsonVine {

    /** */
    static Logger log = LoggerFactory.getLogger(JsonVine)

    /** */
    Map<String,Object> _dynamicVineMethodResources = new HashMap<String,Object>()

    /** */
    def methodMissing(String name, def args) {
        log.trace "JsonVine invoke method via methodMissing name:$name args:${args?.class?.name}"

        // verify that there is only one argument and it is a map
        if (args != null) {
            if (args.size() > 2) throw new IllegalArgumentException("there can be at most two arguments to a JSON vine method call: ${args}")
            if (args.size() == 1) {
                if (!(args[0] instanceof Map)) throw new IllegalArgumentException("args must be a map: ${args}")
            }
            if (args.size() == 2) {
                if (!(args[0] instanceof CachingVine.CacheMode)) throw new IllegalArgumentException("the first argument must be a cache mode: ${args[0]}")
                if (!(args[1] instanceof Map)) throw new IllegalArgumentException("the second argument must be a map: ${args[1]}")
            }
        }

        // find and create the vine method instance
        JsonVineMethod vmi = createVineMethodInstance(name)
        if (vmi == null) throw new MissingMethodException(name, this.class, args)

        // call the method
        JsonVineMethodCall mc
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
    JsonVineMethod method(String name) {
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
        
        Set<Class> vineMethodClasses = subClasses.findAll { cl -> JsonVineMethod.isAssignableFrom(cl) }

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
    public JsonVineMethod createVineMethodInstance(String methodName) {
        log.trace "JsonVine.createVineMethodInstance methodName:${methodName}"

        // find the vine method class
        def vmc = findVineMethodClass(methodName)
        if (!vmc) throw new MissingMethodException(methodName, this.class)
        log.trace "json-vine method class: ${vmc.name}"

        // create a vine method instance
        def vm = vmc.newInstance()

        // add the vine method resource to the vine method instance
        def classes = CoreUtil.allClasses(this)

        //log.debug "this.class: ${this.class}"
        List<Field> vineMethodResourceFields = []
        classes.each { cl ->
            for (Field field: cl.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(VineMethodResource.class)) {
                    vineMethodResourceFields << field
                }
            }
        }

        vineMethodResourceFields.each { vmrf ->
            setResource(vm, vmrf.name, vmrf.get(this))
        }

        _dynamicVineMethodResources.each { String name, Object value ->
            setResource(vm, name, value)
        }

        return vm
    }


    /** */
    void setResource(JsonVineMethod vm, String name, Object value) {
        vm.metaClass."${name}" = value

        // the following does not work, which seems pretty terrifying
        //vm.metaClass.setProperty(vm, name, value)
        // the weird looking uncommented code does work. I don't know
        // why it works, but methods called directoy on the metaClass
        // do not. this seems brittle and not good.
    }


    /** */
    void vineMethodResource(String name, Object value) {
        _dynamicVineMethodResources.put(name, value)
    }

}