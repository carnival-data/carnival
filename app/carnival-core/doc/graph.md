# Cypher Style Guide

## Labels

Labels are upper camel case.

```
:Patient
:LastName
```

## Properties

Properties are lower camel case.

```
(:Patient {lastName:<string>})
```

## Relationships

Relationships are lower snake case. They should always be verb phrases.

```
-[:has]->
-[:has_not]->
-[:is_part_of]->
```

# Glossary

### Controlled Instance

Descriptor for graph labels. Vertexes with VertexBuilder labels behave in a way that is conceptually similar to Singleton classes. They must have at least one unique property and the allowed values for these properties are defined by controlled vocabulary that is not expected to change very often. These vertices will usually only be created in the initial graph setup, the controlled vocabulary will be maintained in Carnival (perhaps via enums) and the Carnival graph integrity QC will check that the controlled vocabulary matches the existing instances in the graph.

An example of this is the label IdentifierClass. There is a static set of identifier types that can be used to represent patients and encounters (EMPIs, MRNs, collection packet IDs, etc). These identifier types will be defined by an enum elsewhere. This enum will be referenced during the graph set-up phase to create the IdentifierClass instances.

# Graph Implementation

## Constraints

Property uniqueness constraints can be enforced via [Neo4J graph constraints](http://neo4j.com/docs/developer-manual/current/cypher/schema/constraints/).

Property existence constraints and relationship constraints cannot be enforced at a schema level, so it would be possible to add data to the graph that violates these contraints. QC methods will be written to check if an existing graph meets these conditions.

# Graph Model

## Patients

### Patient

```
(:Patient)
```

#### Relationships

```
(:Patient) -[:is_identified_by]-> (:Identifier)
```

## Encounters, Protocols, and Case Report Forms

![Encounters Model](graphviz/encounters.png?raw=true)

### BiobankEncounter

```
(:BiobankEncounter {encounterDate:<date?>})
```

#### Property Constraints

| Property      | Required | Unique | Default |
| ------------- | -------- | ------ | ------- |
| encounterDate | no       | no     |

#### Relationships

```
(:BiobankEncounter) -[:is_identified_by]-> (:Identifier)
(:BiobankEncounter) -[:is_under_protocol]-> (:Protocol)
(:BiobankEncounter) -[:is_under_consent]-> (:CaseReportForm)
(:BiobankEncounter) -[:participated_in_form_filling]-> (:CaseReportForm)
```

#### Relationship Constraints

The encounter may or may not have participated in form filling.

```
(:BiobankEncounter) -[:participated_in_crf_filling]-> (:Crf)
```

### Encounter Group

An encounter group is a generic grouping of encounters. The semantics of the group, why and how it was created, are defined elsewhere.

```
(:EncounterGroup)
```

#### Relationships

```
(:Encounter) -[:is_member_of]-> (:EncounterGroup)
(:Encounter) -[:is_not_member_of]-> (:EncounterGroup)
```

### Protocol

Research protocol under biobank encounters occur.
Protocol instances are unique by name, and should be created during the initial graph bootstrap step.

```
(:BiobankProtocol {name:<string>, futureEmrAlwaysAllowed:<boolean>})
```

#### Property Constraints

| Property               | Required | Unique | Default |
| ---------------------- | -------- | ------ | ------- |
| name                   | yes      | yes    |
| futureEmrAlwaysAllowed | yes      | no     | true    |

### Case Report Form

An instance that represents CRFs that were collected during a BiobankEncounter.

```
(:CaseReportForm {futureEmrAllowed:<boolean>})
```

#### Property Constraints

| Property         | Required | Unique | Default |
| ---------------- | -------- | ------ | ------- |
| futureEmrAllowed | no       | no     | true    |

#### Relationships

```
(:CaseReportForm) -[:is_under_protocol]-> (:Protocol)
```

## Patient and Encounter Identifiers

![Identifiers Model](graphviz/identifiers.png?raw=true)

### IdentifierClass (VertexBuilder)

Instances with this label represent the types of identifiers used to represent patients or biobank encounters. Examples include EMPI or MRN for patients, and collection encounter id and collection packet id for encounters.
IdentifierClass instances are unique by name, and should be created during the initial graph bootstrap step.

```
(:IdentifierClass {name:<string>, hasScope:<boolean>, hasCreationFacility:<boolean>})
```

#### Property Constraints

| Property            | Required | Unique | Default |
| ------------------- | -------- | ------ | ------- |
| name                | yes      | yes    |
| hasScope            | yes      | no     | false   |
| hasCreationFacility | yes      | no     | false   |

### IdentifierFacility (VertexBuilder)

Used with MRNs to indicate the facility where the MRN was generated (for example 'HUP' or 'PMC')
IdentifierFacility instances are unique by name, and should be created during the initial graph bootstrap step.

```
(:IdentifierFacility {name:<string>})
```

#### Property Constraints

| Property | Required | Unique |
| -------- | -------- | ------ |
| name     | yes      | yes    |

### IdentifierScope

Required for identifiers that have an identifierClass where `volitile == true`. Indicates the scope for which the identifier is valid.

```
(:IdentifierScope {name:<string>})
```

#### Property Constraints

| Property | Required | Unique |
| -------- | -------- | ------ |
| name     | yes      | yes    |

### Identifier

An identifier for an instance of an encounter or patient.

```
(:Identifier {value:<string>})
```

#### Property Constraints

| Property | Required | Unique |
| -------- | -------- | ------ |
| value    | yes      | no     |

#### Relationships

```
(:Identifier) -[:is_instance_of]-> (:IdentifierClass)
(:Identifier) -[:was_created_by]-> (:IdentifierFacility)
(:Identifier) -[:is_scoped_by]-> (:IdentifierScope)
```

#### Relationship Constraints

All identifiers must have an identifierClass.

```
(:Identifier) -[:is_instance_of]-> (:IdentifierClass)
```

If the identifierClass.hasCreationFacility == true, then there must be an identifierFacility attached to any identifier that is an instance of that identifier class.

```
MATCH
  (id:Identifier) -[:is_instance_of]-> (idc:hasCreationFacility {hasScope: true}),
  (id:Identifier) -[:was_created_by]-> (idf:IdentifierFacility)
```

If the identifierClass.hasScope == true, then there must be an identifierScope attached to any identifier that is an instance of that identifier class.

```
MATCH
  (id:Identifier) -[:is_instance_of]-> (idc:IdentifierClass {hasScope: true}),
  (id:Identifier) -[:is_scoped_by]-> (ids:IdentifierScope)
```

## Life Process

[![Life Process](graphviz/life_process.png?raw=true)](graphviz/life_process.png?raw=true)

Each patient participates in a life process, which is used as an organizational unit to relate things like dates of birth and death with a patient. The graphic above depicts the entities necessary to compute the Death event of a patient's life process. The Birth event will have a similar structure connected to data source data.

## Phenotype Codes

### Code Ref

A code reference is an individual reference to a code in a coding system or a wildcard reference to multiple codes. For example,

- '1.1' is an individual reference to the code 1.1
- '1.\*' is a wildcard reference that would map to 1.1, 1.2, 1.3, etc...

Each code reference value is associated with a coding system, examples of which include 'ICD9' and 'CPT'. Code references are not singletons and are not direct instantiations of codes from a coding system. They are references to those codes, which can be used as componenents to a query or other automated process that links entities to code assignments.

```
(:CodeRef {system:<string>, value:<string>})
```

#### Property Constraints

| Property | Required | Unique |
| -------- | -------- | ------ |
| system   | yes      | no     |
| value    | yes      | no     |

### Code Group

A code group is simply an arbitrary grouping of code references. For example, Barrett's Esophagus is identified by the following ICD 10 codes: K22.70, K22.719, K22.711. These three codes could comprise a code group with the name 'barrets'.

```
(:CodeGroup {name:<string>})
```

#### Property Constraints

| Property | Required | Unique |
| -------- | -------- | ------ |
| name     | yes      | no     |

#### Relationships

```
(:CodeGroup) -[:has_part]-> (:CodeRef)
```

## CodedPhenotype

CodedPhenotype refers to the phenotype codes developed by (TODO: include reference). A CodedPhenotype is very similar to a CodeGroup.

```
(:CodedPhenotype {name:String})
```

#### Relationships

```
(:CodedPhenotype {name:String}) -[has_part]-> (:CodeRef)
(:Patient) -[:has_num_ocurrences_of {count:Integer}]-> (:CodedPhenotype)
```

#### Inferences

The following inferred relationships are provided to facilitate searching.

```
(:Patient) -[:has_coded_phenotype {status:String, pheCodeProcId:Integer}]-> (:CodedPhenotype)
```

where the status is one of: ['y', 'n', 'na'].

## PhecodeAssignmentReaperProcess

Refers to the PhecodeAssignmentReaperProcess that associates patients with CodedPhenotypes.

```
(:PhecodeAssignmentReaperProcess {date:Long})
```

#### Relationships

```
(:Patient) -[:is_input_of]-> (:PhecodeAssignmentReaperProcess)
```

## Patient Group

A patient group is a generic grouping of patients. The semantics of the group, why and how it was created, are defined elsewhere.

```
(:PatientGroup)
```

#### Relationships

```
(:Patient) -[:is_member_of]-> (:PatientGroup)
(:Patient) -[:is_not_member_of]-> (:PatientGroup)
```

## Planned Process

Similar to [planned process](http://purl.obolibrary.org/obo/OBI_0000011) in the Ontology for Biomedical Investigations.

```
(:PlannedProcess {name:<string>})
```

#### Property Constraints

| Property | Required | Unique |
| -------- | -------- | ------ |
| name     | no       | no     |

## Reaper Process

ReaperProcess is a subclass of PlannedProcess and a superclass for instances that denote the execution of a specific reaper.

#### Relationships

```
(:ReaperProcess) -[:is_instance_of]-> (:PlannedProcess)
```

## Reaper Process Properties _(proposed)_

ReaperProcessProperties represent properties gathered by the reaper pertaining to a specific entity:

#### Relationships

```
(:ReaperProcessProperties) -[:is_output_of]-> (:ReaperProcess)
(:ReaperProcessProperties) -[:pertains_to]-> (:Thing)
```

## Reaper Process Evidence _(proposed)_

ReaperProcessEvidence represents evidence for statements created by a reaper process.

```
(:ReaperProcessEvidence {edge:<string>})
```

#### Property Constraints

| Property | Required | Unique |
| -------- | -------- | ------ |
| edge     | yes      | no     |

#### Relationships

```
(:ReaperProcessEvidence) -[:is_output_of]-> (:ReaperProcess)
(:ReaperProcessEvidence) -[:has_subject]-> (:Thing)
(:ReaperProcessEvidence) -[:has_object]-> (:Thing)
```

## Diagnosis Ever Never Reaper Process

The DxEverNeverReaper inspects the medical record for each patient and assigns them to a group if they have a diagnosis code assignment for any of a given set of code refs.

![Dx Ever Never Reaper Process Model](graphviz/dx_ever_never_reaper_process.png?raw=true)

#### Relationships

```
(:DxEverNeverReaperProcess) -[:is_instance_of]-> (:ReaperProcess)
(:CodeGroup) -[:is_input_of]-> (:DxEverNeverReaperProcess)
(:PatientGroup) -[:is_output_of]-> (:DxEverNeverReaperProcess)
```

## Encounter BMI (first pass)

Height and weight measurements for a patient can be taken and recorded at the time of a biobank encounter, and during healthcare encounters. The encounter BMI reapers attach calculate and assign height, weight and bmi information to a particular biobank encounter. These data include the height, weight and calculated bmi value from the CRF, and summary height, weight and bmi information taken from the healthcare encounters a patient has had before the Biobank Encounter.

![Encounter BMI Reaper Model](graphviz/encounter_bmi_reaper.png?raw=true)

#### Relationships

```
(:BiobankEncounter) -[:has_bmi_data]-> (:CrfBmiEncounterData)
(:BiobankEncounter) -[:has_bmi_data]-> (:SummaryBmiEncounterData)
```
