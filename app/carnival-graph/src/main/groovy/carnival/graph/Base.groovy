package carnival.graph



import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory



/** 
 * @EdgeDefinition and @PropertyDefinition do not seem to work here... not sure
 * why.  Perhaps it is becasue this class is in the same package as the
 * definition classes.  Whatever it is, directly implementing the traits works
 * fine.
 *
 */
class Base {

    /** */
    //@EdgeDefinition
    static enum EX implements EdgeDefTrait {
        IS_SUBCLASS_OF,
    }


    /** */
    //@PropertyDefinition
    static enum PX implements PropertyDefTrait {
        IS_CLASS,
        NAME_SPACE,
        VERTEX_DEFINITION_CLASS
    }

}
