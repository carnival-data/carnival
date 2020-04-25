package carnival.core.graph.reasoner



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.core.vine.Vine
import carnival.core.graph.Reasoner



/** */
public class PersonEncounterLinker extends Reasoner {
	
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public PersonEncounterLinker(Graph graph) {
        super(graph)
    }


    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public Map validate(Map args) {
        Collection<Vertex> sbl

        withGremlin { graph, g ->
            sbl = shouldBeLinked(g).toList()
            //log.debug "sbl: $sbl"
        }

        if (sbl.size() > 0) return [
            success:true,
            isValid:false,
            shouldBeLinked:sbl
        ]

        return [
            success:true,
            isValid:true,
            shouldBeLinked:[]
        ]
    }


    /** */
    public Map reason(Map args) {
        Collection<Vertex> edgeVs

        withGremlin { graph, g ->
            edgeVs = shouldBeLinked(g)
                .addE('participated_in_encounter').from('patient').to('encounter').toList()
                .toList()
            //log.debug "edgeVs: $edgeVs"
        }

        return [
            success:true,
            edgeVertices:edgeVs
        ]
    }



    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    GraphTraversal shouldBeLinked(GraphTraversalSource g) {
        g.V().match(
            __.as('patient').hasLabel('Patient').out('is_identified_by').as('id'),
            __.as('id').in('is_identified_by').hasLabel('BiobankEncounter').as('encounter'),
            __.not(__.as('patient').out('participated_in_encounter').as('encounter'))
        )
    }

}