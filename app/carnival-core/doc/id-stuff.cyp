///////////////////////////////////////////////////////////////////////////////
// GENERIC IDENTIFIERS REPORT
///////////////////////////////////////////////////////////////////////////////

MATCH 
	// classes and facilities
	(empiClass:IdentifierClass {name:'empi'})
	, (mrnClass:IdentifierClass {name:'mrn'})
	, (ecClass:IdentifierClass {name:'encounter_code'} )
	, (packetIdClass:IdentifierClass {name:'encounter_pack_id'} )
	, (cgiProtocol:Protocol {name:'CGI'} )
	, (hupFacility:IdentifierFacility {name:'HUP'})

	// cohort from issue
	, (idmp:IdentifierMatchProcess)
	, (qivg:QueryIdValueGroup)-[:is_input_of]->(idmp)
	, (qiv:QueryIdValue)-[:is_member_of]->(qivg)

	// identifier match
	, (qiv)-[:maps_to]->(id:Identifier)
	, (id)-[:is_instance_of]->(idClass:IdentifierClass)	

// bind with the match process
WHERE 
	ID(idmp) = 788481 // $matchProcId

// gather elements
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass

//
// patients
//

// patient p
OPTIONAL MATCH (qiv)-[:identifies]->(p:Patient)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p

// EMPIs for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(empiP:Identifier)
	, (empiP)-[:is_instance_of]->(empiClass)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , COLLECT(DISTINCT empiP.value) AS PATIENT_EMPIS

// MRNS for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(mrnP:Identifier)
	, (mrnP)-[:is_instance_of]->(mrnClass)
	, (mrnP)-[:was_created_by]->(hupFacility)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
    , COLLECT(DISTINCT mrnP.value) AS PATIENT_HUP_MRNS

// encounter codes for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(encounterCodeP:Identifier)
	, (encounterCodeP)-[:is_instance_of]->(ecClass)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
    , PATIENT_HUP_MRNS
    , COLLECT(DISTINCT encounterCodeP.value) AS PATIENT_ENCOUNTER_CODES

// packet identifiers for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(packetIdP:Identifier)
	, (packetIdP)-[:is_instance_of]->(packetIdClass)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
	, PATIENT_HUP_MRNS
	, PATIENT_ENCOUNTER_CODES
	, COLLECT(DISTINCT packetIdP.value) AS PATIENT_PACKET_IDS
    
//
// unmatched patient consents
//
OPTIONAL MATCH (qiv)-[:identifies]->(encUnmatched:BiobankEncounter)
, (encUnmatched)-[:is_associated_with]->(pts:BiobankPtsRecord)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
	, PATIENT_HUP_MRNS
	, PATIENT_ENCOUNTER_CODES
	, PATIENT_PACKET_IDS
    , COLLECT(properties(pts)) AS UNMATCHED_CRF_DATA

//
// multi-match patient consents
//
OPTIONAL MATCH (qiv)-[:identifies]->(encMultiMatch:BiobankEncounter)
, (encMultiMatch)-[:is_associated_with]->(mmr:EncounterMultiMatchRecord)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
	, PATIENT_HUP_MRNS
	, PATIENT_ENCOUNTER_CODES
	, PATIENT_PACKET_IDS
    , UNMATCHED_CRF_DATA
    , COLLECT(properties(mmr)) AS MULTIMATCH_PATIENT_DATA

//
// cgi encounter properties
//
OPTIONAL MATCH (qiv)-[:identifies]->(cgiEnc:BiobankEncounter)
, (cgiEnc)-[:is_under_protocol]->(cgiProtocol)
, (cgiEnc)-[:participated_in_form_filing]->(cgiCrf:CaseReportForm)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
	, PATIENT_HUP_MRNS
	, PATIENT_ENCOUNTER_CODES
	, PATIENT_PACKET_IDS
    , UNMATCHED_CRF_DATA
    , MULTIMATCH_PATIENT_DATA
    , COLLECT(properties(cgiCrf)) AS CGI_CRF_DATA


//
// return
//
RETURN 
	qiv.value AS IDENTIFIER_VALUE
	, qiv.cleanValue AS IDENTIFIER_VALUE_CLEAN

    , COUNT(id) AS NUM_MATCHED_IDENTIFIERS
    , COLLECT(id.value) AS MATCHED_IDENTIFIERS
    , COLLECT(id.value + ":" + idClass.name) AS MATCHED_IDENTIFIER_CLASSES

	, CASE ID(p) WHEN NULL THEN '' ELSE ID(p) END AS TMP_PID
    
    , PATIENT_EMPIS
    , PATIENT_HUP_MRNS
    , PATIENT_ENCOUNTER_CODES
    , PATIENT_PACKET_IDS
    
    , UNMATCHED_CRF_DATA
    , MULTIMATCH_PATIENT_DATA
    , CGI_CRF_DATA

