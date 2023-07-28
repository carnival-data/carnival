package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.util.MethodsHolder






/** 
 * GraphMethods is a trait that facilitates the calling of methods on an 
 * object.
 *
 */
trait GraphMethods extends MethodsHolder {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** Logger */
    static Logger log = LoggerFactory.getLogger(this.metaClass.theClass)


    ///////////////////////////////////////////////////////////////////////////
    // CLIENT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Look up a graph method by name.
     * @param name The name of the graph method
     * @return The GraphMethod
     */
    GraphMethod method(String name) {
        assert name != null
        assert name.trim().length() > 0
        createGraphMethodInstance(name)
    }


    /** 
     * Look up a graph method by class.
     * @param gmc The graph method class
     * @return The GraphMethod
     */
    GraphMethod method(Class gmc) {
        assert gmc != null
        Set<Class> allGmcs = allGraphMethodClasses()
        Class foundGmc = allGmcs.find { it == gmc }
        assert foundGmc
        return foundGmc.newInstance(this)
    }


    /** 
     * Return a list of graph methods matching the provided names.
     * @param names The list of method names
     * @return The GraphMethodList
     */
    GraphMethodList methods(List<String> names) {
        assert names != null

        GraphMethodList gml = new GraphMethodList()
        names.each { name ->
            def gm = method(name)
            gml.graphMethods.add(gm)
        }

        return gml
    }


    /** 
     * Return a list of graph methods matching the provided names.
     * @param names The list of method names
     * @return The GraphMethodList
     */
    GraphMethodList methods(String... names) {
        methods(names.toList())
    }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////    

    /** 
     * Return all the graph methods of this object.
     * @return The set of all graph methods
     */
    public Set<Class> allGraphMethodClasses() {
        allMethodClasses(GraphMethod)
    }

    /** 
     * Find a graph method class by name.
     * @param name The name of the graph method to find.
     * @return The graph method class.
     */
    public Class findGraphMethodClass(String name) {
        findMethodClass(GraphMethod, name)
    }

    /** 
     * Create a graph method instance from a method name.
     * @param methodName The name of the method.
     * @return The GraphMethod
     */
    public GraphMethod createGraphMethodInstance(String methodName) {
        log.trace "GraphMethods.createGraphMethodInstance methodName:${methodName}"

        // find the vine method class
        def vmc = findMethodClass(GraphMethod, methodName)
        if (!vmc) throw new MissingMethodException(methodName, this.class)
        log.trace "graph method class: ${vmc.name}"

        // create a vine method instance
        def vm = vmc.newInstance(this)

        return vm
    }

}