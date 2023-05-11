package carnival.util



import java.text.DateFormat
import java.text.SimpleDateFormat
import static java.util.UUID.randomUUID

import groovy.transform.Memoized
import groovy.util.logging.Slf4j

import groovy.sql.*
import groovy.transform.ToString
import groovy.transform.Synchronized
import groovy.transform.WithReadLock
import groovy.transform.WithWriteLock
import static groovy.json.JsonOutput.*



/** 
 * A class intended to help describe a data set.  This class is 
 * incubating.
 *
 */
@ToString(includeNames=true)
@Slf4j
class DataSetDescriptor extends MarkdownRenderer implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a data set descriptor.
     * @param args Map of args passed to the constructor; can be used to set
     * data fields.
     * @return A DataSetDescriptor object
     */
    static DataSetDescriptor create(Map args = [:]) {
        new DataSetDescriptor(args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Default indent level for produced Markdown.
     */
    int defaultLevel = 1

    /** 
     * A set of feature set descriptors that describe the target data set.
     */
    Set<FeatureSetDescriptor> featureSetDescriptors = new HashSet<FeatureSetDescriptor>()

    /**
     * A set of recipe steps that describe the construction process of the 
     * target data set. 
     */
    Set<FeatureSetRecipeStep> recipeSteps = new HashSet<FeatureSetRecipeStep>()

    /** 
     * A set of ingredients used by the recipe steps.
     */
    Set<FeatureSetRecipeIngredient> recipeIngredients = new HashSet<FeatureSetRecipeIngredient>()


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Render this data set descriptor as a Markdown document.
     * @param args Map of arguments used to configure the markdown printer.
     * @see MarkdownRenderer#printer(Map)
     * @return Markdown text in string format.
     */
    String toMarkdown(Map args = [:]) { 
        def sp = printer(args)

        sp.printHeader(this, 'DATA SET')

        // find steps that do not appear in any feature set descriptors, print them
        /*Set<FeatureSetRecipeStep> orphanSteps = new HashSet<FeatureSetRecipeStep>()
        orphanSteps.addAll(recipeSteps)
        featureSetDescriptors.each { fsd ->
            orphanSteps.removeAll(findAllSteps(fsd))
        }
        if (orphanSteps) {
            sp.printHeader "Orphaned Steps"
            orphanSteps.each { sp.print it.toMarkdown(level:sp.level+1) }
        }*/

        // find ingredients that do not appear in any steps, print them
        /*Set<FeatureSetRecipeIngredient> orphanIngredients = new HashSet<FeatureSetRecipeIngredient>()
        orphanIngredients.addAll(recipeIngredients)
        recipeSteps.each { rs ->
            if (orphanSteps.contains(rs)) return
            orphanIngredients.removeAll(rs.inputs)
            orphanIngredients.removeAll(rs.outputs)
        }
        featureSetDescriptors.each { fsd ->
            orphanIngredients.removeAll(findAllIngredients(fsd))
        }
        if (orphanIngredients) {
            sp.printHeader "Orphaned Ingredients"
            orphanIngredients.each { sp.print it.toMarkdown(level:sp.level+1) }
        }*/

        // print feature set descriptors
        featureSetDescriptors.each { fsd ->
            sp.print fsd.toMarkdown(level:sp.level+1)
        }


        // APPENDIX - all Steps
        def allSteps = findAllSteps().unique({it.eid}).sort({it.name})
        sp.printHeader("APPENDIX: All Steps (${allSteps.size()})")
        allSteps.each { sp.print it.toMarkdown(level:sp.level+1) }


        // APPENDIX - all ingredients
        def allIngs = findAllIngredients().unique({it.eid}).sort({it.name})
        sp.printHeader("APPENDIX: All Ingredients (${allIngs.size()})")
        allIngs.each { sp.print it.toMarkdown(level:sp.level+1) }


        return sp.finalRender()
    }


    /** 
     * Traverse the data set descriptor and return a flat set of all feature
     * set recipe steps.
     * @return A flat set of all feature set recipe steps of this data set
     * descriptor.
     */
    Set<FeatureSetRecipeStep> findAllSteps() {
        Set<FeatureSetRecipeStep> steps = new HashSet<FeatureSetRecipeStep>()
        steps.addAll(recipeSteps)
        featureSetDescriptors.each { fsd ->
            steps.addAll(findAllSteps(fsd))
        }
        steps
    }


    /** 
     * Traverse the data set descriptor starting at the provided feature set
     * descriptor and return a flat set of feature set recipe steps.
     * @param fsd The feature set descriptor from which to start searching
     * @return A flat set of feature set recipe steps
     */
    Set<FeatureSetRecipeStep> findAllSteps(FeatureSetDescriptor fsd) {
        Set<FeatureSetRecipeStep> steps = new HashSet<FeatureSetRecipeStep>()
        findAllSteps(fsd.recipe.finalStep)
        steps
    }

    /** 
     * Traverse the data set descriptor starting at the provided feature set
     * descriptor and add to a flat set of feature set recipe steps.
     * @param fsd The feature set descriptor from which to start searching
     * @param steps A flat set of feature set recipe steps
     */
    void findAllSteps(Set<FeatureSetRecipeStep> steps, FeatureSetRecipeStep fs) {
        if (fs == null) return
        steps.add(fs)
        fs.dependencies.each { findAllSteps(steps, it) }
    }

    /** 
     * Traverse the data set descriptor and return a flat set of feature set
     * recipe ingredients.
     * @return The set of all FeatureSetRecipeIngredient
     */
    Set<FeatureSetRecipeIngredient> findAllIngredients() {
        Set<FeatureSetRecipeIngredient> ings = new HashSet<FeatureSetRecipeIngredient>()
        recipeSteps.each { findAllIngredients(ings, it) }
        featureSetDescriptors.each { findAllIngredients(it.finalStep) }
        ings
    }

    /** 
     * Traverse the data set descriptor starting at the provided feature set
     * desctiptor and return a flat set of feature set recipe ingredients.
     * @param fsd The feature set descriptor starting point
     * @return A set of FeatureSetRecipeIngredient
     */
    Set<FeatureSetRecipeIngredient> findAllIngredients(FeatureSetDescriptor fsd) {
        Set<FeatureSetRecipeIngredient> ings = new HashSet<FeatureSetRecipeIngredient>()
        findAllIngredients(fsd.recipe.finalStep)
        ings
    }

    /** 
     * Traverse the data set descriptor starting at the provided feature set
     * desctiptor and add to a flat set of feature set recipe ingredients.
     * @param fsd The feature set descriptor starting point
     * @param ings A set of FeatureSetRecipeIngredient
     */
    void findAllIngredients(Set<FeatureSetRecipeIngredient> ings, FeatureSetRecipeStep fs) {
        if (fs == null) return
        ings.addAll(fs.inputs)
        ings.addAll(fs.outputs)
        fs.dependencies.each { findAllIngredients(ings, it) }
    }


}



/** 
 * Encapsulates the description of a feature, including its name, long form
 * description, the query that was used to generate it, and a set of
 * FeatureDataTypes.
 *
 */
@ToString(includeNames=true)
@Slf4j
class FeatureSetDescriptor extends MarkdownRenderer implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static FeatureSetDescriptor create(Map args = [:]) {
        new FeatureSetDescriptor(args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** default indentiation level */
    int defaultLevel = 2

    /** set of feature data types */
    Set<FeatureDataType> dataTypes = new HashSet<FeatureDataType>()

    /** feature set names: NAME_1, NAME_2, etc. */
    Set<String> featureSetNames = new HashSet<String>()

    /** the feature set recipe */
    FeatureSetRecipe recipe


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return this feature set descriptor as Markdown text.
     * @see MarkdownRenderer#toMarkdown(Map)
     * @param args Optional args passed to MarkdownRenderer.printer
     * @return A Mardown representation of this object.
     */
    String toMarkdown(Map args = [:]) { 
        def sp = printer(args)

        sp.printHeader(this, 'FEATURE SET')

        if (featureSetNames) {
            sp.print "feature set names: "
            sp.println featureSetNames.collect().join(', ')
        }
        if (dataTypes) {
            sp.print "data types: "
            sp.println dataTypes.collect({it.name()}).join(', ')
        }
        if (featureSetNames || dataTypes) sp.println()

        if (recipe) sp.print recipe.toMarkdown(level:sp.level+1)

        sp.finalRender()
    }


    /** */
    FeatureSetRecipe recipe(Map args = [:]) {
        new FeatureSetRecipe(args)
    }

}



/**
 * A description of a recipe used to create a feature set.
 */
@ToString(includeNames=true)
@Slf4j
class FeatureSetRecipe extends MarkdownRenderer implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a feature set recipe.
     * @args Optional args to pass to the FeatureSetRecipe map constructor.
     * @return A FeatureSetRecipe object.
     */
    static FeatureSetRecipe create(Map args = [:]) {
        new FeatureSetRecipe(args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** The final step of this recipe */
    FeatureSetRecipeStep finalStep

    /** 
     * Default indent level for produced Markdown.
     */
    int defaultLevel = 3


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** @see MarkdownRenderer#toMarkdown(Map) */
    String toMarkdown(Map args = [:]) {
        def sp = printer(args)

        sp.printHeader(this, 'RECIPE')

        if (finalStep) sp.printSub { toMarkdownSteps(it, finalStep) }

        sp.finalRender()
    }

    /**
     * Render the provided feature recipe step to the provided MarkdownPrinter.
     * @param sp The MarkdownPrinter to use
     * @param step The step to render
     */
    void toMarkdownSteps(MarkdownPrinter sp, FeatureSetRecipeStep step) {
        log.trace "toMarkdownSteps $sp $step"

        step.dependencies.each { priorStep -> 
            toMarkdownSteps(sp, priorStep)
            sp.println()
        }
        sp.print step.toMarkdown()
    }

}



/**
 * A step in a feature set recipe.
 */
@ToString(includeNames=true)
@Slf4j
class FeatureSetRecipeStep extends MarkdownRenderer implements DescribedEntity {


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Create a feature set recipe step.
     * @param args Args to pass to the FeatureSetRecipeStep map constructor.
     * @return A FeatureSetRecipeStep object.
     */
    static FeatureSetRecipeStep create(Map args = [:]) {
        new FeatureSetRecipeStep(args)
    }

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** Default Markdown indentetion level */
    int defaultLevel = 4

    /** the inputs of this step */
    Set<FeatureSetRecipeIngredient> inputs = new HashSet<FeatureSetRecipeIngredient>()

    /** the outputs of this step */
    Set<FeatureSetRecipeIngredient> outputs = new HashSet<FeatureSetRecipeIngredient>()

    /** the dependencies of this step */
    Set<FeatureSetRecipeStep> dependencies = new HashSet<FeatureSetRecipeStep>()


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** @see MarkdownRenderer#toMarkdown(Map) */
    String toMarkdown(Map args = [:]) {
        log.trace "FeatureSetRecipeStep toMarkdown $name $args"

        def sp = printer(args)
        sp.printHeader(this, 'STEP')

        if (dependencies) {
            sp.print "* dependencies: "
            sp.println dependencies.collect({it.name}).join(', ')
        }
        if (inputs) {
            sp.print "* inputs: "
            sp.println inputs.collect({it.name}).join(', ')
        }
        if (outputs) {
            sp.print "* outputs: "
            sp.println outputs.collect({it.name}).join(', ')

            outputs.each { 
                sp.println()
                sp.print it.toMarkdown(level:sp.level+1) 
            }
        }

        sp.finalRender()
    }

    /** 
     * Add a dependent step.
     * @param priorStep The dependent step.
     * @return The priorStep argument.
     */
    FeatureSetRecipeStep dependsOn(FeatureSetRecipeStep priorStep) {
        this.dependencies << priorStep
        priorStep
    }

    /** 
     * Add an input ingredient to this step.
     * @param ingredient The input ingredient
     * @return This object
     */
    FeatureSetRecipeStep input(FeatureSetRecipeIngredient ingredient) {
        this.inputs << ingredient
        this
    }

    /** 
     * Create a new feature set recipe ingredient using the optional args
     * and add it as an input.
     * @param args An optional map to sent to the FeatureSetRecipeIngredient 
     * map constructor.
     * @return This object
     */
    FeatureSetRecipeStep input(Map args = [:]) {
        this.inputs << new FeatureSetRecipeIngredient(args)
        this
    }

    /** 
     * Add an output ingredient to this step.
     * @param ingredient The output ingredient
     * @return This object
     */
    FeatureSetRecipeStep output(FeatureSetRecipeIngredient ingredient) {
        this.outputs << ingredient
        this
    }

    /** 
     * Create a new feature set recipe ingredient using the optional args
     * and add it as an output.
     * @param args An optional map to sent to the FeatureSetRecipeIngredient 
     * map constructor.
     * @return This object
     */
    FeatureSetRecipeStep output(Map args = [:]) {
        this.outputs << new FeatureSetRecipeIngredient(args)
        this
    }

}



/**
 * A feature set recipe ingredient.
 */
@ToString(includeNames=true)
@Slf4j
class FeatureSetRecipeIngredient extends MarkdownRenderer implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** default Markdown indentation level */
    int defaultLevel = 3

    /** The step that produced this ingredient */
    FeatureSetRecipeStep outputOf


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** @see MarkdownRenderer#toMarkdown(Map) */
    String toMarkdown(Map args = [:]) {
        def sp = printer(args)
        sp.printHeader(this, 'INGREDIENT')
        sp.finalRender()
    }
}


/** 
 * A convenience object to encapsulate string printing.
 */
@ToString(includeNames=true)
class StringPrinter {
    /** internal StringWriter */
    StringWriter sw = new StringWriter()

    /** internal PrintWriter */
    PrintWriter pw = new PrintWriter(sw)

    /**
     * Render the text as a String and close this writer.
     * @return The String.
     */
    String finalRender() {
        def str = this.sw.toString()
        this.close()
        str
    }

    /**
     * Render the text as a String.
     * @return The String.
     */
    String render() {
        this.sw.toString()
    }

    /**
     * Close this writer and any internal writers.
     */
    void close() {
        this.sw.close()
        this.pw.close()
    }

    /**
     * @see PrintWriter#print(String) 
     */
    void print(String str) { pw.print(str) }

    /**
     * @see PrintWriter#println(String)
     */
    void println(String str) { pw.println(str) }

    /**
     * @see PrintWriter#println()
     */
    void println() { pw.println() }
}


/** 
 * An object to facilitate printing in Markdown format.
 *
 */
@ToString(includeNames=true)
class MarkdownPrinter extends StringPrinter {

    /** header level */
    int level = 1

    /**
     * Set the header level.
     * @param i The header level
     */
    void setLevel(int i) { level = i }

    /**
     * Incremenet the header level by 1.
     */
    void incrementLevel() { level++ }

    /**
     * Decrement the header level by 1.
     */
    void decrementLevel() { level-- }

    /**
     * Return hashes per the current header level.
     * @return A string of hashes
     */
    String hashes() { 
        def buf = new StringBuffer()
        (1..level).each { buf.append('#') }
        buf.toString()
    }

    /**
     * Print the provided string as a header at the current header level.
     * @param str The string
     */
    void printHeader(String str) {
        println "${hashes()} $str"
    }

    /**
     * Return a Markdown with a header level +1 of this object.
     * @return A MarkdownPrinter one header level down.
     */
    MarkdownPrinter subPrinter() {
        new MarkdownPrinter(level:this.level+1)
    }

    /**
     * Create a sub-printer, pass it to the provided closure as a parameter,
     * call the closure, then print the result to this object.
     * @param cl Closure that accepts and prints to a MarkdownPrinter
     */
    void printSub(Closure cl) {
        def mp = subPrinter()
        cl(mp)
        this.print(mp.finalRender())
    }

    /**
     * Print a header for the provided described entity with an optional tag.
     * @param desc The DescribedEntity to print
     * @param tag Optional string tag to include in printed text.
     */
    void printHeader(DescribedEntity desc, String tag = null) {
        def str = tag ? "${tag}: " : ""
        str = "${str}${desc.name}"
        printHeader(str)
        if (desc.description) println "${desc.description}"
        println()
    }
}


/** 
 * Addes a randomly generated UUID.
 */
trait IdentifiedEntity {
    /** randomly generated UUID stripped of dashes. */
    String eid = randomUUID().toString().replaceAll('-','')
}

/**
 * Adds a name property.
 */
trait NamedEntity extends IdentifiedEntity {

    /** String name */
    String name
}

/** 
 * Adds a description property.
 */
trait DescribedEntity extends NamedEntity {

    /** String description */
    String description
}


/** 
 * Abstract class to facilitate rendering an object as Markdown.
 */
abstract class MarkdownRenderer {
    
    /**
     * Return the object as Markdown text
     * @return The Markdown text as a string
     */
    abstract String toMarkdown()
    
    /**
     * Create a MarkdownPrinter.
     * @param args.level Optional set the printer indentation level
     * @return A MarkdownPrinter
     */
    MarkdownPrinter printer(Map args = [:]) {
        Integer level = args.level
        if (level == null && this.hasProperty('defaultLevel')) level = this.defaultLevel
        if (level == null) level = 1
        new MarkdownPrinter(level:level)
    }

    /*void toMarkdownEntityTree(MarkdownPrinter sp, MarkdownRenderer entity, String propertyName) {
        log.trace "toMarkdownEntityTree $sp $entity $propertyName"

        sp.print entity.toMarkdown()
        entity."$propertyName".each { subEntity -> 
            sp.println()
            toMarkdownEntityTree(sp, subEntity)
        }
    }*/  
}


/** 
 * An enumeration of data type descriptors to describe feature sets. Any number
 * of descriptors can be combined to describe a feature set.
 *
 */
enum FeatureDataType {
    /** a real number */
    NUMBER('a real number'), 

    /** a rational number */
    RATIONAL('a rational number'), 

    /** an integer */
    INTEGER('an integer'),
    
    /** a non-negative number */
    NON_NEGATIVE('a non-negative number'),

    /** a positive number */
    POSITIVE('a positive number'),

    /** a negative number */
    NEGATIVE('a negative number'),

    /** the magnitude of a number without regard to its sign*/
    ABSOLUTE_VALUE('the magnitude of a number without regard to its sign'),

    /** represents a count of things */
    COUNT('represents a count of things'),

    /** a Boolean true/false value */
    BOOLEAN('a Boolean true/false value'), 

    /** a sequence of alphanumeric characters */
    STRING('a sequence of alphanumeric characters'),

    /** a date at least as specific as the day of year, potentially without a time of day component */
    DATE('a date at least as specific as the day of year, potentially without a time of day component'),

    /** a date with a time of day component */
    DATETIME('a date with a time of day component'),

    /** a value whose meaning is defined in a dictionary */
    CODE('a value whose meaning is defined in a dictionary')

    /** a description of the enum value */
    String description

    /**
     * Constructor that sets the description.
     * @param description The description to use
     */
    FeatureDataType(String description) {
        this.description = description
    }
}



/** 
 * A class that counts things.
*/
class EntityCounter {
    /** map of counts of things */
    Map<String,Integer> counts = new HashMap<String,Integer>()
    
    /**
     * Add to the count of things keyed by the provided string.
     * @str The String key
     */
    void count(String str) {
        if (!counts.containsKey(str)) counts.put(str, 0)
        counts.out(str, counts.get(str)+1)
    }

    /** 
     * Add to the count of things keyed by the provided enum.
     * @en The Enum key
     */
    void count(Enum en) { count(en.name()) }
}