ORDER BY qiv.value

LIMIT 100










MATCH 
	(matchProc:IdentifierMatchProcess)
	, (inputIdGroup:QueryIdValueGroup)-[:is_input_of]->(matchProc)
	, (inputId:QueryIdValue)-[:is_member_of]->(inputIdGroup)
	, (inputId)-[:maps_to]->(id:Identifier)
	, (id)-[:is_instance_of]->(idClass:IdentifierClass)
WHERE 
	ID(matchProc) = 788481
RETURN 
	matchProc.name AS MATCH_PROCESS_NAME
	, inputIdGroup.name AS INPUT_ID_GROUP_NAME
    , inputId.value AS IDENTIFIER_VALUE
    , inputId.cleanValue AS IDENTIFIER_VALUE_CLEAN
    , COUNT(id) AS NUM_MATCHED_IDENTIFIERS
    , COLLECT(id.value) AS MATCHED_IDENTIFIERS
    , COLLECT(id.value + ":" + idClass.name) AS MATCHED_IDENTIFIER_CLASSES
LIMIT 10















MATCH 
	// classes and facilities
	(empiClass:IdentifierClass {name:'empi'})
	, (mrnClass:IdentifierClass {name:'mrn'})
	, (ecClass:IdentifierClass {name:'encounter_code'} )
	, (packetIdClass:IdentifierClass {name:'encounter_pack_id'} )
	, (cgiProtocol:Protocol {name:'CGI'} )
	, (hupFacility:IdentifierFacility {name:'HUP'})

	// cohort from issue
	, (idmp:IdentifierMatchProcess)
	, (qivg:QueryIdValueGroup)-[:is_input_of]->(idmp)
	, (qiv:QueryIdValue)-[:is_member_of]->(qivg)

	// identifier match
	, (qiv)-[:maps_to]->(id:Identifier)
	, (id)-[:is_instance_of]->(idClass:IdentifierClass)	

// bind with the match process
WHERE 
	ID(idmp) = 788481 // $matchProcId

// gather elements
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass

//
// patients
//
OPTIONAL MATCH (qiv)-[:identifies]->(p:Patient)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p

// EMPIs for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(empiP:Identifier)
	, (empiP)-[:is_instance_of]->(empiClass)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , COLLECT(DISTINCT empiP.value) AS PATIENT_EMPIS

// MRNS for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(mrnP:Identifier)
	, (mrnP)-[:is_instance_of]->(mrnClass)
	, (mrnP)-[:was_created_by]->(hupFacility)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
    , COLLECT(DISTINCT mrnP.value) AS PATIENT_HUP_MRNS

// encounter codes for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(encounterCodeP:Identifier)
	, (encounterCodeP)-[:is_instance_of]->(ecClass)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
    , PATIENT_HUP_MRNS
    , COLLECT(DISTINCT encounterCodeP.value) AS PATIENT_ENCOUNTER_CODES

// packet identifiers for patient p
OPTIONAL MATCH 
	(p)-[:is_identified_by]->(packetIdP:Identifier)
	, (packetIdP)-[:is_instance_of]->(packetIdClass)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
	, PATIENT_HUP_MRNS
	, PATIENT_ENCOUNTER_CODES
	, COLLECT(DISTINCT packetIdP.value) AS PATIENT_PACKET_IDS
    
//
// unmatched patients 
//
OPTIONAL MATCH (qiv)-[:identifies]->(encUnmatched:BiobankEncounter)
, (encUnmatched)-[:is_associated_with]->(pts:BiobankPtsRecord)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
	, PATIENT_HUP_MRNS
	, PATIENT_ENCOUNTER_CODES
	, PATIENT_PACKET_IDS
    , COLLECT(properties(pts)) AS UNMATCHED_CRF_DATA

