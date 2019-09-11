package carnival.core.graph



import java.text.DateFormat
import java.text.SimpleDateFormat

import groovy.transform.Memoized

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

import carnival.util.KeyType
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

import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.VertexDefTrait




/**
 *
 *
 */
class DataSetDescriptorGraph implements GremlinTrait {


    ///////////////////////////////////////////////////////////////////////////
    // GRAPH MODEL
    ///////////////////////////////////////////////////////////////////////////

    static enum VX implements VertexDefTrait {
        FEATURE_REPORT(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true)
            ]
        ),
        DATA_SET_DESCRIPTOR(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION
            ]
        ),
        FEATURE_SET_DESCRIPTOR(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),
        FEATURE_SET_RECIPE(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),
        FEATURE_SET_RECIPE_STEP(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),
        FEATURE_SET_RECIPE_INGREDIENT(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
                Core.PX.DESCRIPTION,
            ]
        ),
        FEATURE_DATA_TYPE(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true),
            ]
        ),
        FEATURE_SET_NAME(
            vertexProperties:[
                Core.PX.VALUE.withConstraints(required:true),
            ]
        )

        private VX() {}
        private VX(Map m) { if (m.vertexProperties) this.vertexProperties = m.vertexProperties }
    }


    /** */
    static enum EX implements EdgeDefTrait {
        HAS_FINAL_STEP
    }


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    ///////////////////////////////////////////////////////////////////////////

    /** */
    DataSetDescriptorGraph(Graph graph) {
        this.graph = graph
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    DataSetDescriptor load(Vertex dataSetDescriptorV) {
        withTraversal { g ->
            load(g, dataSetDescriptorV)
        }
    }


    /** */
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
            log.debug "${it} ${it.label()}"
            dsd.recipeSteps << loadFeatureSetRecipeStep(g, it)
        }


        dsd
    }


    /** */
    FeatureSetDescriptor loadFeatureSetDescriptor(GraphTraversalSource g, fsdV) {
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


    /** */
    FeatureSetRecipe loadFeatureSetRecipe(GraphTraversalSource g, fsrV) {
        def fsr = load(g, FeatureSetRecipe, fsrV)

        g.V(fsrV)
            .out(EX.HAS_FINAL_STEP.label)
            .hasLabel(VX.FEATURE_SET_RECIPE_STEP.label)
        .toSet().each {
            fsr.finalStep = recursiveLoadFeatureSetRecipeSteps(g, it)
        }

        fsr
    }


    /** */
    FeatureSetRecipeStep loadFeatureSetRecipeStep(GraphTraversalSource g, fsrsV) {
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


    /** */
    FeatureSetRecipeStep recursiveLoadFeatureSetRecipeSteps(GraphTraversalSource g, fsrsV) {
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


    /** */
    FeatureSetRecipeIngredient loadFeatureSetRecipeIngredient(GraphTraversalSource g, ingV) {
        load(g, FeatureSetRecipeIngredient, ingV)
    }


    /** */
    DescribedEntity load(GraphTraversalSource g, Class targetClass, Vertex entityV) {
        DescribedEntity ent = targetClass.newInstance()
        ent.eid = entityV.id()
        ent.name = entityV.value(Core.PX.NAME.label)
        if (entityV.property(Core.PX.DESCRIPTION.label).isPresent()) ent.description = entityV.value(Core.PX.DESCRIPTION.label)
        ent
    }


    /** */
    Vertex save(DataSetDescriptor dsd, FeatureReport report) {
        withTraversal { g -> save(g, dsd) }
    }


    /** */
    Vertex save(GraphTraversalSource g, DataSetDescriptor dsd, FeatureReport report) {
        assert g
        assert dsd

        def dsdV = save(g, dsd)

        def reportV = VX.FEATURE_REPORT.instance().withProperty(Core.PX.NAME, report.name).createVertex(graph, g)
        Core.EX.DESCRIBES.relate(g, dsdV, reportV)

        dsdV
    }


    /** */
    Vertex save(GraphTraversalSource g, DataSetDescriptor dsd) {
        assert g
        assert dsd
        
        def dsdV = VX.DATA_SET_DESCRIPTOR.instance().withProperty(Core.PX.NAME, dsd.name).createVertex(graph, g)
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
    

    /** */
    Vertex save(GraphTraversalSource g, FeatureSetDescriptor fsd) {
        assert g
        assert fsd

        def fsdV = save(g, VX.FEATURE_SET_DESCRIPTOR, fsd)

        if (fsd.recipe) {
            def fsrV = save(g, fsd.recipe)
            Core.EX.HAS_PART.relate(g, fsdV, fsrV)
        }

        fsd.featureSetNames.each {
            def fsnV = VX.FEATURE_SET_NAME.instance().withProperty(Core.PX.VALUE, it).createVertex(graph, g)
            Core.EX.DESCRIBES.relate(g, fsdV, fsnV)
        }

        fsd.dataTypes.each { FeatureDataType fdt ->
            def fdtV = VX.FEATURE_DATA_TYPE.instance().withProperty(Core.PX.NAME, fdt.name()).createVertex(graph, g)
            Core.EX.DESCRIBES.relate(g, fdtV, fsdV)
        }

        fsdV
    }


    /** */
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


    /** */
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


    /** */
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


    /** */
    Vertex save(GraphTraversalSource g, FeatureSetRecipeIngredient ing) {
        assert g
        assert ing

        def ingV = save(g, VX.FEATURE_SET_RECIPE_INGREDIENT, ing)

        ingV
    }


    /** */
    Vertex save(GraphTraversalSource g, VertexDefTrait vdt, DescribedEntity de) {
        assert g
        assert vdt
        assert de

        def v = vdt.instance().withProperty(Core.PX.NAME, de.name).createVertex(graph, g)
        if (de.description) v.property(Core.PX.DESCRIPTION.label, de.description)

        v
    }

}






///////////////////////////////////////////////////////////////////////////////
// NOT CURRENTLY USED
///////////////////////////////////////////////////////////////////////////////

/**
 *
 */
@ToString(includeNames=true)
class FeatureSetRecipeStepVineMethod implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    FeatureSetRecipeIngredient hasOutput

}



/**
 *
 */
@ToString(includeNames=true)
class FeatureSetRecipeStepGraphTraversal implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static Logger log = LoggerFactory.getLogger('carnival')


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    FeatureSetRecipeIngredient hasOutput

}



