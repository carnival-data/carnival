package carnival.core.graph



import groovy.transform.ToString

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.Base
import carnival.core.Core



/** 
 * A domain class to represent Identifier objects in the graph.  This class is
 * deprecated.  It is recommended that identifier classes be modeled using as
 * needed using standard Carnival graph models.
 * @deprecated
 */
@ToString
class Identifier {

	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Get or create a vertex.
	 */
	public static Vertex getOrCreateVertex(Graph graph, GraphTraversalSource g, Map args) {
		assert graph
		assert g
		assert args.identifierClass
		assert args.value

		def identifier = new Identifier(args)
		identifier.getOrCreateNode(graph, g)
	}


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////
	
	/** The vertex representing the identifier class */
	Vertex identifierClass

	/** The vertrex representing the identifier scope */
	Vertex identifierScope

	/** The vertex representing the identifier facility */
	Vertex identifierFacility

	/** The value of the identifier */
	String value


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////

	/** 
	 * Get or create an identifier node (vertex) in the provided graph.
	 */
	public Vertex getOrCreateNode(Graph graph) {
		def g = graph.traversal()
		getOrCreateNode(graph, g)
	}


	/** 
	 * Get or create an identifier node (vertex) in the provided graph using
	 * the provided graph traversal source.
	 */
	public Vertex getOrCreateNode(Graph graph, GraphTraversalSource g) {
		assert value
		assert identifierClass
		assert !(identifierScope && identifierFacility)

		
		if (identifierScope) {
			def foundVert = g.V().hasLabel(Core.VX.IDENTIFIER.label).has(Core.PX.VALUE.label, value).as("id")
			.and(
				__.out(Base.EX.IS_INSTANCE_OF.label).hasId(identifierClass.id()),
				__.out(Core.EX.IS_SCOPED_BY.label).hasId(identifierScope.id())
			) .select("id").tryNext()

			if (foundVert.isPresent()) {
				return foundVert.get()
			}
			else {
				def idVert = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, value).createVertex(graph)
				Base.EX.IS_INSTANCE_OF.relate(g, idVert, identifierClass)
				Core.EX.IS_SCOPED_BY.relate(g, idVert, identifierScope)
				return idVert
			}
		}
		else if (identifierFacility) {
			def foundVert = g.V().hasLabel(Core.VX.IDENTIFIER.label).has(Core.PX.VALUE.label, value).as("id")
			.and(
				__.out(Base.EX.IS_INSTANCE_OF.label).hasId(identifierClass.id()),
				__.out(Core.EX.WAS_CREATED_BY.label).hasId(identifierFacility.id())
			) .select("id").tryNext()

			if (foundVert.isPresent()) {
				return foundVert.get()
			}
			else {
				def idVert = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, value).createVertex(graph)
				Base.EX.IS_INSTANCE_OF.relate(g, idVert, identifierClass)
				Core.EX.WAS_CREATED_BY.relate(g, idVert, identifierFacility)
				return idVert
			}
		}
		else {
			def foundVert = g.V().hasLabel(Core.VX.IDENTIFIER.label).has(Core.PX.VALUE.label, value).as("id").out(Base.EX.IS_INSTANCE_OF.label).hasId(identifierClass.id()).select("id").tryNext()

			if (foundVert.isPresent()) {
				return foundVert.get()
			}
			else {
				def idVert = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, value).createVertex(graph)
				Base.EX.IS_INSTANCE_OF.relate(g, idVert, identifierClass)
				return idVert
			}
		}
	}
}


