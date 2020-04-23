package carnival.clinical.graph.domain



import java.text.SimpleDateFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.step.*
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import static org.apache.tinkerpop.gremlin.process.traversal.step.TraversalOptionParent.Pick.*
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import org.apache.tinkerpop.gremlin.neo4j.structure.*
import carnival.core.*
import carnival.core.graph.CarnivalTraversalSource
import carnival.core.graph.*
import carnival.util.KeyType
import static carnival.util.DataTable.toIdValue


/**
 *
 */
class PatientGroup {

    ///////////////////////////////////////////////////////////////////////////
    // LOGGING
    ///////////////////////////////////////////////////////////////////////////

    static final Logger sqllog = LoggerFactory.getLogger('sql')
    static final Logger elog = LoggerFactory.getLogger('db-entity-report')
    static final Logger log = LoggerFactory.getLogger(PatientGroup)



    ///////////////////////////////////////////////////////////////////////////
    // FACTORY
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    static public Vertex createPatientGroupFromIdValues(Graph graph, String idClassName, Collection<String> idVals) {
        assert idClassName
        def keyType = Enum.valueOf(KeyType, idClassName.toUpperCase())
        if (!keyType) throw new IllegalArgumentException("unsupported key type: $idClassName")
        createPatientGroupFromIdValues(graph, keyType, idVals)
    }


    /**
     * Create a patient group vertex that includes all the patients referenced
     * by the given collection of identifiers.
     *
     * @return The Vertex of the PatientGroup created.
     *
     */
    static public Vertex createPatientGroupFromIdValues(Graph graph, KeyType keyType, Collection<String> idVals) {
        if (![KeyType.ENCOUNTER_PACK_ID, KeyType.EMPI, KeyType.PK_PATIENT_ID, KeyType.GENERIC_PATIENT_ID, KeyType.MRN].contains(keyType))
            throw new IllegalArgumentException("unsupported key type: $keyType")

        def g = graph.traversal(CarnivalTraversalSource.class)

        def idClassName = keyType.name().toLowerCase()

        /*
        (:BiobankEncounter) -[:is_identified_by]-> (:Identifier)
        (:Identifier {value})
        */      
        def cq = """
            MATCH (e:Patient) -[:is_identified_by]-> (i:Identifier),
            (i) -[:is_instance_of]-> (c:IdentifierClass)
            WHERE i.value IN SUB_ENTITY_IDS
            AND c.name = '${idClassName}'
            RETURN e, i
        """.replaceAll('SUB_ENTITY_IDS', idVals.collect({"'${toIdValue(it)}'"}).toString())
        sqllog.info "$cq"

        def res = cypher(graph, cq).toList()

        def eVs = res*.e
        def iVs = res*.i

        def graphIdVals = iVs*.value('value')
        //log.debug "idVals: $idVals"
        //log.debug "graphIdVals: $graphIdVals"
        //log.debug "${idVals.size()} ${graphIdVals.size()}"
        //assert graphIdVals.size() == idVals.size()
        def numIdsFromGraph = graphIdVals.size()
        def numInputIds = idVals.size()
        log.info "of the $numInputIds input ids, $numIdsFromGraph were matched"

        def distinctPatVs = eVs.unique { it.id() }
        log.info "the $numInputIds ids provided identify ${distinctPatVs.size()} distinct patients"

        def gV = graph.addVertex(T.label, "PatientGroup")
        distinctPatVs.each { eV ->
            eV.addEdge('is_member_of', gV)
        }

        return gV
    }



    ///////////////////////////////////////////////////////////////////////////
    // PATIENT LOOKUPS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    static public Map<String,Vertex> patientLookupGenericId(Graph graph, String facilityName, Collection<Vertex> patientVerts = []) {
        log.trace "patientLookupPkPatientId facilityName:$facilityName"
        assert graph
        assert facilityName

        def g = graph.traversal(CarnivalTraversalSource.class)

        Map<String,Vertex> patientLookup = new HashMap<String,Vertex>()

        def identifierFacility
        if (facilityName) {
            identifierFacility = g.V()
                .hasLabel('IdentifierFacility')
                .has('name', facilityName)
                .tryNext()
        }
        if (!identifierFacility.isPresent()) throw new IllegalArgumentException("could not find facility: $facilityName")
        identifierFacility = identifierFacility.get()

        def pats = patientVerts ?: g.patients()
        pats.each { pV ->
            def pidVs = g.V(pV)
                .out('is_identified_by').hasLabel('Identifier').as('pid')
                .and(
                    __().out('is_instance_of').has("name", "generic_patient_id"),
                    __().out("was_created_by").is(identifierFacility)
                )
                .select('pid')
                .toList()
            if (pidVs.size() > 1) log.warn "patient $pV has generic ids(${pidVs.size()}): ${pidVs*.value('value')}"
            pidVs.each { pidV ->
                def pidStr = pidV.value('value')
                if (patientLookup.containsKey(pidStr)) {
                    log.warn "$pidStr already in map. skipping."
                    return
                }
                patientLookup.put(pidStr, pV)
            }
        }

        /*def lookupSize = patientLookup.size()
        def numVerts = pats.size()
        assert lookupSize == numVerts*/

