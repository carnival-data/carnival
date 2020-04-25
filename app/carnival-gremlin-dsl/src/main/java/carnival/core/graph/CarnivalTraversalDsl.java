package carnival.core.graph;



import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.apache.tinkerpop.gremlin.*;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.util.*;
import org.apache.tinkerpop.gremlin.groovy.loaders.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.*;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.*;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
//import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import static org.apache.tinkerpop.gremlin.process.traversal.P.*;



/**
 * CarnivalTraversalDsl defined gremlin traversals useful in a carnival graph.
 *
 * Comment conventions:
 *
 * (*)                   a vertex
 * ((*))                 a vertex that will be returned
 * (foo)                 a vertex with label 'foo'
 * (foo|name="stuff")    a vertex with label 'foo' and property "name" with value "stuff"
 * --[edge_label]-->     an edge with label 'edge_label'
 *
 */
@GremlinDsl(traversalSource = "carnival.core.graph.CarnivalTraversalSourceDsl")
public interface CarnivalTraversalDsl<S, E> extends GraphTraversal.Admin<S, E> {


    ///////////////////////////////////////////////////////////////////////////
    // GENERAL PURPOSE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * .inVL('e', 'o') --> .in('e').hasLabel('o')
     *
     */
    public default GraphTraversal<S, Vertex> inVL(String edgeLabel, String inVertexLabel) {
        return in(edgeLabel).hasLabel(inVertexLabel);
    }


    /**
     * .outVL('e', 'o') --> .out('e').hasLabel('o')
     *
     */
    public default GraphTraversal<S, Vertex> outVL(String edgeLabel, String outVertexLabel) {
        return out(edgeLabel).hasLabel(outVertexLabel);
    }



    ///////////////////////////////////////////////////////////////////////////
    // ENCOUNTERS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    public default GraphTraversal<S, Vertex> withActivity(String name) {
    	// I could not get either of the following to work... though it seemed like I should
    	// have been able to.  Was getting an error about an array list not being able to be
    	// case as edge.  Could not figure it out.
        //return as("a").out("has_activity").has("name", name).select("a");
        //return as("a").outE("has_activity").as("b").inV().has("name", name).select("b").outV().unfold();

