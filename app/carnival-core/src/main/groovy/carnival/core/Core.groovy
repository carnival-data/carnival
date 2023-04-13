package carnival.core



//import groovy.transform.MapConstructor
//import groovy.transform.TupleConstructor
import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.graph.EdgeModel
import carnival.graph.PropertyModel
import carnival.graph.VertexModel



/** 
 * The Core graph model.  The intention is for this model to be linked to
 * published ontologies such that there is congruency between the terms and
 * relations of this model and those of the published ontologies.  Future 
 * versions of Carnival will explicitly include this linkage.  The current
 * model is minimally documented in anticipation of this enhancement.
 *
 */
class Core {

	///////////////////////////////////////////////////////////////////////////
    // DATA REPRESENTATION
	///////////////////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////////////////////
	// GRAPH MODEL
	//
	// The graph model enums below are currently part of an incubating method
	// of graph specification that may replace the *Definition classes found
	// in this file.
	///////////////////////////////////////////////////////////////////////////

    /** 
     * Core vertex model. 
     * @see carnival.core.Core
     */
    @VertexModel
    static enum VX {
        /** Application */
        APPLICATION (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, index:true),
                PX.VERSION
            ]
        ),

        /** Database */
    	DATABASE,

        /** Relational data base */
    	RELATIONAL_DATABASE,

        /** Relational database record */
    	RELATIONAL_DATABASE_RECORD,

        /** Process class */
        PROCESS_CLASS,

        /** Process */
        PROCESS(
            vertexProperties:[
                PX.ARGUMENTS_HASH,
                PX.START_TIME,
                PX.STOP_TIME,
                PX.EXCEPTION_MESSAGE
            ]
        ),

        /** Graph process class */
        GRAPH_PROCESS_CLASS(
            superClass: VX.PROCESS_CLASS
        ),

        /** Graph process */
        GRAPH_PROCESS(
            vertexProperties:[
                PX.NAME,
                PX.ARGUMENTS_HASH,
                PX.START_TIME,
                PX.STOP_TIME,
                PX.EXCEPTION_MESSAGE
            ]
        ),

        /** Data transformation process class */
        DATA_TRANSFORMATION_PROCESS_CLASS(
            superClass: VX.PROCESS_CLASS            
        ),

        /** Validation failure */
        VALIDATION_FAILURE,

        /** Identifier class */
        IDENTIFIER_CLASS (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, unique:true), 
                PX.HAS_SCOPE.defaultValue(false).withConstraints(required:true, index:true), 
                PX.HAS_CREATION_FACILITY.defaultValue(false).withConstraints(required:true, index:true)
            ]
        ),

        /** Identifier */
        IDENTIFIER (
            vertexProperties:[
                PX.VALUE.withConstraints(required:true, index:true)
            ]
        ),

        /** Identifier facility */
        IDENTIFIER_FACILITY (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, unique:true)
            ]
        ),

        /** Identifier scope */
        IDENTIFIER_SCOPE (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, unique:true)
            ]
        )
    }


    /** 
     * Core edge model 
     * @see carnival.core.Core
     */
    @EdgeModel
    static enum EX {

        /** Is identified by */
    	IS_IDENTIFIED_BY,

        /** Was identified by */
        WAS_IDENTIFIED_BY,

        /** Is input of */
    	IS_INPUT_OF,

        /** Is output of */
    	IS_OUTPUT_OF,
        
        /** Is member of */
        IS_MEMBER_OF,

        /** Was member of */
        WAS_MEMBER_OF,

        /** Is not a member of */
        IS_NOT_MEMBER_OF,

        /** Was not a member of */
        WAS_NOT_MEMBER_OF,

        /** Has part */
        HAS_PART,

        /** Depends on */
        DEPENDS_ON,
        
        /** References */
    	REFERENCES,

        /** Participates in */
    	PARTICIPATED_IN,

        /** Is refernced by */
    	IS_REFERENCED_BY,

        /** Is scoped by */
    	IS_SCOPED_BY (
            domain:[VX.IDENTIFIER],
            range:[VX.IDENTIFIER_SCOPE]
        ),
    	
        /** Was created by */
        WAS_CREATED_BY (
            domain:[VX.IDENTIFIER], 
            range:[VX.IDENTIFIER_FACILITY]
        ),
    	
        /** Contains */
        CONTAINS,

        /** Is associated with */
    	IS_ASSOCIATED_WITH,

        /** Describes */
        DESCRIBES,

        /** Is invalid as described by */
        IS_INVALID_AS_DESCRIBED_BY,

        /** Is derived from */
        IS_DERIVED_FROM
    }


    /** 
     * Core property model 
     * @see carnival.core.Core
     */
    @PropertyModel
    static enum PX {
        /** Name */
        NAME,

        /** Date */
        DATE,

        /** Start time */
        START_TIME,

        /** Stop time */
        STOP_TIME,

        /** Value */
        VALUE,

        /** Unit */
        UNIT,

        /** Description */
        DESCRIPTION,

        /** Version */
        VERSION,

        /** Has scope */
        HAS_SCOPE,

        /** Has creation facility */
        HAS_CREATION_FACILITY,

        /** Success */
        SUCCESS,

        /** Message */
        MESSAGE,

        /** Excel row num */
        EXCEL_ROW_NUM,

        /** Derivation source */
        DERIVATION_SOURCE,

        /** Arguments hash */
        ARGUMENTS_HASH,

        /** Exception message */
        EXCEPTION_MESSAGE
    }

}
