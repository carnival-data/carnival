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
    static Logger log = LoggerFactory.getLogger(this.class)


    ///////////////////////////////////////////////////////////////////////////
    // CLIENT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Look up a graph method.
     */
    GraphMethod method(String name) {
        assert name != null
        assert name.trim().length() > 0
        createGraphMethodInstance(name)
    }


    /** */
    GraphMethod method(Class gmc) {
        assert gmc != null
        Set<Class> allGmcs = allGraphMethodClasses()
        Class foundGmc = allGmcs.find { it == gmc }
        assert foundGmc
        return foundGmc.newInstance(this)
    }


    /** */
    GraphMethodList methods(List<String> names) {
        assert names != null

        GraphMethodList gml = new GraphMethodList()
        names.each { name ->
            def gm = method(name)
            gml.graphMethods.add(gm)
        }

        return gml
    }


    /** */
    GraphMethodList methods(String... names) {
        methods(names.toList())
    }


    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////    

    /** */
    public Set<Class> allGraphMethodClasses() {
        allMethodClasses(GraphMethod)
    }

    /** */
    public Class findGraphMethodClass(String name) {
        findMethodClass(GraphMethod, name)
    }

    /** */
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