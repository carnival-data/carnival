package carnival.core.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of




/** */
class CoreGraphUtils {

    /** SQL log */
    static Logger sqllog = LoggerFactory.getLogger('sql')

    /** Error log */
    static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** Carnival log */
    static Logger log = LoggerFactory.getLogger('carnival')



    ///////////////////////////////////////////////////////////////////////////
    // CYPHER
    ///////////////////////////////////////////////////////////////////////////
    
    /** */
    public static String toPropertyMap(Map args) {
        def vals = args.vals
        if (!vals) return ""

        def excludes = args.excludes ?: []

        def propStings = []
        vals.each { k, v ->
            if (excludes.contains(k)) return
            if (v == null) return

            def str = "$k:"
            if ("$v".isInteger()) str += v
            else str += "'$v'"

            propStings << str
        }

        def str = "{"
        str += propStings.join(', ')
        str += "}"

        return str
    }


    ///////////////////////////////////////////////////////////////////////////
    // GREMLIN TRAVERSALS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get all the identifier verticies of a particular class and all the patients and encounters 
     * associated with the vertex.
     *
     * Assumes that idVertex.value is unique for the given identfierClass and identifierScope filters.
     *
     * TODO: perhaps throw a warning if multiple id vertices have the same value
     *
     * @return [id.value : [ids:[idVertex], patients:[patientVertices], biobankEncounters:[biobankEncounterVerticies]]
    */
    public static populateIdMap(def graph, def identifierClass, def identifierScope = null) {
        assert graph
        assert identifierClass
        
        def g = graph.traversal()        
        def idClassName = identifierClass.property("name").value

        def identifierMap = [:]

        def idList // all identifiers of the given type/scope
        def idPatList // identifiers and patients
        def idEncList // identifiers and biobankEncounters

        if (!identifierScope) {

            idList = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName)
            )
           .select('id').toList()

            idPatList = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName),
            __.as('id').in('is_identified_by').hasLabel('Patient').as('identifiedObject')
            )
           .select('id', 'identifiedObject').toList()

            idEncList = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName),
            __.as('id').in('is_identified_by').hasLabel('BiobankEncounter').as('identifiedObject')
            )
           .select('id', 'identifiedObject').toList()
        }
        else {
            def idScopeName = identifierScope.property("name").value

            idList = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName),
            __.as('id').out('is_scoped_by').has("name", idScopeName)
            )
           .select('id').toList()

            idPatList = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName),
            __.as('id').out('is_scoped_by').has("name", idScopeName),
            __.as('id').in('is_identified_by').hasLabel('Patient').as('identifiedObject')
            )
           .select('id', 'identifiedObject').toList()

            idEncList = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName),
            __.as('id').out('is_scoped_by').has("name", idScopeName),
            __.as('id').in('is_identified_by').hasLabel('BiobankEncounter').as('identifiedObject')
            )
           .select('id', 'identifiedObject').toList()
        }


        // *Map of the form:
        // ["uuid1234":[v[1], v[2], v[3]]
        def idPatMap = idPatList.groupBy({it.id.property("value").value}).collectEntries{k,v ->[k, v.identifiedObject]}.withDefault{[]}
        def idEncMap = idEncList.groupBy({it.id.property("value").value}).collectEntries{k,v ->[k, v.identifiedObject]}.withDefault{[]}


        identifierMap = idList.groupBy({it.property("value").value}).collectEntries{ k, v -> [k, [ids:v, patients:idPatMap[k], biobankEncounters:idEncMap[k]]]}
        
        return identifierMap

    }


    /**
     * Get all the identifier vertices of a particular class, and optionally an associated patient.
     *
     * @return [id.value : [id:idVertex, patient:patientVertex]]
     */
    public static populatePatientIdMap(def graph, def identifierClass, def identifierScope = null) {
        assert graph
        assert identifierClass
        
        def g = graph.traversal()

        def idClassName = identifierClass.property("name").value

        def patientRes
        def nonPatientRes

        if (!identifierScope) {
            patientRes = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName),
            __.as('id').in('is_identified_by').hasLabel('Patient').as('patient')
            )
           .select('id', 'patient').toList()

           nonPatientRes = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName).as('patient'),
            __.as('id').not(__.in('is_identified_by').hasLabel('Patient'))
            )
           .select('id').toList()

       }
       else {
            def idScopeName = identifierScope.property("name").value

            patientRes = g.V().match(
            __.as('id').out('is_instance_of').has("name", idClassName),
            __.as('id').out('is_scoped_by').has("name", idScopeName),
            __.as('id').in('is_identified_by').hasLabel('Patient').as('patient'))
           .select('patient','id').toList()

           nonPatientRes = g.V().match(
            __.as('id').hasLabel('Identifier').out('is_instance_of').has("name", idClassName).as('patient'),
            __.as('id').out('is_scoped_by').has("name", idScopeName),
            __.as('id').not(__.in('is_identified_by').hasLabel('Patient'))
            )
           .select('id').toList()
       }

       log.trace "populatePatientIdMap($idClassName, $identifierScope) found ${patientRes.size()} patient connected entries"
       log.trace "populatePatientIdMap($idClassName, $identifierScope) found ${nonPatientRes.size()} non-patient connected entries"
 

        def entries = patientRes.collectEntries {
            [(it.id.property("value").value) : it]
        }

        nonPatientRes.each { id ->
            entries[id.property("value").value] = [id:id]
        }

        return entries
    }


    /**
     * Get all the identifier vertices of a particular class, and optionally an associated patient.
     *
     * @return [id.value : [id:idVertex, patient:patientVertex]]
     */
    public static populatePatientIdMapWithFacility(Graph graph, Vertex identifierClass, Vertex identifierFacility) {
        assert graph
        assert identifierClass
        
        def g = graph.traversal()

        def idClassName = identifierClass.value("name")
        def idFacilityName = identifierFacility.value("name")

        def patientRes
        def nonPatientRes

        patientRes = g.V().match(
        __.as('id').out('is_instance_of').is(identifierClass),
        __.as('id').out('was_created_by').is(identifierFacility),
        __.as('id').in('is_identified_by').hasLabel('Patient').as('patient'))
       .select('patient','id').toList()

       nonPatientRes = g.V().match(
        __.as('id').hasLabel('Identifier').out('is_instance_of').is(identifierClass).as('patient'),
        __.as('id').out('was_created_by').is(identifierFacility),
        __.as('id').not(__.in('is_identified_by').hasLabel('Patient'))
        )
       .select('id').toList()

       log.trace "populatePatientIdMap($idClassName, $idFacilityName) found ${patientRes.size()} patient connected entries"
       log.trace "populatePatientIdMap($idClassName, $idFacilityName) found ${nonPatientRes.size()} non-patient connected entries"
 
        def entries = patientRes.collectEntries {
            [(it.id.value("value")) : it]
        }

        nonPatientRes.each { id ->
            entries[id.value("value")] = [id:id]
        }

        return entries
    }



    ///////////////////////////////////////////////////////////////////////////
    // DEBUG PRINTING
    ///////////////////////////////////////////////////////////////////////////
    static void printVerts(Traversal trv, String prefixLine = "") {
        printVerts(trv.toList())
    }

    static void printVerts(Collection<Vertex> vertices, String prefixLine = "") {
        if (prefixLine) println prefixLine
        vertices.each { v ->
            println "${v.id()}:${v.label()} " + v.properties().collect { "${it.key()}:${it.value()}" }
        }
    }

    static String str(String prefix, int num) {
        return String.valueOf("${prefix}${num}")
    }

    static void printGraph(GraphTraversalSource g) {
        def vs = g.V().valueMap(true)
        vs.each { v -> println "$v" }

        def es = g.E()
        es.each { e -> println "$e" }
    }
}