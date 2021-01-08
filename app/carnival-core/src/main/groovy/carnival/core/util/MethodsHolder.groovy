package carnival.core.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory



/**
 *
 *
 */
trait MethodsHolder {

    /** */
    static Logger log = LoggerFactory.getLogger(this.class)



    /** */
    public Set<Class> allMethodClasses(Class methodClass) {
        assert methodClass != null

        Set<Class> subClasses = new HashSet<Class>()
        subClasses.addAll(Arrays.asList(this.class.getDeclaredClasses()));
        //log.debug "subClasses: ${subClasses}"
        
        Set<Class> methodClasses = subClasses.findAll { cl -> methodClass.isAssignableFrom(cl) }

        return methodClasses
    }


    /**
     * Find a vine method class by case insensitive matching of the name.
     *
     */
    public Set<Class> findAllMethodClasses(Class methodClass, String methodName) {
        assert methodClass != null
        assert methodName != null
        def vmcs = allMethodClasses(methodClass)
        vmcs.findAll { it.simpleName.toLowerCase() == methodName.toLowerCase() }
    }


    /**
     * Find a vine method class by case insensitive matching of the name.
     *
     */
    public Class findMethodClass(Class methodClass, String methodName) {
        assert methodClass != null
        assert methodName != null

        def matches = findAllMethodClasses(methodClass, methodName)
        if (matches.size() > 1) throw new RuntimeException("multiple matches for $methodName: ${matches}")
        if (matches.size() < 1) return null

        def match = matches.first()
        return match
    }
  
}