        return patientLookup
    }


    /**
     *
     *
     */
    static public Map<Integer,Vertex> patientLookupMrn(Graph graph, String facilityName, Collection<Vertex> patientVerts = []) {
        log.trace "patientLookupPkPatientId facilityName:$facilityName"
        assert graph
        assert facilityName

        def g = graph.traversal(CarnivalTraversalSource.class)

        Map<Integer,Vertex> patientLookup = new HashMap<Integer,Vertex>()

        def identifierFacility
        if (facilityName) {
            identifierFacility = g.V()
                .hasLabel('IdentifierFacility')
                .has('name', facilityName)
                .tryNext()
        }
        if (!identifierFacility.isPresent()) throw new IllegalArgumentException("could not find facility: $facilityName")
        identifierFacility = identifierFacility.get()

        def pats = patientVerts ?: g.patients()
        pats.each { pV ->
            def pidVs = g.V(pV)
                .out('is_identified_by').hasLabel('Identifier').as('pid')
                .and(
                    __().out('is_instance_of').has("name", "mrn"),
                    __().out("was_created_by").is(identifierFacility)
                )
                .select('pid')
                .toList()
            //if (pidVs.size() > 1) log.warn "patient $pV has mrns(${pidVs.size()}): $pidVs"
            pidVs.each { pidV ->
                def pidStr = pidV.value('value')
                if (!pidStr.isInteger()) {
                    log.warn "skpping mrn $pidStr: it is not parseable to an integer"
                    return
                }
                if (patientLookup.containsKey(pidStr)) {
                    log.warn "$pidStr already in map. skipping."
                    return
                }

                patientLookup.put(pidStr.toInteger(), pV)
            }
        }

        /*def lookupSize = patientLookup.size()
        def numVerts = pats.size()
        assert lookupSize == numVerts*/

        return patientLookup
    }



    /**
     *
     *
     */
    static private Map<String,Vertex> patientLookupScopedIdentifier(Graph graph, String idClass, String scopeName, Collection<Vertex> patientVerts = []) {
        log.trace "patientLookupScopedIdentifier idClass:$idClass scopeName:$scopeName patientVerts:${patientVerts.size()}"

        def g = graph.traversal(CarnivalTraversalSource.class)
        def identifierScope = g.V()
            .hasLabel('IdentifierScope')
            .has('name', scopeName)
            .tryNext()

        if (!identifierScope.isPresent()) throw new IllegalArgumentException("could not find scope: $scopeName")

        return patientLookupScopedIdentifier(g, idClass, identifierScope.get(), patientVerts)
    }


    /**
     *
     *
     */
    static private Map<String,Vertex> patientLookupScopedIdentifier(
        GraphTraversalSource g,
        String idClass, 
        Vertex identifierScope, 
        Collection<Vertex> patientVerts = []
    ) {
        log.trace "patientLookupScopedIdentifier idClass:$idClass identifierScope:$identifierScope patientVerts:${patientVerts.size()}"
        assert g
        assert idClass
        assert identifierScope

        Map<String,Vertex> patientLookup = new HashMap<String,Vertex>()

        def pats = patientVerts ?: g.patients()
        pats.each { pV ->
            def idvs = g.V(pV)
                .out('is_identified_by').hasLabel('Identifier').as('pid')
                .and(
                    __().out('is_instance_of').has("name", idClass),
                    __().out("is_scoped_by").is(identifierScope)
                )
                .select('pid')
                .toList()

            idvs.each { idv ->
                def id = idv.value('value')
                if (patientLookup.containsKey(id)) {
                    log.warn "$id already in map. skipping."
                    return
                }
                patientLookup.put(id, pV)
            }
        }

        /*
        def lookupSize = patientLookup.size()
        def numVerts = pats.size()
        assert lookupSize == numVerts
        */

        return patientLookup
    }


    /**
     *
     *
     */
    static public Map<String,Vertex> patientLookupPacketId(Graph graph, Collection<Vertex> patientVerts = []) {
        log.trace "patientLookupPacketId patientVerts:${patientVerts.size()}"

        return patientLookupUnambiguousIdentifier(graph, "encounter_pack_id", patientVerts)
    }



    /**
     *
     *
     */
    static public Map<String,Vertex> patientLookupEmpi(Graph graph, Collection<Vertex> patientVerts = []) {
        log.trace "patientLookupEmpi patientVerts:${patientVerts.size()}"

        return patientLookupUnambiguousIdentifier(graph, "empi", patientVerts)
    }


    /**
     *
     *
     */
    static private Map<String,Vertex> patientLookupUnambiguousIdentifier(Graph graph, String idClass, Collection<Vertex> patientVerts = []) {
        log.trace "patientLookupEmpi idClass:${idClass} patientVerts:${patientVerts.size()}"
        assert graph
        assert idClass

        def g = graph.traversal(CarnivalTraversalSource.class)

        Map<String,Vertex> patientLookup = new HashMap<String,Vertex>()

        def pats = patientVerts.size() > 0 ? patientVerts : g.patients().toList()
        pats.each { pV ->
            def idvs = g.V(pV)
                .out('is_identified_by').hasLabel('Identifier').as('pid')
                .out('is_instance_of').has("name", idClass)
                .select('pid')
                .toList()
            
            idvs.each { idv ->
                def empi = idv.value('value')
                if (patientLookup.containsKey(empi)) {
                    log.warn "$empi already in map. skipping."
                    return
                }
                patientLookup.put(idv.value('value'), pV)
            }
        }   

        return patientLookup
    }



    ///////////////////////////////////////////////////////////////////////////
    // UTILITY
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Convenenience method to log a Cypher statment and then run it.
     *
     */
    static public Object cypher(Graph graph, String q) {
        sqllog.info("PatientLookup.cypher:\n$q")
        return graph.cypher(q)
    }



}