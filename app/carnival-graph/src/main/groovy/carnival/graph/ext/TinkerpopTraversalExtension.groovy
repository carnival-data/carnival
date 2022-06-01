package carnival.graph.ext



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.transform.EqualsAndHashCode

import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.graph.EdgeDefinition
import carnival.graph.VertexDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.Base



/** 
 * Extensions to the Tinkerpop Gremlin graph traversal language that enable the use of 
 * Carnival objects in Gremlin treversals.
 *
 */
class TinkerpopTraversalExtension {

    /** 
     * Extension of the Properties step tp accept a property definition.
     *
     * @param traversal The traversal this method will modify.
     * @param pdef      The proprty definition
     * @return          The modified traversal
     *
     */
    static GraphTraversal properties(DefaultTraversal traversal, PropertyDefinition pdef) {
        traversal.properties(pdef.label)
    }


    /** 
     * This step can be applied to vertices that represent classes finding
     * all vertices that represent instances of the class or any of its sub-
     * classes.
     *
     * @param traversal The traversal this method will modify.
     * @return          The modified traversal
     *
     */
    static GraphTraversal instances(DefaultTraversal traversal) {
        traversal
            .emit()
            .repeat(
                __.in(Base.EX.IS_SUBCLASS_OF)
            )
        .in(Base.EX.IS_INSTANCE_OF)
    }

    /** 
     * Filters elements to include only  instances of the given
     * vertex definition by traversing IS_INSTANCE_OF and IS_SUBCLASS_OF
     * edges.
     *
     * @param traversal The traversal this method will modify.
     * @param vdef      The vertex definition
     * @return          The modified traversal
     */
    static GraphTraversal isInstanceOf(DefaultTraversal traversal, VertexDefinition vdef) {
        traversal
            .as('ttev')
            .out(Base.EX.IS_INSTANCE_OF)
            .until(
                __.isa(vdef)
            )
            .repeat(
                __.out(Base.EX.IS_SUBCLASS_OF)
            )
        .select('ttev')
    }


    /** 
     * Finds all the class vertices relevant to the current elements in the 
     * traversal by traversing IS_INSTANCE_OF and IS_SUBCLASS_OF edges.
     *
     * @param traversal The traversal this method will modify.
     * @return          The modified traversal
     */
    static GraphTraversal classes(DefaultTraversal traversal) {
        traversal
            .out(Base.EX.IS_INSTANCE_OF)
            .emit()
        .repeat(
            __.out(Base.EX.IS_SUBCLASS_OF)
        )
    }


    /** 
     * Find the class vertices of the current elements in the traversal by
     * looking for an IS_INSTANCE_OF edge that points to a class vertex.
     * 
     *
     * @param traversal The traversal this method will modify.
     * @return          The modified traversal
     */
    static GraphTraversal instanceClass(DefaultTraversal traversal) {
        traversal
            .out(Base.EX.IS_INSTANCE_OF)
            .has(Base.PX.IS_CLASS, true)
    }


    /** 
     * Filters the elements in the traversal to include only those that match
     * the given vertex definition.
     *
     * @param traversal The traversal this method will modify.
     * @param vdef      The vertex definition
     * @return          The modified traversal
     */
    static GraphTraversal isa(DefaultTraversal traversal, VertexDefinition vdef) {
        traversal.hasLabel(vdef.label).has(Base.PX.NAME_SPACE.label, vdef.nameSpace)
    }


    /** 
     * Filters the elements in the traversal to include only those that match
     * the given edge definition.
     *
     * @param traversal The traversal this method will modify.
     * @param edef      The edge definition
     * @return          The modified traversal
     */
    static GraphTraversal isa(DefaultTraversal traversal, EdgeDefinition edef) {
        traversal.hasLabel(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }


    /** 
     * Extension of the Both step that traverses edges matching the given edge
     * definition.
     *
     * @param traversal The traversal this method will modify.
     * @param edef      The edge definition
     * @return          The modified traversal
     */
    static GraphTraversal both(DefaultTraversal traversal, EdgeDefinition edef) {
        traversal.bothE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).otherV()
    }


    /** 
     * Extension of the BothE step that traverses edges matching the given edge
     * definition.
     *
     * @param traversal The traversal this method will modify.
     * @param edef      The edge definition
     * @return          The modified traversal
     */
    static GraphTraversal bothE(DefaultTraversal traversal, EdgeDefinition edef) {
        traversal.bothE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }


