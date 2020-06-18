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
    def methodMissing(String name, def args) {
        log.trace "JsonVine invoke method via methodMissing name:$name args:${args?.class?.name}"

        // verify that there is only one argument and it is a map
        if (args != null) {
            if (args.size() > 1) throw new IllegalArgumentException("there can only be a single map argument. args must be a list of length one. ${args}")
            if (!(args[0] instanceof Map)) throw new IllegalArgumentException("args must be a map: ${args}")
        }

        // find and create the vine method instance
        JsonVineMethod vmi = createVineMethodInstance(name)
        if (vmi == null) throw new MissingMethodException(name, this.class, args)

        // call the method
        Map methodArgs = (args != null) ? args[0] : null
        JsonVineMethodCall mc
        if (methodArgs == null) mc = vmi.call()
        else mc = vmi.call(methodArgs)

        // return the result
        mc
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
    public Class findVineMethodClass(String methodName) {
        def vmcs = allVineMethodClasses()
        def matches = vmcs.findAll { it.simpleName.toLowerCase() == methodName.toLowerCase() }
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
            vm.metaClass."${vmrf.name}" = vmrf.get(this)
        }

        return vm
    }


}