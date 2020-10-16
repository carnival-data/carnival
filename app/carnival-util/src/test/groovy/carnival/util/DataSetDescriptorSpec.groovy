package carnival.util


import groovy.sql.*
import groovy.transform.InheritConstructors

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import carnival.graph.*




/**
 * gradle test --tests "carnival.core.util.DataSetDescriptorSpec"
 *
 */
class DataSetDescriptorSpec extends Specification {


    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////


    @Shared testDate



    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    def setupSpec() {
        testDate = new Date("01/01/2000")
    } 


    def cleanupSpec() {
    }




    ///////////////////////////////////////////////////////////////////////////
    // FEATURE SET RECIPE
    ///////////////////////////////////////////////////////////////////////////

    /*def "markdown data set descriptor simple"() {
        def md
        def dsd
        def fsd
        def recipe

        when:
        dsd = new DataSetDescriptor()
        def ing1 = new FeatureSetRecipeIngredient(name:'ingredient 1', description:'the first ingredient')
        dsd.recipeIngredients << ing1

        md = dsd.toMarkdown()
        println md

        then:
        md.contains 'Orphaned Ingredients'
        md.contains 'ingredient 1'
        md.contains 'the first ingredient'
    }*/



    def "markdown feature set descriptor simple"() {
        def md
        def fsd
        def recipe

        when:
        fsd = new FeatureSetDescriptor(
            name:'SOME_CODE',
            description:'a code for something',
            dataTypes:[FeatureDataType.NUMBER],
            featureSetNames:['SOME_CODE_1', 'SOME_CODE_2']
        )
        fsd.recipe = new FeatureSetRecipe(name:'simple recipe', description:'simple recipe for testing')
        def ing1 = new FeatureSetRecipeIngredient(name:'ingredient 1', description:'the first ingredient')
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
        
        md = fsd.toMarkdown()
        println md

        then:
        md.contains('SOME_CODE')
        md.contains('SOME_CODE_1')
        md.contains('SOME_CODE_2')
        md.contains('a code for something')
        md.contains("${FeatureDataType.NUMBER}")
        md.contains('simple recipe')
        md.contains('step 1')
        md.contains('step 2')
        md.contains('ingredient 1')
        md.contains('the first ingredient')
        md.indexOf('step 1') < md.indexOf('step 2')
    }


    def "markdown recipe with outputs"() {
        def md
        def recipe

        when:
        recipe = new FeatureSetRecipe(name:'simple recipe', description:'simple recipe for testing')
        recipe.finalStep = FeatureSetRecipeStep
            .create(name:'final step', description:'produce final result')
            .output(name:'ingredient 1', description:'the first ingredient')
        
        md = recipe.toMarkdown()
        println md

        then:
        md.contains('simple recipe')
        md.contains('final step')
        md.contains('ingredient 1')
        md.contains('the first ingredient')
    }


    def "markdown recipe with inputs"() {
        def md
        def recipe

        when:
        recipe = new FeatureSetRecipe(name:'simple recipe', description:'simple recipe for testing')
        recipe.finalStep = FeatureSetRecipeStep
            .create(name:'final step', description:'produce final result')
            .input(name:'ingredient 1', description:'the first ingredient')
        
        md = recipe.toMarkdown()
        println md

        then:
        md.contains('simple recipe')
        md.contains('final step')
        md.contains('ingredient 1')
    }



    def "markdown single dependency"() {
        def md
        def recipe

        when:
        recipe = new FeatureSetRecipe(name:'simple recipe', description:'simple recipe for testing')
        recipe.finalStep = new FeatureSetRecipeStep(name:'final step', description:'produce final result')
        recipe.finalStep.dependsOn(new FeatureSetRecipeStep(name:'step 1', description:'do the first step'))
        
        md = recipe.toMarkdown()
        println md

        then:
        md.contains('simple recipe')
        md.contains('final step')
        md.contains('step 1')
    }



    def "markdown simple recipe"() {
        def file = new File("markdown simple recipe.md")
        def md
        def recipe

        when:
        recipe = new FeatureSetRecipe(name:'simple recipe', description:'simple recipe for testing')
        recipe.finalStep = new FeatureSetRecipeStep(name:'final step', description:'produce final result')
        
        md = recipe.toMarkdown()
        println md
        //file.write(md)

        then:
        md.contains('simple recipe')
        md.contains('final step')
    }


    def "FeatureSetRecipeIngredient toMarkdown"() {
        def md
        def ing

        when:
        ing = new FeatureSetRecipeIngredient(name:'ing 1', description:'ing 1 description')
        md = ing.toMarkdown()
        println "$md"

        then:
        md.contains('ing 1')
        md.contains('ing 1 description')
    }


    def "StringPrinter"() {
        when:
        def sp1 = new StringPrinter()
        sp1.print('a line')
        def str = sp1.render()

        then:
        str == 'a line'

        cleanup:
        sp1.close()
    }


    def "objects have name and decription"() {
        when:
        def thing = thingClass.newInstance(name:'thing name', description:'thing description')

        then:
        thing.name == 'thing name'
        thing.description == 'thing description'

        where:
        thingClass << [FeatureSetDescriptor, FeatureSetRecipe, FeatureSetRecipeStep, FeatureSetRecipeIngredient]
    }











}