package carnival.core.graph



//import groovy.transform.MapConstructor
//import groovy.transform.TupleConstructor
import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.VertexDefTrait



/** */
class Core {

	///////////////////////////////////////////////////////////////////////////
	// GRAPH MODEL
	//
	// The graph model enums below are currently part of an incubating method
	// of graph specification that may replace the *Definition classes found
	// in this file.
	///////////////////////////////////////////////////////////////////////////

    /** */
    static enum VX implements VertexDefTrait {
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
        DATA_TRANSFORMATION_PROCESS_CLASS,

        PROCESS,

        VALIDATION_FAILURE,

        IDENTIFIER (
            vertexProperties:[
                PX.VALUE.withConstraints(required:true, index:true)
            ]
        ),

        IDENTIFIER_CLASS (
            vertexProperties:[
                PX.NAME.withConstraints(required:true, unique:true), 
                PX.HAS_SCOPE.defaultValue(false).withConstraints(required:true, index:true), 
                PX.HAS_CREATION_FACILITY.defaultValue(false).withConstraints(required:true, index:true)
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
        ),


        private VX() {}
        //private VX(Map m) { if (m.vertexProperties) this.vertexProperties = m.vertexProperties }
        private VX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** */
    static enum EX implements EdgeDefTrait {
    	IS_IDENTIFIED_BY,
        WAS_IDENTIFIED_BY,

    	IS_INSTANCE_OF,
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

        private EX() {}
        private EX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** */
    static enum PX implements PropertyDefTrait {
        NAME,
        DATE,
        START_TIME,
        VALUE,
        UNIT,
        DESCRIPTION,
        VERSION,
        HAS_SCOPE,
        HAS_CREATION_FACILITY,
        SUCCESS
    }

}