//
// multi-match matients
//
OPTIONAL MATCH (qiv)-[:identifies]->(encMultiMatch:BiobankEncounter)
, (encMultiMatch)-[:is_associated_with]->(mmr:EncounterMultiMatchRecord)
WITH 
	empiClass, mrnClass, ecClass, packetIdClass
    , hupFacility, cgiProtocol
	, qiv, id, idClass
    , p
    , PATIENT_EMPIS
	, PATIENT_HUP_MRNS
	, PATIENT_ENCOUNTER_CODES
	, PATIENT_PACKET_IDS
    , UNMATCHED_CRF_DATA
    , COLLECT(properties(mmr)) AS MULTIMATCH_PATIENT_DATA

// return
RETURN 
	qiv.value AS IDENTIFIER_VALUE
	, qiv.cleanValue AS IDENTIFIER_VALUE_CLEAN

    , COUNT(id) AS NUM_MATCHED_IDENTIFIERS
    , COLLECT(id.value) AS MATCHED_IDENTIFIERS
    , COLLECT(id.value + ":" + idClass.name) AS MATCHED_IDENTIFIER_CLASSES

	, CASE ID(p) WHEN NULL THEN '' ELSE ID(p) END AS TMP_PID
    
    , PATIENT_EMPIS
    , PATIENT_HUP_MRNS
    , PATIENT_ENCOUNTER_CODES
    , PATIENT_PACKET_IDS
    
    , UNMATCHED_CRF_DATA
    , MULTIMATCH_PATIENT_DATA

ORDER BY qiv.value
LIMIT 100














MATCH (pp:PatientWithIdProblems)
, (pp)-[:is_identified_by]->(mrnCCH:Identifier)-[:is_instance_of]->(:IdentifierClass {name:"mrn"})-[:was_created_by]->(:IdentifierFacility {name:"CCH"})
RETURN pp, mrnCCH 
LIMIT 25


MATCH (pkPatientIdClass:IdentifierClass {name:"pk_patient_id"})
, (mrnClass:IdentifierClass {name:"mrn"})
, (facilityCCH:IdentifierFacility {name:"CCH"})
, (facilityPMC:IdentifierFacility {name:"PMC"})
, (facilityHUP:IdentifierFacility {name:"HUP"})
, (facilityPAH:IdentifierFacility {name:"PAH"})
WITH pkPatientIdClass, mrnClass, facilityCCH, facilityPMC, facilityHUP, facilityPAH
MATCH (pp:PatientWithIdProblems) // n=388

MATCH (pp)-[:is_identified_by]->(pkPatientId:Identifier)
, (pkPatientId)-[:is_instance_of]->(pkPatientIdClass)

OPTIONAL MATCH (pp)-[:is_identified_by]->(mrnCCH:Identifier)
, (mrnCCH)-[:is_instance_of]->(mrnClass)
, (mrnCCH)-[:was_created_by]->(facilityCCH)

OPTIONAL MATCH (pp)-[:is_identified_by]->(mrnPMC:Identifier)
, (mrnPMC)-[:is_instance_of]->(mrnClass)
, (mrnPMC)-[:was_created_by]->(facilityPMC)

OPTIONAL MATCH (pp)-[:is_identified_by]->(mrnHUP:Identifier)
, (mrnHUP)-[:is_instance_of]->(mrnClass)
, (mrnHUP)-[:was_created_by]->(facilityHUP)

OPTIONAL MATCH (pp)-[:is_identified_by]->(mrnPAH:Identifier)
, (mrnPAH)-[:is_instance_of]->(mrnClass)
, (mrnPAH)-[:was_created_by]->(facilityPAH)

RETURN ID(pp) AS PATIENT_NODE_ID
, pkPatientId.value AS PK_PATIENT_ID
, COLLECT(mrnCCH.value) AS MRNS_CCH
, COLLECT(mrnPMC.value) AS MRNS_PMC
, COLLECT(mrnHUP.value) AS MRNS_HUP
, COLLECT(mrnHUP.value) AS MRNS_PAH
, pp.idsErr



RETURN COUNT(DISTINCT pp)






, (pp)-[:is_identified_by]->(mrnPMC:Identifier)
, (mrnCCH)-[:is_instance_of]->(mrnClass)
, (mrnCCH)-[:was_created_by]->(facilityPMC)

, (pp)-[:is_identified_by]->(mrnHUP:Identifier)
, (mrnCCH)-[:is_instance_of]->(mrnClass)
, (mrnCCH)-[:was_created_by]->(facilityHUP)

, (pp)-[:is_identified_by]->(mrnPAH:Identifier)
, (mrnCCH)-[:is_instance_of]->(mrnClass)
, (mrnCCH)-[:was_created_by]->(facilityPAH)
