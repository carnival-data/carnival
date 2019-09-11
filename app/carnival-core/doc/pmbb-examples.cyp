//-- Identifiers
//--------------
// nodes: all identifiers for all patients
MATCH (p:Patient) -[:is_identified_by]-> (pid:Identifier),
(pid) -[:is_instance_of]-> (pidClass:IdentifierClass)
RETURN p, pid, pidClass
LIMIT 10


// table: all identifiers for all patients
MATCH (p:Patient) -[:is_identified_by]-> (pid:Identifier),
(pid) -[:is_instance_of]-> (pidClass:IdentifierClass)
RETURN ID(p) AS patient_node, pid.value AS identifier, pidClass.name AS identifier_type
ORDER BY patient_node, identifier_type
LIMIT 10


// table: ids for each patient
MATCH (p:Patient)
OPTIONAL MATCH (p) -[:is_identified_by]-> (empi:Identifier) -[:is_instance_of]-> (:IdentifierClass {name:'empi'})
OPTIONAL MATCH (p) -[:is_identified_by]-> (pkPatientId:Identifier) -[:is_instance_of]-> (:IdentifierClass {name:'pk_patient_id'})
OPTIONAL MATCH (p) -[:is_identified_by]-> (packetId:Identifier) -[:is_instance_of]-> (:IdentifierClass {name:'encounter_pack_id'})
RETURN ID(p) AS patient_node, empi.value AS empi, pkPatientId.value AS pk_patient_id, packetId.value AS packet_id
LIMIT 10




//--Patient Biobank Encounter Count Exploration
//-------------------------------------
// patients who have participated in at least 3 biobank encounters
MATCH (p)-[:participated_in_encounter]->(enc:BiobankEncounter)
WITH p, count(enc) as encs ORDER BY encs desc
WHERE encs > 2
MATCH (p:Patient)-[:participated_in_encounter]->(e:BiobankEncounter)
RETURN p, encs, e

// table: patients with encounters under multiple protocols
MATCH (p:Patient)-[:participated_in_encounter]->(enc1:BiobankEncounter),
(enc1)-[:is_under_protocol]->(protocol1:Protocol)
WITH p, count(distinct protocol1) as protocolCount
WHERE protocolCount > 1
MATCH (p)-[:participated_in_encounter]->(enc:BiobankEncounter),
(enc)-[:is_under_protocol]->(protocol:Protocol)
RETURN p, id(p), protocolCount, enc, protocol

// table: top 20 patients with the most biobank encounters
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter) 
WITH p, COUNT(enc) as encs 
ORDER BY encs DESC 
RETURN p, encs LIMIT 20

// table: patient with the most encounters, and that patient's identifiers
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter) 
WITH p, COUNT(enc) as encs 
ORDER BY encs DESC LIMIT 1 
MATCH 
	(p)-[:is_identified_by]->(id),
	(id)-[:is_instance_of]->(idClass:IdentifierClass)
OPTIONAL MATCH (id)-[:is_scoped_by]->(idScope:IdentifierScope)
RETURN p, id, encs, idClass, idScope

// details of the patient with the second most most biobank encounters
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter) 
WITH p, COUNT(enc) as encs 
ORDER BY encs DESC SKIP 1 LIMIT 1
MATCH 
	(p)-[:participated_in_encounter]->(enc:BiobankEncounter),
	(enc)-[:is_under_protocol]->(protocol:Protocol)
//ORDER BY enc.encounterDate ASC
return p, enc, protocol




//--Demographics
//----------------
// table: PDS demographics for patients and the number of biobank encounters
MATCH (p:Patient)-[:has_demographics_summary]->(demo:PatientDemographicsSummary),
(p)-[:participated_in_encounter]->(enc:BiobankEncounter)
WITH  demo, count(enc) as numberEncounters
RETURN demo.EMR_CURRENT_AGE, demo.EMR_RACE_CODE, demo.EMR_RACE_HISPANIC_YN, numberEncounters


