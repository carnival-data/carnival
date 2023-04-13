package carnival.core.graph



import java.text.DateFormat
import java.text.SimpleDateFormat

import groovy.transform.Memoized
import groovy.util.logging.Slf4j

import groovy.transform.ToString
import groovy.transform.Synchronized
import groovy.transform.WithReadLock
import groovy.transform.WithWriteLock

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.util.DescribedEntity
import carnival.util.DataSetDescriptor
import carnival.util.FeatureReport
import carnival.util.FeatureSetDescriptor
import carnival.util.FeatureSetRecipe
import carnival.util.FeatureSetRecipeStep
import carnival.util.FeatureSetRecipeIngredient
import carnival.util.FeatureDataType
import carnival.util.StringPrinter
import carnival.util.MarkdownPrinter

import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.VertexDefinition
import carnival.core.Core




/**
 * A persistence of a data set descriptor in a property graph.  This is
 * experimental code and subject to rapid change. 
 *
 */
class DataSetDescriptorGraph implements GremlinTrait {


    ///////////////////////////////////////////////////////////////////////////
    // GRAPH MODEL
    ///////////////////////////////////////////////////////////////////////////

    /** Vertex model */
    static enum VX implements VertexDefinition {
        
        /** Feature report */
        FEATURE_REPORT(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true)
            ]
        ),

        /** Data set description */
        DATA_SET_DESCRIPTOR(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION
            ]
        ),

        /** Feature set description */
        FEATURE_SET_DESCRIPTOR(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),

        /** Feature set recipe */
        FEATURE_SET_RECIPE(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),

        /** Feature set recipe step */
        FEATURE_SET_RECIPE_STEP(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),

        /** Feature set recipe ingredient */
        FEATURE_SET_RECIPE_INGREDIENT(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),

        /** Feature data type */
        FEATURE_DATA_TYPE(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
            ]
        ),

        /** Feature set name */
        FEATURE_SET_NAME(
            vertexProperties:[
                Core.PX.VALUE.withConstraints(required:true),
            ]
        )

        private VX() {}
        private VX(Map m) { if (m.vertexProperties) this.vertexProperties = m.vertexProperties }
    }


    /** Edge model */
    static enum EX implements EdgeDefinition {
        
        /** Has final step */
        HAS_FINAL_STEP
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a DataSetDescriptorGraph.
     * @param graph The graph to use.
     */
    DataSetDescriptorGraph(Graph graph) {
        this.graph = graph
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Load a data set descriptor from a vertex.
     * @param dataSetDescriptorV The vertex
     * @return A DataSetDescriptor
     */
    DataSetDescriptor load(Vertex dataSetDescriptorV) {
        withTraversal { g ->
            load(g, dataSetDescriptorV)
        }
    }


    /** 
     * Load a data set descriptor from a vertex using the provided graph
     * traversal source.
     * @param dataSetDescriptorV The vertex
     * @param g The graph traversal source
     * @return The data set descriptor object
     */
    DataSetDescriptor load(GraphTraversalSource g, Vertex dataSetDescriptorV) {

        def dsd = new DataSetDescriptor()

        g.V(dataSetDescriptorV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(VX.FEATURE_SET_RECIPE_INGREDIENT.label)
        .toSet().each {
            dsd.recipeIngredients << loadFeatureSetRecipeIngredient(g, it)
        }

        g.V(dataSetDescriptorV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(VX.FEATURE_SET_DESCRIPTOR.label)
        .toSet().each {
            dsd.featureSetDescriptors << loadFeatureSetDescriptor(g, it)
        }


        //g.V(1).repeat(out()).until(outE().count().is(0)).path().by('name')

        g.V(dataSetDescriptorV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(VX.FEATURE_SET_DESCRIPTOR.label)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(VX.FEATURE_SET_RECIPE.label)
            .out(EX.HAS_FINAL_STEP.label)
            .hasLabel(VX.FEATURE_SET_RECIPE_STEP.label)
            .emit() // emit the final step
            .repeat(
                __.out(Core.EX.DEPENDS_ON.label)
                .hasLabel(VX.FEATURE_SET_RECIPE_STEP.label)
            ).until (
                __.outE(Core.EX.DEPENDS_ON.label).count().is(0)
            )
        .toSet().each {
            //log.debug "${it} ${it.label()}"
            dsd.recipeSteps << loadFeatureSetRecipeStep(g, it)
        }


        dsd
    }


    /** 
     * Load a feature set descriptor from a vertex and a graph traversal 
     * source.
     * @param fsdV The vertrex
     * @param g The graph traversal source
     * @return The feature set descriptor
     */
    FeatureSetDescriptor loadFeatureSetDescriptor(
        GraphTraversalSource g, 
        Vertex fsdV
    ) {
        def fsd = load(g, FeatureSetDescriptor, fsdV)

        g.V(fsdV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(VX.FEATURE_SET_RECIPE.label)
        .toSet().each {
            fsd.recipe = loadFeatureSetRecipe(g, it)
        }

        g.V(fsdV)
            .out(Core.EX.DESCRIBES.label)
            .hasLabel(VX.FEATURE_SET_NAME.label)
        .toSet().each {
            fsd.featureSetNames << it.value(Core.PX.VALUE.label)
        }

        g.V(fsdV)
            .in(Core.EX.DESCRIBES.label)
            .hasLabel(VX.FEATURE_DATA_TYPE.label)
        .toSet().each {
            fsd.dataTypes << Enum.valueOf(FeatureDataType, it.value(Core.PX.NAME.label)) 
        }

        fsd
    }


    /** 
     * Load a feature set recipe froma vertex and a graph traversal source.
     * @param fsrV The vertrex
     * @param g The graph traversal source
     */
    FeatureSetRecipe loadFeatureSetRecipe(
        GraphTraversalSource g, 
        Vertex fsrV
    ) {
        def fsr = load(g, FeatureSetRecipe, fsrV)

        g.V(fsrV)
            .out(EX.HAS_FINAL_STEP.label)
            .hasLabel(VX.FEATURE_SET_RECIPE_STEP.label)
        .toSet().each {
            fsr.finalStep = recursiveLoadFeatureSetRecipeSteps(g, it)
        }

        fsr
    }


    /** 
     * Load a feature set recipe from a vertex and a graph traversal source.
     * @param fsrsV The vertex
     * @param g The graph traversal source
     * @return A feature set recipe object
     */
    FeatureSetRecipeStep loadFeatureSetRecipeStep(
        GraphTraversalSource g, 
        Vertex fsrsV
    ) {
        def fsrs = load(g, FeatureSetRecipeStep, fsrsV)

        g.V(fsrsV)
            .in(Core.EX.IS_INPUT_OF.label)
            .hasLabel(VX.FEATURE_SET_RECIPE_INGREDIENT.label)
        .toSet().each {
            def ing = loadFeatureSetRecipeIngredient(g, it)
            fsrs.inputs << ing
        }

        g.V(fsrsV)
            .in(Core.EX.IS_OUTPUT_OF.label)
            .hasLabel(VX.FEATURE_SET_RECIPE_INGREDIENT.label)
        .toSet().each {
            def ing = loadFeatureSetRecipeIngredient(g, it)
            fsrs.outputs << ing
        }

        fsrs
    }


    /** 
     * Recursively load feature set recipe steps from a vertex using the
     * provided graph traversal source.
     * @param fsrsV The vertex
     * @param g The graph traversal source
     * @return Feature set recipe step
     */
    FeatureSetRecipeStep recursiveLoadFeatureSetRecipeSteps(
        GraphTraversalSource g, 
        Vertex fsrsV
    ) {
        def fsrs = loadFeatureSetRecipeStep(g, fsrsV) 

        g.V(fsrsV)
            .out(Core.EX.DEPENDS_ON.label)
            .hasLabel(VX.FEATURE_SET_RECIPE_STEP.label)
        .toSet().each {
            def priorStep = recursiveLoadFeatureSetRecipeSteps(g, it)
            fsrs.dependencies << priorStep
        }

        fsrs
    }


    /** 
     * Load a feature set recipe ingredient from a vertex using the graph
     * traversal source.
     * @param ingV The vertex
     * @param g The graph traversal source
     * @return The feature set recipe object
     */
    FeatureSetRecipeIngredient loadFeatureSetRecipeIngredient(
        GraphTraversalSource g, 
        Vertex ingV
    ) {
        load(g, FeatureSetRecipeIngredient, ingV)
    }


    /** 
     * Load a described entity from a vertex, target class, and graph traversal
     * source.
     * @param entityV The vertex
     * @param targetClass The target class
     * @param g The graph traversal source
     * @return The described entity object
     */
    DescribedEntity load(GraphTraversalSource g, Class targetClass, Vertex entityV) {
        DescribedEntity ent = targetClass.newInstance()
        ent.eid = entityV.id()
        ent.name = entityV.value(Core.PX.NAME.label)
        if (entityV.property(Core.PX.DESCRIPTION.label).isPresent()) ent.description = entityV.value(Core.PX.DESCRIPTION.label)
        ent
    }


    /** 
     * Save feature report and data set desriptor using a new graph traversal
     * source.
     * @param report The feature report
     * @param dsd The data set descriptor
     * @return The vertex representing the saved entity
     */
    Vertex save(DataSetDescriptor dsd, FeatureReport report) {
        withTraversal { g -> save(g, dsd) }
    }


    /** 
     * Save feature report and data set desriptor using thge graph traversal
     * source provided.
     * @param report The feature report
     * @param dsd The data set descriptor
     * @param g The graph traversal source
     * @return The vertex representing the saved entity
     */
    Vertex save(GraphTraversalSource g, DataSetDescriptor dsd, FeatureReport report) {
        assert g
        assert dsd

        def dsdV = save(g, dsd)

        def reportV = VX.FEATURE_REPORT.instance().withProperty(Core.PX.NAME, report.name).createVertex(graph)
        Core.EX.DESCRIBES.relate(g, dsdV, reportV)

        dsdV
    }


    /** 
     * Save the data set descriptor using the provided grah traversal source.
     * @param dsd The data set descriptor
     * @param g The graph traversal source
     * @return The vertex representing the saved entity
     */
    Vertex save(GraphTraversalSource g, DataSetDescriptor dsd) {
        assert g
        assert dsd
        
        def dsdV = VX.DATA_SET_DESCRIPTOR.instance().withProperty(Core.PX.NAME, dsd.name).createVertex(graph)
        if (dsd.description) dsdV.property(Core.PX.DESCRIPTION.label, dsd.description)

        dsd.recipeIngredients.each { 
            def ingV = save(g, it)
            Core.EX.HAS_PART.relate(g, dsdV, ingV)
        }

        dsd.featureSetDescriptors.each {
            def fsdV = save(g, it)
            Core.EX.HAS_PART.relate(g, dsdV, fsdV)
        }

        dsdV
    }
    

    /** 
     * Save the feature set descriptor using the provided graph traversal
     * source.
     * @param fsd The feature set descriptor
     * @param g The graph traversal source
     * @return The vertex representing the saved entity.
     */
    Vertex save(GraphTraversalSource g, FeatureSetDescriptor fsd) {
        assert g
        assert fsd

        def fsdV = save(g, VX.FEATURE_SET_DESCRIPTOR, fsd)

        if (fsd.recipe) {
            def fsrV = save(g, fsd.recipe)
            Core.EX.HAS_PART.relate(g, fsdV, fsrV)
        }

        fsd.featureSetNames.each {
            def fsnV = VX.FEATURE_SET_NAME.instance().withProperty(Core.PX.VALUE, it).createVertex(graph)
            Core.EX.DESCRIBES.relate(g, fsdV, fsnV)
        }

        fsd.dataTypes.each { FeatureDataType fdt ->
            def fdtV = VX.FEATURE_DATA_TYPE.instance().withProperty(Core.PX.NAME, fdt.name()).createVertex(graph)
            Core.EX.DESCRIBES.relate(g, fdtV, fsdV)
        }

        fsdV
    }


    /** 
     * Save the feature set recipe using the provided graph traversal source.
     * @param fsr The feature set recipe
     * @param g The graph traversal source to use
     * @return The vertex representing the saved entity.
     */
    Vertex save(GraphTraversalSource g, FeatureSetRecipe fsr) {
        assert g
        assert fsr

        def fsrV = save(g, VX.FEATURE_SET_RECIPE, fsr)
        if (fsr.finalStep) {
            def fsrsV = recursiveSave(g, fsrV, fsr.finalStep)
            EX.HAS_FINAL_STEP.relate(g, fsrV, fsrsV)
        }

        fsrV
    }


    /** 
     * Recursively save the feature set recipe step to the feature set recipe
     * represented by the provided vertex using the provided graph traversal
     * source.
     * @param fsrs The feature set recipe step
     * @param fsrV The vertex representing the feature set recipe
     * @param g The graph traversal source to use
     * @return The vertex representing the saved entity
     */
    Vertex recursiveSave(GraphTraversalSource g, Vertex fsrV, FeatureSetRecipeStep fsrs) {
        assert g
        assert fsrV
        assert fsrs

        def fsrsV = save(g, fsrs)
        Core.EX.HAS_PART.relate(g, fsrV, fsrsV)

        fsrs.dependencies.each {
            def priorFsrsV = recursiveSave(g, fsrV, it)
            Core.EX.DEPENDS_ON.relate(g, fsrsV, priorFsrsV)
        }

        fsrsV
    }


    /** 
     * Save the feature set recipe step using the provided graph traversal
     * source.
     * @param fsrs The feature set recipe step
     * @param g The graph traversal source to use
     * @return The vertex representing the saved entity
     */
    Vertex save(GraphTraversalSource g, FeatureSetRecipeStep fsrs) {
        assert g
        assert fsrs

        def fsrsV = save(g, VX.FEATURE_SET_RECIPE_STEP, fsrs)

        fsrs.inputs.each { 
            def ingV = save(g, it)
            Core.EX.IS_INPUT_OF.relate(g, ingV, fsrsV) 
        }

        fsrs.outputs.each { 
            def ingV = save(g, it)
            Core.EX.IS_OUTPUT_OF.relate(g, ingV, fsrsV) 
        }

        fsrsV
    }


    /** 
     * Save the feature set recipe ingredient using the provided graph
     * traversal source.
     * @param ing The feature set recipe ingredient
     * @param g The grpah traversal source
     * @return The vertex representing the saved entity
     */
    Vertex save(GraphTraversalSource g, FeatureSetRecipeIngredient ing) {
        assert g
        assert ing

        def ingV = save(g, VX.FEATURE_SET_RECIPE_INGREDIENT, ing)

        ingV
    }


    /** 
     * Save the described entity using the provided vertex definition and graph
     * traversal source.
     * @param de The described entity
     * @param vdt The vertex definition
     * @param g The grpah traversal source
     * @return The vertex representing the saved entity
     */
    Vertex save(GraphTraversalSource g, VertexDefinition vdt, DescribedEntity de) {
        assert g
        assert vdt
        assert de

        def v = vdt.instance().withProperty(Core.PX.NAME, de.name).createVertex(graph)
        if (de.description) v.property(Core.PX.DESCRIPTION.label, de.description)

        v
    }

}






///////////////////////////////////////////////////////////////////////////////
// NOT CURRENTLY USED
///////////////////////////////////////////////////////////////////////////////

/*

@ToString(includeNames=true)
class FeatureSetRecipeStepVineMethod implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    FeatureSetRecipeIngredient hasOutput

}



@ToString(includeNames=true)
class FeatureSetRecipeStepGraphTraversal implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    FeatureSetRecipeIngredient hasOutput

}

*/