        return match(__.as("a").out("has_activity").has("name", name)).select("a");
    }




    ///////////////////////////////////////////////////////////////////////////
    // PATIENTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    public default GraphTraversal<S, Vertex> memberOfProcessGroup(String processLabel, String groupName) {
        return out("is_member_of").hasLabel("PatientGroup").out("is_output_of").hasLabel(processLabel).has("name", groupName);
    }


    /**
     *
     *
     */
    public default GraphTraversal<S, Vertex> notMemberOfProcessGroup(String processLabel, String groupName) {
        return out("is_not_member_of").hasLabel("PatientGroup").out("is_output_of").hasLabel(processLabel).has("name", groupName);
    }



    ///////////////////////////////////////////////////////////////////////////
    // IDS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * verticies with the label 'Identifier' associated with the input that have class 'empi'
     *
     *
     * (*) --[is_identified_by]--> (id:Identifier)
     */
    public default GraphTraversal<S, Vertex> empis() {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b")
            .out("is_instance_of").hasLabel("IdentifierClass").has("name", "empi")
        ).select("b");
    }


    /** */
    public default GraphTraversal<S, Vertex> patientStudyCodes(String studyName) {
        return out("is_identified_by")
            .hasLabel("Identifier").as("id")
            .out("is_instance_of")
            .hasLabel("IdentifierClass")
            .has("name", "patient_study_code")
            .select("id")
            .out("is_scoped_by")
            .hasLabel("Study")
            .has("name", studyName)
        .select("id");
    }


    /** */
    public default GraphTraversal<S, Vertex> patientStudyCodes(Vertex studyV) {
        return out("is_identified_by")
            .hasLabel("Identifier").as("id")
            .out("is_instance_of")
            .hasLabel("IdentifierClass")
            .has("name", "patient_study_code")
            .select("id")
            .out("is_scoped_by")
            .is(studyV)
        .select("id");
    }

    
    /**
     * verticies with the label 'Identifier' associated with the input.
     *
     *
     * (*) --[is_identified_by]--> ((id:Identifier))
     */
    public default GraphTraversal<S, Vertex> identifiers() {
        return out("is_identified_by").hasLabel("Identifier");
    }



    /**
     * verticies with the label 'Identifier' associated with the input that have class 'mrn'
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='mrn')
     */
    public default GraphTraversal<S, Vertex> mrns() {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b")
            .out("is_instance_of").hasLabel("IdentifierClass").has("name", "mrn")
        ).select("b");
    }


    /**
     * verticies with the label 'Identifier' associated with the input that have class 'mrn' and a scope with name facilityName
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='mrn')
     *                   ((id)) --[was_created_by]--> (IdentifierFacility|name=facilityName)
     */
    public default GraphTraversal<S, Vertex> mrns(String facilityName) {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "mrn"),
            __.as("b").out("was_created_by").hasLabel("IdentifierFacility").has("name", facilityName)
        ).select("b");
    }


    /**
     * verticies with the label 'Identifier' associated with the input that have class 'mrn' and a scope with name facilityName
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='mrn')
     *                   ((id)) --[was_created_by]--> (IdentifierFacility|name=facilityName)
     */
    public default GraphTraversal<S, Vertex> mrns(Vertex facility) {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "mrn"),
            __.as("b").out("was_created_by").is(facility)
        ).select("b");
    }


    /**
     * verticies with the label 'Identifier' associated with the input that have the
     * provided class
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='arg')
     */
    public default GraphTraversal<S, Vertex> identifiersOfClass(String className) {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b")
            .out("is_instance_of").hasLabel("IdentifierClass").has("name", className)
        ).select("b");
    }


    /**
     * verticies with the label 'Identifier' associated with the input that have the
     * provided class
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='arg')
     */
    public default GraphTraversal<S, Vertex> identifiersOfClass(Vertex classVertex) {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b")
            .out("is_instance_of").is(classVertex)
        ).select("b");
    }


    /**
     *
     *
     */
    public default GraphTraversal<S, Vertex> isIdentifiedBy(String identifierClassName, Object identifierValue) {
        return match(   
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", identifierClassName),
            __.as("b").has("value", identifierValue)
        ).select("a");
    }


    /**
     *
     *
     */
    public default GraphTraversal<S, Vertex> isIdentifiedByWithFacility(String identifierClassName, String facilityName, Object identifierValue) {
        return match(   
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", identifierClassName),
            __.as("b").has("value", identifierValue),
            __.as("b").out("was_created_by").has("name", facilityName)
        ).select("a");
    }


    /**
     *
     *
     */
    public default GraphTraversal<S, Vertex> isIdentifiedByWithScope(String identifierClassName, String scopeName, Object identifierValue) {
        return match(   
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", identifierClassName),
            __.as("b").has("value", identifierValue),
            __.as("b").out("is_scoped_by").has("name", scopeName)
        ).select("a");
    }



    ///////////////////////////////////////////////////////////////////////////
    //
    // BIOBANK
    //
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // CONSENT
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public default GraphTraversal<S, Vertex> hasConsentEmrData() {
        return hasLabel("BiobankEncounter").as("hasConsentEmrDataEnc")
            .has("hasConsentEmrData", true)
            .select("hasConsentEmrDataEnc");
    }


    /** */
    public default GraphTraversal<S, Vertex> isNotEmrUploadable() {
        return hasLabel("BiobankEncounter").as("isNotEmrUploadableEnc")
            .not(__.has("hasConsentEmrData", true))
            .select("isNotEmrUploadableEnc");
    }


    /** */
    public default GraphTraversal<S, Vertex> isUnenrolled() {
        return hasLabel("BiobankEncounter").as("isUnenrolledEnc")
            .has("isUnenrolled", true)
            .select("isUnenrolledEnc");
    }


    /** */
    public default GraphTraversal<S, Vertex> isNotUnenrolled() {
        return hasLabel("BiobankEncounter").as("isNotUnenrolledEnc")
            .not(__.has("isUnenrolled", true))
            .select("isNotUnenrolledEnc");
    }


    /** */
    public default GraphTraversal<S, Vertex> isConsentEncounter() {
        return hasLabel("BiobankEncounter").as("iceEnc")
            .out("participated_in_form_filing")
            .hasLabel("CaseReportForm")
            .select("iceEnc");
    }

    /** */
    public default GraphTraversal<S, Vertex> futureEmrAlwaysAllowed() {
        return hasLabel("Protocol").as("feaaProt")
            .has("futureEmrAlwaysAllowed", true)
            .select("feaaProt");
    }


    /** */
    public default GraphTraversal<S, Vertex> notFutureEmrAlwaysAllowed() {
        return hasLabel("Protocol").as("nfeaaProt")
            .not(__.has("futureEmrAlwaysAllowed", true))
            .select("nfeaaProt");
    }


    /** */
    public default GraphTraversal<S, Vertex> doesNotHaveFutureEmrConsent() {
        return hasLabel("BiobankEncounter").as("dnhfecEnc")
            .not(
                __.or(
                    __.out("is_under_protocol").has("futureEmrAlwaysAllowed", true),
                    __.and(
                        __.out("participated_in_form_filing").has("futureEmrAllowed", true),
                        __.out("is_under_protocol").has("futureEmrAlwaysAllowed", false)
                    )
                )
            ).select("dnhfecEnc");
    }


    /** */
    public default GraphTraversal<S, Vertex> hasFutureEmrConsent() {
        return hasLabel("BiobankEncounter").as("hfecEnc")
            .or(
                __.out("is_under_protocol").has("futureEmrAlwaysAllowed", true),
                __.and(
                    __.out("participated_in_form_filing").has("futureEmrAllowed", true),
                    __.out("is_under_protocol").has("futureEmrAlwaysAllowed", false)
                )
            )
            .select("hfecEnc");
    }


    /** */
    public default GraphTraversal<S, Vertex> belongsToPatient() {
        return hasLabel("BiobankEncounter")
            .in("participated_in_encounter").hasLabel("Patient");
    }



    ///////////////////////////////////////////////////////////////////////////
    // PATIENTS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public default GraphTraversal<S, Vertex> patientEncounters() {
        return hasLabel("Patient")
            .out("participated_in_encounter")
            .hasLabel("BiobankEncounter");
    }


    /**
     * All the encounters associated with a patient.
     *
     * (*:Patient) -[:participated_in_encounter]-> (enc:BiobankEncounter)
     */
    public default GraphTraversal<S, Vertex> encounters() {
        return hasLabel("Patient").out("participated_in_encounter").hasLabel("BiobankEncounter");
    }


    ///////////////////////////////////////////////////////////////////////////
    // ENCOUNTERS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public default GraphTraversal<S, Vertex> encounterProtocol() {
        return hasLabel("BiobankEncounter").out("is_under_protocol").hasLabel("Protocol");
    }



    ///////////////////////////////////////////////////////////////////////////
    // IDS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * verticies with the label 'Identifier' associated with the input that have class 'encounter_pack_id'
     *
     *
     * (*) --[is_identified_by]--> (id:Identifier)
     */
    public default GraphTraversal<S, Vertex> encounterPackIds() {
        return hasLabel("BiobankEncounter") 
            .match(
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b")
                .out("is_instance_of").hasLabel("IdentifierClass").has("name", "encounter_pack_id")
            ).select("b");
    }



    /**
     * verticies with the label 'Identifier' associated with the input that have class 'pk_patient_id'
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='pk_patient_id')
     */
    public default GraphTraversal<S, Vertex> pkPatientIds() {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b")
            .out("is_instance_of").hasLabel("IdentifierClass").has("name", "pk_patient_id")
        ).select("b");
    }


    /**
     * verticies with the label 'Identifier' associated with the input that have class 'pk_patient_id' and a scope with name scopeName
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='pk_patient_id')
     *                   ((id)) --[is_scoped_by]--> (IdentifierScope|name=scopeName)
     */
    public default GraphTraversal<S, Vertex> pkPatientIds(String scopeName) {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "pk_patient_id"),
            __.as("b").out("is_scoped_by").hasLabel("IdentifierScope").has("name", scopeName)
        ).select("b");
    }


    /**
     * verticies with the label 'Identifier' associated with the input that have class 'pk_patient_id' and a scope with name scopeName
     *
     *
     * (*) --[is_identified_by]--> ((id)) --[is_instance_of]--> (IdentifierClass|name='pk_patient_id')
     *                   ((id)) --[is_scoped_by]--> (IdentifierScope|name=scopeName)
     */
    public default GraphTraversal<S, Vertex> pkPatientIds(Vertex scope) {
        return match(
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "pk_patient_id"),
            __.as("b").out("is_scoped_by").is(scope)
        ).select("b");
    }



}
