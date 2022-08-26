package carnival.core.graph



import java.text.SimpleDateFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.P

import carnival.util.Log
import carnival.core.graph.GraphValidationError
import carnival.graph.VertexDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.EdgeDefinition
import carnival.core.graph.Core
import carnival.core.Carnival




/** */
trait NameTrait {
    abstract public String getName()
}



/** 
 * GraphEntityCache was initially created as a way to store graph vertices in
 * maps for efficiency purposes.  In the course of its evolution, fun methods
 * like linkSubject and setRelation were added.  The concepts that came out of
 * that were incorporated into the *DefTrait traits.
 *
 * GraphEntityCache should probably be deprecated.
 *
 */
class GraphEntityCache {

	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

    /** */
    Graph graph

    /** */
    Map<String,Vertex> subjectByIdValueCache = new HashMap<String,Vertex>()

    /** */
    Map<String,Vertex> subjectByIdentifierCache = new HashMap<String,Vertex>()

    /** */
    Map<String,Vertex> linkedSubjectCache = new HashMap<String,Vertex>()

    /** */
    Map<String,Vertex> identifierCache = new HashMap<String,Vertex>()


	///////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	///////////////////////////////////////////////////////////////////////////

	/** */
	public GraphEntityCache(Graph graph) {
        assert graph
        this.graph = graph
	}


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

    /** */
    Vertex subjectByIdValue(GraphTraversalSource g, VertexDefinition subjectDef, PropertyDefinition idPropertyDef, String idValue) {
        String key = "${subjectDef.label}-${idPropertyDef.label}-${idValue}"
        if (subjectByIdValueCache.containsKey(key)) return subjectByIdValueCache.get(key)

        def outV = g.V()
            .hasLabel(subjectDef.label)
            .has(idPropertyDef.label, idValue)
            .tryNext()
            .orElseGet {
                graph.addVertex(
                    T.label, subjectDef.label,
                    idPropertyDef.label, idValue
                )
            }
        subjectByIdValueCache.put(key, outV)
        return outV 
    }        


    /** */
    Vertex subjectByIdentifier(GraphTraversalSource g, VertexDefinition subjectDef, Vertex identifierV) {
        String key = "${subjectDef.label}-${identifierV.id()}"
        if (subjectByIdentifierCache.containsKey(key)) return subjectByIdentifierCache.get(key)

        def outV = g.V(identifierV)
            .in(Core.EX.IS_IDENTIFIED_BY.label)
            .hasLabel(subjectDef.label)
            .tryNext()
            .orElseGet {
                graph.addVertex(
                    T.label, subjectDef.label,
                    idPropertyDef.label, idValue
                )
            }
        subjectByIdentifierCache.put(key, outV)
        return outV 
    }    


    /** */
    Vertex identifierClass(GraphTraversalSource g, NameTrait idc) {
        g.V().hasLabel(Core.VX.IDENTIFIER_CLASS.label)
            .has(Core.PX.NAME.label, idc.name)
            .tryNext().orElseGet {
                graph.addVertex(
                    T.label, Core.VX.IDENTIFIER_CLASS.label,
                    Core.PX.NAME.label, idc.name
                )
        }                
    }


    /** */
    Vertex identifier(GraphTraversalSource g, Vertex idClassV, String idValue) {
        String key = "${idClassV.id()}-${idValue}"
        if (identifierCache.containsKey(key)) return identifierCache.get(key)
        def outV = g.V(idClassV)
            .in(Base.EX.IS_INSTANCE_OF.label)
            .hasLabel(Core.VX.IDENTIFIER.label)
            .has(Core.PX.VALUE.label, idValue)
            .tryNext().orElseGet {
                def idv = graph.addVertex(
                    T.label, Core.VX.IDENTIFIER.label,
                    Core.PX.VALUE.label, idValue
                )
                idv.addEdge(Base.EX.IS_INSTANCE_OF.label, idClassV)
                return idv
            }
        identifierCache.put(key, outV)
        return outV 
    }


    /** */
    Vertex linkSubject(GraphTraversalSource g, VertexDefinition subjectDef, Vertex identifierV) {
        String key = "${subjectDef.label}-${identifierV.id()}"
        if (linkedSubjectCache.containsKey(key)) return linkedSubjectCache.get(key)

        def outV = g.V(identifierV)
            .in(Core.EX.IS_IDENTIFIED_BY.label)
            .hasLabel(subjectDef.label)
            .tryNext().orElseGet {
                def sv = graph.addVertex(
                    T.label, subjectDef.label
                )
                sv.addEdge(Core.EX.IS_IDENTIFIED_BY.label, identifierV)
                return sv
            }
        linkedSubjectCache.put(key, outV)
        return outV 
    }


    /** */
    Edge setRelationship(GraphTraversalSource g, Vertex subjectV, EdgeDefinition predicateDef, Vertex objectV) {
        g.V(subjectV)
            .outE(predicateDef.label).as('e')
            .inV()
            .is(objectV)
            .select('e')
        .tryNext().orElseGet {
            subjectV.addEdge(predicateDef.label, objectV)
        }
    }

}