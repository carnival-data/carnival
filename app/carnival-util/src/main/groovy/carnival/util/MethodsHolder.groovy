package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory



/**
 * A trait that supports the finding of "method" classes.
 *
 */
trait MethodsHolder {

    /** A logger to use */
    static Logger log = LoggerFactory.getLogger(this.class)



    /**
     * Return all the method classes of this class that are subclasses of the
     * provided method class.
     * @param methodClass The method class
     * @return The matching set of method classes of this object
     */
    public Set<Class> allMethodClasses(Class methodClass) {
        assert methodClass != null

        Set<Class> subClasses = new HashSet<Class>()
        Class cl = this.class
        while (cl != null) {
            subClasses.addAll(Arrays.asList(cl.getDeclaredClasses()));
            cl = cl.getSuperclass()
        }
        //log.debug "subClasses: ${subClasses}"
        
        Set<Class> methodClasses = subClasses.findAll { methodClass.isAssignableFrom(it) }

        return methodClasses
    }


    /**
     * Find all method classes by case insensitive matching of the name.
     * @param methodClass The method class to search
     * @param methodName The name of the method class to find
     * @return The set of matching classes
     */
    public Set<Class> findAllMethodClasses(Class methodClass, String methodName) {
        assert methodClass != null
        assert methodName != null
        def vmcs = allMethodClasses(methodClass)
        vmcs.findAll { it.simpleName.toLowerCase() == methodName.toLowerCase() }
    }


    /**
     * Find the first method class by case insensitive matching of the name.
     * @param methodClass The method class to search
     * @param methodName The name of the method class to find
     * @return The matched method class
     */
    public Class findMethodClass(Class methodClass, String methodName) {
        assert methodClass != null
        assert methodName != null

        def matches = findAllMethodClasses(methodClass, methodName)

        // if no matches, we need to return null
        if (matches.size() == 0) return null

        // if there is only one match, we want to return it whether
        // it was defined in the 'this' class or not
        if (matches.size() == 1) return matches[0]

        // there are multiple matches, try to find the one defined in 
        // this class.  this needs to be refined to look up the inheritance
        // tree and pick the first that matches.
        matches = matches.findAll({
            it.enclosingClass == this.class
        })

        // if matches does not equal 1 exactly, it means that there were
        // multiple matches that could not be resolved
        if (matches.size() != 1) throw new RuntimeException("multiple matches for $methodName: ${matches}")

        def match = matches[0]
        return match
    }
  
}




