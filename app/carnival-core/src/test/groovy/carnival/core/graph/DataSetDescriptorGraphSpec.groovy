package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.util.FeatureReport
import carnival.util.DataSetDescriptor
import carnival.util.FeatureSetDescriptor
import carnival.util.FeatureSetRecipe
import carnival.util.FeatureSetRecipeStep
import carnival.util.FeatureSetRecipeIngredient
import carnival.util.FeatureDataType

import carnival.graph.*



/**
 * gradle test --tests "carnival.core.graph.DataSetDescriptorGraphSpec"
 *
 */
class DataSetDescriptorGraphSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // DEFS
    ///////////////////////////////////////////////////////////////////////////

    static enum VX implements VertexDefinition {
        CGS_SUITCASE
    }

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared carnival
    @Shared graph
    @Shared g
    

    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    

    def setupSpec() {
    } 

    def setup() {
        carnival = CarnivalTinker.create()
        graph = carnival.graph
        g = graph.traversal()
    }

    def cleanup() {
        if (g) g.close()
        if (graph) graph.close()
    }

    def cleanupSpec() {
    }



    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "reload basic"() {
        def dsd
        def dsdV
        def dsdGraph
        def report

        given:
        dsdGraph = new DataSetDescriptorGraph(graph)        

        when:
        report = new FeatureReport(
            name:'test report 1',
            idFieldName:'ID'
        )
        dsd = new DataSetDescriptor(name:'dsd 1', description:'description for dsd 1')
        def ing1 = new FeatureSetRecipeIngredient(name:'ingredient 1', description:'the first ingredient')
        dsd.recipeIngredients << ing1
        def fsd = new FeatureSetDescriptor(
            name:'SOME_CODE',
            description:'a code for something',
            dataTypes:[FeatureDataType.NUMBER],
            featureSetNames:['SOME_CODE_1', 'SOME_CODE_2']
        )
        fsd.recipe = new FeatureSetRecipe(name:'simple recipe', description:'simple recipe for testing')
        def step0 = FeatureSetRecipeStep.create(
            name:'step 0', 
            description:'init step'
        )
        def step1 = FeatureSetRecipeStep.create(
            name:'step 1', 
            description:'the first step',
            outputs:[ing1]
        )
        step1.dependsOn(step0)
        def step2 = FeatureSetRecipeStep.create(
            name:'step 2', 
            description:'the second step',
            inputs:[ing1]
        )
        step2.dependsOn(step1)
        fsd.recipe.finalStep = step2
        dsd.featureSetDescriptors << fsd

        dsdV = dsdGraph.save(g, dsd, report)

        def dsdReloaded = dsdGraph.load(dsdV)

        then:
        dsdReloaded
        
        dsdReloaded.recipeIngredients 
        dsdReloaded.recipeIngredients.size() == 1 
        dsdReloaded.recipeIngredients.first().name == 'ingredient 1'

        dsdReloaded.recipeSteps        
        dsdReloaded.recipeSteps.size() == 3
        dsdReloaded.recipeSteps.find({it.name == 'step 0'})
        dsdReloaded.recipeSteps.find({it.name == 'step 1'})
        dsdReloaded.recipeSteps.find({it.name == 'step 2'})

        dsdReloaded.featureSetDescriptors 
        dsdReloaded.featureSetDescriptors.size() == 1 
        dsdReloaded.featureSetDescriptors.first().name == 'SOME_CODE'

        when:
        def fsdReloaded = dsdReloaded.featureSetDescriptors.first()

        then:
        fsdReloaded.featureSetNames
        fsdReloaded.featureSetNames.size() == 2
        fsdReloaded.featureSetNames.contains('SOME_CODE_1')
        fsdReloaded.featureSetNames.contains('SOME_CODE_2')
        fsdReloaded.dataTypes
        fsdReloaded.dataTypes.size() == 1

        when:        
        def fsdrReloaded = fsdReloaded.recipe

        then:
        fsdrReloaded
        fsdrReloaded.name == 'simple recipe'

        when:
        def fsdrFinalStepReloaded = fsdrReloaded.finalStep

        then:
        fsdrFinalStepReloaded
        fsdrFinalStepReloaded.name == 'step 2'

        when:
        def step2Reloaded = fsdrFinalStepReloaded

        then:
        step2Reloaded.dependencies
        step2Reloaded.dependencies.size() == 1
        step2Reloaded.inputs
        step2Reloaded.inputs.size() == 1

        when:
        def step1Reloaded = step2Reloaded.dependencies.first()

        then:
        step1Reloaded.name == 'step 1'        
        step1Reloaded.outputs
        step1Reloaded.outputs.size() == 1
        step1Reloaded.dependencies
        step1Reloaded.dependencies.size() == 1

        when:
        def step0Reloaded = step1Reloaded.dependencies.first()

        then:
        step0Reloaded.name == 'step 0'        
        step0Reloaded.outputs.size() == 0
        step0Reloaded.dependencies.size() == 0

    }


    def "save basic"() {
        def dsd
        def dsdV
        def dsdGraph
        def report

        given:
        dsdGraph = new DataSetDescriptorGraph(graph)        

    	when:
        report = new FeatureReport(
            name:'test report 1',
            idFieldName:'ID'
        )
        dsd = new DataSetDescriptor(name:'dsd 1', description:'description for dsd 1')
        
        dsdV = dsdGraph.save(g, dsd, report)

    	then:
        dsdV
        dsdV.property(Core.PX.NAME.label).isPresent()
        dsdV.value(Core.PX.NAME.label) == 'dsd 1'
        dsdV.property(Core.PX.DESCRIPTION.label).isPresent()
        dsdV.value(Core.PX.DESCRIPTION.label) == 'description for dsd 1'
        g.V(dsdV)
            .out(Core.EX.DESCRIBES.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_REPORT.label)
        .tryNext().isPresent()

        when:
        def ing1 = new FeatureSetRecipeIngredient(name:'ingredient 1', description:'the first ingredient')
        dsd.recipeIngredients << ing1
        
        dsdV = dsdGraph.save(g, dsd, report)

        then:
        dsd.recipeIngredients.size() == 1
        g.V(dsdV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_SET_RECIPE_INGREDIENT.label)
            .has(Core.PX.NAME.label, 'ingredient 1')
            .has(Core.PX.DESCRIPTION.label, 'the first ingredient')
        .tryNext().isPresent()

        when:
        def fsd = new FeatureSetDescriptor(
            name:'SOME_CODE',
            description:'a code for something',
            dataTypes:[FeatureDataType.NUMBER],
            featureSetNames:['SOME_CODE_1', 'SOME_CODE_2']
        )
        fsd.recipe = new FeatureSetRecipe(name:'simple recipe', description:'simple recipe for testing')
        def step1 = FeatureSetRecipeStep.create(
            name:'step 1', 
            description:'the first step',
            outputs:[ing1]
        )
        def step2 = FeatureSetRecipeStep.create(
            name:'step 2', 
            description:'the second step',
            inputs:[ing1]
        )
        step2.dependsOn(step1)
        fsd.recipe.finalStep = step2
        dsd.featureSetDescriptors << fsd

        dsdV = dsdGraph.save(g, dsd, report)

        def fsdV = g.V(dsdV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_SET_DESCRIPTOR.label)
            .has(Core.PX.NAME.label, 'SOME_CODE')
            .has(Core.PX.DESCRIPTION.label, 'a code for something')
        .tryNext()

        then:
        fsdV.isPresent()

        when:
        fsdV = fsdV.get()

        then:
        fsdV
        fsdV instanceof Vertex

        when:
        def recipeV = g.V(fsdV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_SET_RECIPE.label)
            .has(Core.PX.NAME.label, 'simple recipe')
            .has(Core.PX.DESCRIPTION.label, 'simple recipe for testing')
        .tryNext()

        then:
        recipeV.isPresent()

        when:
        recipeV = recipeV.get()
        def step2V = g.V(recipeV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_SET_RECIPE_STEP.label)
            .has(Core.PX.NAME.label, 'step 2')
            .has(Core.PX.DESCRIPTION.label, 'the second step')
        .tryNext()

        then:
        step2V.isPresent()

        when:
        step2V = step2V.get()
        def step1V = g.V(recipeV)
            .out(Core.EX.HAS_PART.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_SET_RECIPE_STEP.label)
            .has(Core.PX.NAME.label, 'step 1')
            .has(Core.PX.DESCRIPTION.label, 'the first step')
        .tryNext()

        then:
        step1V.isPresent()

        when:
        step1V = step1V.get()

        then:
        g.V(step2V)
            .out(Core.EX.DEPENDS_ON.label)
            .is(step1V)
        .tryNext().isPresent()

        when:
        def finalStepV = g.V(recipeV)
            .out(DataSetDescriptorGraph.EX.HAS_FINAL_STEP.label)
            .is(step2V)
        .tryNext()

        then:
        finalStepV.isPresent()

        when:
        def ing1V = g.V(step1V)
            .in(Core.EX.IS_OUTPUT_OF.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_SET_RECIPE_INGREDIENT.label)
            .has(Core.PX.NAME.label, 'ingredient 1')
            .has(Core.PX.DESCRIPTION.label, 'the first ingredient')
        .tryNext()

        then:
        ing1V.isPresent()

        when:
        ing1V = ing1V.get()

        then:
        g.V(step2V)
            .in(Core.EX.IS_INPUT_OF.label)
            .hasLabel(DataSetDescriptorGraph.VX.FEATURE_SET_RECIPE_INGREDIENT.label)
            .has(Core.PX.NAME.label, 'ingredient 1')
            .has(Core.PX.DESCRIPTION.label, 'the first ingredient')
        .tryNext().isPresent()
    }

}

