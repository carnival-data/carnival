package carnival.graph



import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory



/** 
 * Contains property and edge definitions used to express base concepts like class relationships 
 * and namespace in a Carnival graph. These properties and edges are automatically instantiated
 * in the graph when appropriate.
 * <p>
 * Note: '@EdgeModel' and '@PropertyModel' do not seem to work here... not sure
 * why.  Perhaps it is becasue this class is in the same package as the
 * definition classes.  Whatever it is, directly implementing the traits works
 * fine.
 *
 */
class Base {

	///////////////////////////////////////////////////////////////////////////
	// STATIC FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** 
     * Namespace used for VertexDefTrait or EdgeDefTraits instances where global = true.
     * */
	static final String GLOBAL_NAME_SPACE = 'GlobalNameSpace'


	///////////////////////////////////////////////////////////////////////////
	// ELEMENT DEFINITIONS
	///////////////////////////////////////////////////////////////////////////

    /** 
     * Defines edges used to express class relationships in Carnival graphs.
     * */
    static enum EX implements EdgeDefTrait {
        IS_SUBCLASS_OF,
        IS_INSTANCE_OF
    }


    /** 
     * Defines properties used to define class and namespace information in Carnival graphs.
     * */
    static enum PX implements PropertyDefTrait {
        IS_CLASS,
        NAME_SPACE,
        VERTEX_DEFINITION_CLASS
    }

}
