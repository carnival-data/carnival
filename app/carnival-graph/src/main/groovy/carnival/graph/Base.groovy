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
     * Namespace used for VertexDefinition or EdgeDefinitions instances where global = true.
     * */
	static final String GLOBAL_NAME_SPACE = 'GlobalNameSpace'


	///////////////////////////////////////////////////////////////////////////
	// ELEMENT DEFINITIONS
	///////////////////////////////////////////////////////////////////////////

    /** 
     * Defines edges used to express class relationships in Carnival graphs.
     * */
    static enum EX implements EdgeDefinition {

        /** 
         * Out vertex represents a class that is a sublass of the class
         * represented by the in vertex.
         */
        IS_SUBCLASS_OF,

        /**
         * The out vertex represents and object that is an instance of the
         * class represented by the in vertex.
         */
        IS_INSTANCE_OF
    }


    /** 
     * Defines properties used to define class and namespace information in Carnival graphs.
     * */
    static enum PX implements PropertyDefinition {

        /** If true, the vertex represents a class */
        IS_CLASS,

        /** 
         * The name space of the vertex label, set by Carnival to differentiate
         * between vertex labels defined in different models.
         */
        NAME_SPACE,

        /**
         * If the name space if global, the class that defined the model is
         * stored in the vertex definition class property, which is requried by
         * parts of the Carnival machinery.
         */
        VERTEX_DEFINITION_CLASS
    }

}