    /** 
     * Extension of the Out step that traverses edges matching the given edge
     * definition.
     *
     * @param traversal The traversal this method will modify.
     * @param edef      The edge definition
     * @return          The modified traversal
     */
    static GraphTraversal out(DefaultTraversal traversal, EdgeDefinition edef) {
        traversal.outE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).inV()
    }


    /** 
     * Extension of the OutE step that traverses edges matching the given edge
     * definition.
     *
     * @param traversal The traversal this method will modify.
     * @param edef      The edge definition
     * @return          The modified traversal
     */
    static GraphTraversal outE(DefaultTraversal traversal, EdgeDefinition edef) {
        traversal.outE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }


    /** 
     * Extension of the In step that traverses edges matching the given edge
     * definition.
     *
     * @param traversal The traversal this method will modify.
     * @param edef      The edge definition
     * @return          The modified traversal
     */
    static GraphTraversal "in"(DefaultTraversal traversal, EdgeDefinition edef) {
        traversal.inE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace).outV()
    }


    /** 
     * Extension of the InE step that traverses edges matching the given edge
     * definition.
     *
     * @param traversal The traversal this method will modify.
     * @param edef      The edge definition
     * @return          The modified traversal
     */
    static GraphTraversal inE(DefaultTraversal traversal, EdgeDefinition edef) {
        traversal.inE(edef.label).has(Base.PX.NAME_SPACE.label, edef.nameSpace)
    }


    /** 
     * Extension of the Has step that accepts a property definition.
     *
     * @param traversal The traversal this method will modify.
     * @param pdef      The property definition
     * @return          The modified traversal
     */
    static GraphTraversal has(DefaultTraversal traversal, PropertyDefinition pdef) {
        traversal.has(pdef.label)
    }


    /** 
     * Extension of the HasNot step that accepts a property definition.
     *
     * @param traversal The traversal this method will modify.
     * @param pdef      The property definition
     * @return          The modified traversal
     */
    static GraphTraversal hasNot(DefaultTraversal traversal, PropertyDefinition pdef) {
        traversal.hasNot(pdef.label)
    }


    /** 
     * Extension of the Has step that accepts a property definition and an enum
     * whose name() will be used as the property value.
     *
     * @param traversal The traversal this method will modify.
     * @param pdef      The property definition
     * @param value     The enum whose name() will be used as the property value
     * @return          The modified traversal
     */
    static GraphTraversal has(DefaultTraversal traversal, PropertyDefinition pdef, Enum value) {
        traversal.has(pdef.label, value.name())
    }


    /** 
     * Extension of the Has step that accepts a property definition and a 
     * value.
     *
     * @param traversal The traversal this method will modify.
     * @param pdef      The property definition
     * @param value     The value to use as the property value
     * @return          The modified traversal
     */
    static GraphTraversal has(DefaultTraversal traversal, PropertyDefinition pdef, Object value) {
        traversal.has(pdef.label, value)
    }


    /** 
     * Filter the elements in the traversal to include only those that have the
     * same value as the provided vertex for the provided property or if the 
     * provided vertex does not have the property then include elements that
     * likewise do not have that property.
     *
     * @param traversal The traversal this method will modify.
     * @param pdef      The property definition
     * @param vertex    THe vertex from which to get the property value
     * @return          The modified traversal
     */
    static GraphTraversal matchesOn(DefaultTraversal traversal, PropertyDefinition pdef, Vertex vertex) {
        if (pdef.of(vertex).isPresent()) {
            traversal.has(pdef.label, pdef.valueOf(vertex))
        } else {
            traversal.hasNot(pdef.label)
        }
        traversal        
    }


    /** 
     * Filter the elements in the traversal to include only those that have the
     * same value as the provided vertex for the provided property or if the 
     * provided vertex does not have the property then include elements that
     * likewise do not have that property using one property definition for the
     * traversal elements and another for the provided vertex.
     *
     * @param traversal      The traversal this method will modify.
     * @param traversalPdef  The property definition to use with traversal elements.
     * @param vertex         The vertex from which to get the property value
     * @param vertexPdef     The property definition to use with the provided vertex
     * @return               The modified traversal
     */
    static GraphTraversal matchesOn(
        DefaultTraversal traversal, 
        PropertyDefinition traversalPdef, 
        Vertex vertex, 
        PropertyDefinition vertexPdef
    ) {
        if (vertexPdef.of(vertex).isPresent()) {
            traversal.has(traversalPdef.label, vertexPdef.valueOf(vertex))
        } else {
            traversal.hasNot(traversalPdef.label)
        }
        traversal        
    }


    /** 
     * Extension of the next() terminal step that throws an exception if the
     * traversal yields more than one element.
     *
     * @param traversal      The traversal this method will modify.
     * @return               The modified traversal
     */
    static Object nextOne(DefaultTraversal traversal) {
        def verts = traversal.toList()
        if (verts.size() > 1) throw new RuntimeException("nextOne: ${verts.size()}")
        if (verts.size() == 1) return verts.first()
        else return null
    }


}