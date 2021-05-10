package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.core.util.MethodsHolder






/** 
 *
 */
trait GraphMethods extends MethodsHolder {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger(this.class)


    ///////////////////////////////////////////////////////////////////////////
    // CLIENT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
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