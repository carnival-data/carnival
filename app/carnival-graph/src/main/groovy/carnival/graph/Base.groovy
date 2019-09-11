package carnival.graph



import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory



/** */
class Base {

    /** */
    static enum EX implements EdgeDefTrait {
        IS_SUBCLASS_OF,
    }


    /** */
    static enum PX implements PropertyDefTrait {
        IS_CLASS,
        NAME_SPACE,
        VERTEX_DEFINITION_CLASS
    }

}
