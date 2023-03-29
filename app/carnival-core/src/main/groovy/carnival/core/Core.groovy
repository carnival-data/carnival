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

    /** Core vertex model. */
    @VertexModel
    static enum VX {
        APPLICATION (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, index:true),
                PX.VERSION
            ]
        ),
    	DATABASE,
    	RELATIONAL_DATABASE,
    	RELATIONAL_DATABASE_RECORD,

        PROCESS_CLASS,
        PROCESS(
            vertexProperties:[
                PX.ARGUMENTS_HASH,
                PX.START_TIME,
                PX.STOP_TIME,
                PX.EXCEPTION_MESSAGE
            ]
        ),

        GRAPH_PROCESS_CLASS(
            superClass: VX.PROCESS_CLASS
        ),
        GRAPH_PROCESS(
            vertexProperties:[
                PX.NAME,
                PX.ARGUMENTS_HASH,
                PX.START_TIME,
                PX.STOP_TIME,
                PX.EXCEPTION_MESSAGE
            ]
        ),

        DATA_TRANSFORMATION_PROCESS_CLASS(
            superClass: VX.PROCESS_CLASS            
        ),

        VALIDATION_FAILURE,

        IDENTIFIER_CLASS (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, unique:true), 
                PX.HAS_SCOPE.defaultValue(false).withConstraints(required:true, index:true), 
                PX.HAS_CREATION_FACILITY.defaultValue(false).withConstraints(required:true, index:true)
            ]
        ),
        IDENTIFIER (
            vertexProperties:[
                PX.VALUE.withConstraints(required:true, index:true)
            ]
        ),
        IDENTIFIER_FACILITY (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, unique:true)
            ]
        ),
        IDENTIFIER_SCOPE (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, unique:true)
            ]
        )
    }


    /** Core edge model */
    @EdgeModel
    static enum EX {
    	IS_IDENTIFIED_BY,
        WAS_IDENTIFIED_BY,

    	IS_INPUT_OF,
    	IS_OUTPUT_OF,
        
        IS_MEMBER_OF,
        WAS_MEMBER_OF,
        IS_NOT_MEMBER_OF,
        WAS_NOT_MEMBER_OF,
        HAS_PART,
        DEPENDS_ON,
        
    	REFERENCES,
    	PARTICIPATED_IN,
    	IS_REFERENCED_BY,
    	IS_SCOPED_BY (
            domain:[VX.IDENTIFIER],
            range:[VX.IDENTIFIER_SCOPE]
        ),
    	
        WAS_CREATED_BY (
            domain:[VX.IDENTIFIER], 
            range:[VX.IDENTIFIER_FACILITY]
        ),
    	
        CONTAINS,

    	IS_ASSOCIATED_WITH,
        DESCRIBES,

        IS_INVALID_AS_DESCRIBED_BY,
        IS_DERIVED_FROM
    }


    /** Core property model */
    @PropertyModel
    static enum PX {
        NAME,
        DATE,
        START_TIME,
        STOP_TIME,
        VALUE,
        UNIT,
        DESCRIPTION,
        VERSION,
        HAS_SCOPE,
        HAS_CREATION_FACILITY,
        SUCCESS,
        MESSAGE,
        EXCEL_ROW_NUM,
        DERIVATION_SOURCE,
        ARGUMENTS_HASH,
        EXCEPTION_MESSAGE
    }

}