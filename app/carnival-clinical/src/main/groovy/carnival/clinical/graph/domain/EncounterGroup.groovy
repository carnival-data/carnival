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
class EncounterGroup {

    ///////////////////////////////////////////////////////////////////////////
    // LOGGING
    ///////////////////////////////////////////////////////////////////////////

    static final Logger sqllog = LoggerFactory.getLogger('sql')
    static final Logger elog = LoggerFactory.getLogger('db-entity-report')
    static final Logger log = LoggerFactory.getLogger('carnival')



    ///////////////////////////////////////////////////////////////////////////
    // FACTORY
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    static public Vertex createEncounterGroupFromIdValues(Graph graph, String idClassName, Collection<String> idVals) {
        assert idClassName
        def keyType = Enum.valueOf(KeyType, idClassName.toUpperCase())
        if (!keyType) throw new IllegalArgumentException("unsupported key type: $idClassName")
        createEncounterGroupFromIdValues(graph, keyType, idVals)
    }


    static public Vertex createEncounterGroupFromIdValues(Graph graph, KeyType keyType, Collection<String> idVals) {
        createEncounterGroupFromIdValues(graph:graph, keyType:keyType, idVals:idVals)
    }

    
    static public Vertex createEncounterGroupFromIdValues(Graph graph, String name, KeyType keyType, Collection<String> idVals) {
        createEncounterGroupFromIdValues(graph:graph, name:name, keyType:keyType, idVals:idVals)
    }
    

    /**
     * Create a patient group vertex that includes all the patients referenced
     * by the given collection of identifiers.
     *
     * @return The Vertex of the EncounterGroup created.
     *
     */
    static public Vertex createEncounterGroupFromIdValues(Map args) {
        Graph graph = args.graph
        assert graph
        KeyType keyType = args.keyType
        assert keyType
        Collection<String> idVals = args.idVals
        assert idVals

        if (![KeyType.ENCOUNTER_PACK_ID].contains(keyType))
            throw new IllegalArgumentException("unsupported key type: $keyType")

        def g = graph.traversal(CarnivalTraversalSource.class)

        def idClassName = keyType.name().toLowerCase()

        def cq = """
MATCH (e:BiobankEncounter)-[:is_identified_by]->(i:Identifier),
(i)-[:is_instance_of]-> (c:IdentifierClass)
WHERE i.value IN SUB_ENTITY_IDS
AND c.name = 'encounter_pack_id'
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
        log.info "the $numInputIds ids provided identify ${distinctPatVs.size()} distinct encounters"

        def gV = graph.addVertex(T.label, "EncounterGroup")
        if (args.name) gV.property('name', args.name)
        
        distinctPatVs.each { eV -> eV.addEdge('is_member_of', gV) }

        return gV
    }



    ///////////////////////////////////////////////////////////////////////////
    // ENCOUNTER LOOKUPS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    static public Map<String,Vertex> encounterLookupPacketId(Graph graph, Collection<Vertex> encounterVerts = []) {
        log.trace "encounterLookupPacketId encounterVerts:${encounterVerts.size()}"

        return encounterLookupUnambiguousIdentifier(graph, "encounter_pack_id", encounterVerts)
    }



    /**
     *
     *
     */
    static public Map<String,Vertex> encounterLookupEmpi(Graph graph, Collection<Vertex> encounterVerts = []) {
        log.trace "encounterLookupEmpi encounterVerts:${encounterVerts.size()}"

        return encounterLookupUnambiguousIdentifier(graph, "encounter_pack_id", encounterVerts)
    }


    /**
     *
     *
     */
    static private Map<String,Vertex> encounterLookupUnambiguousIdentifier(Graph graph, String idClass, Collection<Vertex> encounterVerts = []) {
        log.trace "encounterLookupEmpi idClass:${idClass} encounterVerts:${encounterVerts.size()}"
        assert graph
        assert idClass

        def g = graph.traversal(CarnivalTraversalSource.class)

        Map<String,Vertex> encounterLookup = new HashMap<String,Vertex>()

        def pats = encounterVerts ?: g.biobankEncounters()
        pats.each { pV ->
            g.V(pV)
                .out('is_identified_by').hasLabel('Identifier').as('id')
                .out('is_instance_of').has("name", idClass)
                .select('id')
                .each { idv ->

                def idValue = idv.value('value')
                if (encounterLookup.containsKey(idValue)) {
                    log.warn "$idValue already in map. skipping."
                    return
                }
                encounterLookup.put(idv.value('value'), pV)
            }
        }   

        return encounterLookup
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