//--BMI
//----------------
// table -> graph: CRF and PDS BMI values associated with biobank encounters
MATCH (p:Patient)-[:participated_in_encounter]->(b:BiobankEncounter) 
MATCH (b)-[:has_bmi_data]->(summaryBmi:SummaryBmiData)
OPTIONAL MATCH (b)-[:has_bmi_data]->(crfBmi:CrfBmiData)
RETURN id(p), id(b), 
crfBmi.calculatedBmi as BIOBANK_CRF_BMI, 
toFloat(summaryBmi.BMI_RCMT__VALUE) as PDS_BMI,
b.encounterDate as BIOBANK_ENCOUNTER_DATE, 
summaryBmi.BMI_RCMT__ENC_DATE AS PDS_ENCOUNTER_DATE,
toFloat(summaryBmi.BMI_RCMT__ENC_RCMT_DAYDIFF)
//, p, b, summaryBmi, crfBmi

// graph: BMI data for all patient - biobank encounters
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter) 
MATCH (enc)-[:has_bmi_data]->(summaryBmi:SummaryBmiData)
OPTIONAL MATCH (enc)-[:has_bmi_data]->(crfBmi:CrfBmiData)
RETURN p, enc, summaryBmi, crfBmi

// graph: BMI data where there is no CRF BMI data
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter) 
MATCH (enc)-[:has_bmi_data]->(summaryBmi:SummaryBmiData)
WHERE NOT EXISTS ((enc)-[:has_bmi_data]->(:CrfBmiData))
RETURN p, enc, summaryBmi

// table: BMI CRF, EHR, and calculated normalized value
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter) 
MATCH (enc)-[:has_bmi_data]->(summaryBmi:SummaryBmiData)
OPTIONAL MATCH (enc)-[:has_bmi_data]->(crfBmi:CrfBmiData)
CALL apoc.when(crfBmi IS NOT NULL, 'RETURN crfBmi.calculatedBmi as BMI', 'RETURN summaryBmi.BMI_RCMT__VALUE as BMI', {crfBmi:crfBmi, summaryBmi:summaryBmi}) YIELD value
RETURN ID(p), crfBmi.calculatedBmi, summaryBmi.BMI_RCMT__VALUE, value.BMI AS BMI
LIMIT 5

// SET: set normalizedBmi based on simple logic
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter) 
MATCH (enc)-[:has_bmi_data]->(summaryBmi:SummaryBmiData)
OPTIONAL MATCH (enc)-[:has_bmi_data]->(crfBmi:CrfBmiData)
CALL apoc.do.when(crfBmi IS NOT NULL, 'SET enc.normalizedBmi = crfBmi.calculatedBmi', 'SET enc.normalizedBmi = summaryBmi.BMI_RCMT__VALUE', {crfBmi:crfBmi, summaryBmi:summaryBmi, enc:enc}) YIELD value
WITH p, enc
RETURN p, enc
LIMIT 5

// SET: unset normalizedBmi 
MATCH (enc:BiobankEncounter) 
REMOVE enc.normalizedBmi
RETURN enc
LIMIT 5


//--Inventory
//------------------
// patients with encounters that have serum and either DNA or >=2 buffy specimens
MATCH (p:Patient)-[:participated_in_encounter]->(enc:BiobankEncounter),
(enc)-[:has_specimens]->(encSpec:SpecimenSummary)
WHERE (toInteger(encSpec.NUM_DNA) >= 1 OR toInteger(encSpec.NUM_BUFFY) >= 2) and toInteger(encSpec.NUM_SERUM) >= 1
//RETURN p, id(p), id(enc), enc, encSpec
RETURN id(p), enc.encounterDate, encSpec.NUM_DNA, encSpec.NUM_BUFFY, encSpec.NUM_SERUM
LIMIT 10




//--Phenotyping
//--------------
// prev-mi and inc-mi
MATCH (p:Patient) -[:is_member_of]-> (incMiGroup:PatientGroup {name:'INC-MI'} ),
(p) -[:is_member_of]-> (prevMiGroup:PatientGroup {name:'PREV-MI'} )
RETURN p, incMiGroup, prevMiGroup

