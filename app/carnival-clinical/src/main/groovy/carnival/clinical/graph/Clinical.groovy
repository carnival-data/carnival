package carnival.clinical.graph



import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.graph.VertexDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.EdgeDefTrait
import carnival.core.graph.Core




/** */
class Clinical {

    /** */
    static enum VX implements VertexDefTrait {
        PATIENT,
        PATIENT_WITH_ID_PROBLEMS,

        HEALTHCARE_ENCOUNTER (vertexProperties:[PX.ENCOUNTER_DATE]),
        BIOBANK_ENCOUNTER (vertexProperties:[PX.ENCOUNTER_DATE]),
        SPECIMEN_SUMMARY (vertexProperties:[Core.PX.NAME.withConstraints(index:true)]),

        CASE_REPORT_FORM,

        DATE_SHIFT_VALUE,

        CODE_REF (vertexProperties:[Core.PX.VALUE, PX.SYSTEM]),
        MEDICATION (vertexProperties:[Core.PX.VALUE, PX.SYSTEM]),
        MEASUREMENT (vertexProperties:[Core.PX.VALUE, PX.UNIT, Core.PX.NAME]),
        CODE_GROUP (
            vertexProperties:[
                Core.PX.NAME.withConstraints(index:true)
            ]
        ),

        CODED_PHENOTYPE (
            vertexProperties:[
                Core.PX.NAME.withConstraints(index:true), 
                Core.PX.DESCRIPTION, 
                PX.ROLLUP.withConstraints(required:true), 
                PX.LEAF, 
                PX.SEX, 
                PX.CONTROL_EXCLUDE_RANGE
            ]),
        CODED_PHENOTYPE_GROUP (
            vertexProperties:[
                Core.PX.NAME.withConstraints(index:true)
            ]
        ),
        PATIENT_PHECODE_ASSIGNMENT,

        PROTOCOL,
        STUDY (
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true, unique:true)
            ]
        ),

        LIFE_PROCESS,
        DEATH,
        DEATH_CANDIDATE,

        PATIENT_GROUP (
            vertexProperties:[
                Core.PX.NAME.withConstraints(index:true)
            ]
        ),
        
        ENCOUNTER_GROUP,

        PATIENT_STRATA (
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true, unique:true)
            ]
        )

        private VX() {}
        private VX(Map m) { if (m.vertexProperties) this.vertexProperties = m.vertexProperties }
    }


    /** */
    static enum EX implements EdgeDefTrait {
        IS_SCOPED_BY (range:[VX.STUDY]),

        IS_UNDER_PROTOCOL (
            domain:[VX.BIOBANK_ENCOUNTER],
            range:[VX.PROTOCOL]
        ),

        IS_MEMBER_OF (
            domain:[VX.PATIENT, VX.BIOBANK_ENCOUNTER, VX.CODED_PHENOTYPE],
            range:[VX.PATIENT_GROUP, VX.ENCOUNTER_GROUP, VX.CODED_PHENOTYPE_GROUP]
        ),

        WAS_MEMBER_OF (
            domain:[VX.PATIENT_WITH_ID_PROBLEMS],
            range:[VX.PATIENT_GROUP]
        ),

        IS_NOT_MEMBER_OF (
            domain:[VX.PATIENT],
            range:[VX.PATIENT_GROUP]
        ),

        WAS_NOT_MEMBER_OF (
            domain:[VX.PATIENT_WITH_ID_PROBLEMS],
            range:[VX.PATIENT_GROUP]
        ),

        CONTAINS_GROUP (
            domain:[VX.PATIENT_STRATA],
            range:[VX.PATIENT_GROUP]
        ),

        HAS_PART (
            domain:[VX.CODE_GROUP, VX.CODED_PHENOTYPE, VX.LIFE_PROCESS],
            range:[VX.CODE_REF, VX.DEATH, VX.DEATH_CANDIDATE]
        ),

        HAS_SPECIMENS (
            domain:[VX.BIOBANK_ENCOUNTER],
            range:[VX.SPECIMEN_SUMMARY]
        ),

        IS_SUBGROUP_OF(
            domain:[VX.CODED_PHENOTYPE],
            range:[VX.CODED_PHENOTYPE]
        ),

        HAS_CODED_PHENOTYPE(
            domain:[VX.PATIENT, VX.PATIENT_WITH_ID_PROBLEMS],
            range:[VX.CODED_PHENOTYPE]
        ),

        HAS_NUM_OCURRENCES_OF(
            domain:[VX.PATIENT, VX.PATIENT_WITH_ID_PROBLEMS],
            range:[VX.CODED_PHENOTYPE]
        ),

        IS_IDENTIFIED_BY (
            domain:[VX.PATIENT, VX.PATIENT_WITH_ID_PROBLEMS, VX.BIOBANK_ENCOUNTER], 
            range:[Core.VX.IDENTIFIER]
        ),

        IS_UNDER_CONSENT (
            domain:[VX.BIOBANK_ENCOUNTER],
            range:[VX.CASE_REPORT_FORM]
        ),

        PARTICIPATED_IN_ENCOUNTER (
            domain:[VX.PATIENT, VX.PATIENT_WITH_ID_PROBLEMS],
            range:[VX.BIOBANK_ENCOUNTER, VX.HEALTHCARE_ENCOUNTER]
        ),

        PARTICIPATED_IN_FORM_FILING (
            domain:[VX.BIOBANK_ENCOUNTER],
            range:[VX.CASE_REPORT_FORM]
        ),

        HAS_EVIDENCE (
            domain:[VX.DEATH, VX.DEATH_CANDIDATE]
        ),

        IS_PARTICIPANT_OF (
            domain:[VX.PATIENT],
            range:[VX.LIFE_PROCESS]
        ),

        DIED_ON (
            domain:[VX.PATIENT],
            range:[VX.DEATH]
        ),

        BEARS_STUDY_SUBJECT_ROLE_FOR (
            domain:[VX.PATIENT],
            range:[Clinical.VX.STUDY]
        ),

        IS_ASSOCIATED_WITH (
            domain:[VX.PATIENT, VX.DATE_SHIFT_VALUE],
            range:[Clinical.VX.STUDY]
        ),

        HAS_DEIDENTIFICATION_VALUE(
            domain:[VX.PATIENT],
            range:[VX.DATE_SHIFT_VALUE]
        ),

        HAS_ASSOCIATED_CODEREF(
            domain:[VX.HEALTHCARE_ENCOUNTER],
            range:[VX.CODE_REF]
        ),

        HAS_ASSOCIATED_MEDICATION(
            domain:[VX.HEALTHCARE_ENCOUNTER],
            range:[VX.MEDICATION]
        ),

        HAS_ASSOCIATED_MEASUREMENT(
            domain:[VX.HEALTHCARE_ENCOUNTER],
            range:[VX.MEASUREMENT]
        )

        private EX() {}
        private EX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** */
    static enum PX implements PropertyDefTrait {
        ENCOUNTER_DATE,
        SYSTEM,
        ROLLUP,
        LEAF,
        SEX,
        CONTROL_EXCLUDE_RANGE,

        HEIGHT_INCHES,
        WEIGHT_LBS,
        CALCULATED_BMI,

        UNIT
    }

}
