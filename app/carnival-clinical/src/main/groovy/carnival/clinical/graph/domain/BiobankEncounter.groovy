package carnival.clinical.graph.domain



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

import carnival.core.*
import carnival.core.graph.CarnivalTraversalSource
import carnival.core.graph.*



/**
 * This "domain" class is currently just a way to group related functionality
 * that was previously dumped in PmbbGraph.
 *
 */
class BiobankEncounter {

    ///////////////////////////////////////////////////////////////////////////
    // LOGGING
    ///////////////////////////////////////////////////////////////////////////

    /** sql log */
    static final Logger sqllog = LoggerFactory.getLogger('sql')

    /** error log */
    static final Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** carnival log */
    static final Logger log = LoggerFactory.getLogger(BiobankEncounter)


    ///////////////////////////////////////////////////////////////////////////
    // LOOKUPS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static public Map<String,Vertex> lookupPacketId(Graph graph, Collection<Vertex> objectVerts = []) {
        log.trace "lookupPacketId objectVerts:${objectVerts.size()}"

        return lookupUnambiguousIdentifier(graph, "encounter_pack_id", objectVerts)
    }


    /** */
    static private Map<String,Vertex> lookupUnambiguousIdentifier(Graph graph, String idClass, Collection<Vertex> objectVerts = []) {
        log.trace "lookupEmpi idClass:${idClass} objectVerts:${objectVerts.size()}"
        assert graph
        assert idClass

        def g = graph.traversal(CarnivalTraversalSource.class)

        Map<String,Vertex> lookup = new HashMap<String,Vertex>()

        def obvs = objectVerts ?: g.biobankEncounters()
        obvs.each { pV ->
            def idvs = g.V(pV)
                .out('is_identified_by').hasLabel('Identifier').as('oid')
                .out('is_instance_of').has("name", idClass)
                .select('oid')
                .toList()
            
            idvs.each { idv ->
                def idvalue = idv.value('value')
                if (lookup.containsKey(idvalue)) {
                    log.warn "$idvalue already in map. skipping."
                    return
                }
                lookup.put(idvalue, pV)
            }
        }   

        return lookup
    }

}