// inc-mi only
MATCH (p:Patient) -[:is_member_of]-> (incMiGroup:PatientGroup {name:'INC-MI'} )
, (p) -[:is_not_member_of]-> (prevMiGroup:PatientGroup {name:'PREV-MI'} )
RETURN p, incMiGroup
LIMIT 10

// prev-mi only
MATCH (p:Patient) -[:is_not_member_of]-> (incMiGroup:PatientGroup {name:'INC-MI'} )
, (p) -[:is_member_of]-> (prevMiGroup:PatientGroup {name:'PREV-MI'} )
RETURN p, prevMiGroup
LIMIT 10

// prev-mi only, at least 100
MATCH (p:Patient) -[:is_not_member_of]-> (incMiGroup:PatientGroup {name:'INC-MI'} )
, (p) -[:is_member_of]-> (prevMiGroup:PatientGroup {name:'PREV-MI'} )
, (p) -[:has_demographics_summary]-> (demo:PatientDemographicsSummary)
WHERE toInteger(demo.EMR_CURRENT_AGE) >= 100
RETURN p, prevMiGroup
LIMIT 10

// prev-mi only, at least 100, bmi > 26
MATCH (p:Patient) -[:is_not_member_of]-> (incMiGroup:PatientGroup {name:'INC-MI'} )
, (p) -[:is_member_of]-> (prevMiGroup:PatientGroup {name:'PREV-MI'} )
, (p) -[:has_demographics_summary]-> (demo:PatientDemographicsSummary)
, (p) -[:participated_in_encounter]-> (enc:BiobankEncounter)
, (enc) -[:has_bmi_data]-> (crfBmi:CrfBmiData)
WHERE toInteger(demo.EMR_CURRENT_AGE) >= 100
AND toFloat(crfBmi.calculatedBmi) > 26
RETURN p, prevMiGroup
LIMIT 10


// create a phenotype:
// prev-mi only, at least 100, bmi > 26
MATCH (p:Patient) -[:is_not_member_of]-> (incMiGroup:PatientGroup {name:'INC-MI'} )
, (p) -[:is_member_of]-> (prevMiGroup:PatientGroup {name:'PREV-MI'} )
, (p) -[:has_demographics_summary]-> (demo:PatientDemographicsSummary)
, (p) -[:participated_in_encounter]-> (enc:BiobankEncounter)
, (enc) -[:has_bmi_data]-> (crfBmi:CrfBmiData)
WHERE toInteger(demo.EMR_CURRENT_AGE) >= 100
AND toFloat(crfBmi.calculatedBmi) > 26
MERGE (p) -[:expresses_phenotype]-> (phen:RarePhenotype {name:'rare-phenotype-1'})
RETURN p, prevMiGroup, phen


// create a phenotype:
// prev-mi only, older than 90, bmi > 28
MATCH (p:Patient) -[:is_not_member_of]-> (incMiGroup:PatientGroup {name:'INC-MI'} )
, (p) -[:is_member_of]-> (prevMiGroup:PatientGroup {name:'PREV-MI'} )
, (p) -[:has_demographics_summary]-> (demo:PatientDemographicsSummary)
, (p) -[:participated_in_encounter]-> (enc:BiobankEncounter)
, (enc) -[:has_bmi_data]-> (crfBmi:CrfBmiData)
WHERE toInteger(demo.EMR_CURRENT_AGE) >= 95
AND toFloat(crfBmi.calculatedBmi) > 27
MERGE (p) -[:expresses_phenotype]-> (phen:RarePhenotype {name:'rare-phenotype-2'})
RETURN p, prevMiGroup, phen

// match on phenotypes
MATCH (p1:Patient) -[:expresses_phenotype]-> (ph1:RarePhenotype {name:'rare-phenotype-1'})
MATCH (p2:Patient) -[:expresses_phenotype]-> (ph2:RarePhenotype {name:'rare-phenotype-2'})
//WHERE p1 = p2
RETURN p1, ph1, p2, ph2






