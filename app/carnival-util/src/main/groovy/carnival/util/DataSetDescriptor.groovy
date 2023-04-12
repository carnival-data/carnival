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

    /** */
    void findAllSteps(Set<FeatureSetRecipeStep> steps, FeatureSetRecipeStep fs) {
        if (fs == null) return
        steps.add(fs)
        fs.dependencies.each { findAllSteps(steps, it) }
    }

    /** */
    Set<FeatureSetRecipeIngredient> findAllIngredients() {
        Set<FeatureSetRecipeIngredient> ings = new HashSet<FeatureSetRecipeIngredient>()
        recipeSteps.each { findAllIngredients(ings, it) }
        featureSetDescriptors.each { findAllIngredients(it.finalStep) }
        ings
    }

    /** */
    Set<FeatureSetRecipeIngredient> findAllIngredients(FeatureSetDescriptor fsd) {
        Set<FeatureSetRecipeIngredient> ings = new HashSet<FeatureSetRecipeIngredient>()
        findAllIngredients(fsd.recipe.finalStep)
        ings
    }

    /** */
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

    /** */
    int defaultLevel = 2

    /** */
    Set<FeatureDataType> dataTypes = new HashSet<FeatureDataType>()

    /** NAME_1, NAME_2, etc. */
    Set<String> featureSetNames = new HashSet<String>()

    /** */
    FeatureSetRecipe recipe


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
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
 *
 */
@ToString(includeNames=true)
@Slf4j
class FeatureSetRecipe extends MarkdownRenderer implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static FeatureSetRecipe create(Map args = [:]) {
        new FeatureSetRecipe(args)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    FeatureSetRecipeStep finalStep

    /** */
    int defaultLevel = 3


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    String toMarkdown(Map args = [:]) {
        def sp = printer(args)

        sp.printHeader(this, 'RECIPE')

        if (finalStep) sp.printSub { toMarkdownSteps(it, finalStep) }

        sp.finalRender()
    }

    /** */
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
 *
 */
@ToString(includeNames=true)
@Slf4j
class FeatureSetRecipeStep extends MarkdownRenderer implements DescribedEntity {


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static FeatureSetRecipeStep create(Map args = [:]) {
        new FeatureSetRecipeStep(args)
    }

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    int defaultLevel = 4

    /** */
    Set<FeatureSetRecipeIngredient> inputs = new HashSet<FeatureSetRecipeIngredient>()

    /** */
    Set<FeatureSetRecipeIngredient> outputs = new HashSet<FeatureSetRecipeIngredient>()

    /** */
    Set<FeatureSetRecipeStep> dependencies = new HashSet<FeatureSetRecipeStep>()


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
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

    /** */
    FeatureSetRecipeStep dependsOn(FeatureSetRecipeStep priorStep) {
        this.dependencies << priorStep
        priorStep
    }

    /** */
    FeatureSetRecipeStep input(FeatureSetRecipeIngredient ingredient) {
        this.inputs << ingredient
        this
    }

    /** */
    FeatureSetRecipeStep input(Map args = [:]) {
        this.inputs << new FeatureSetRecipeIngredient(args)
        this
    }

    /** */
    FeatureSetRecipeStep output(FeatureSetRecipeIngredient ingredient) {
        this.outputs << ingredient
        this
    }

    /** */
    FeatureSetRecipeStep output(Map args = [:]) {
        this.outputs << new FeatureSetRecipeIngredient(args)
        this
    }

}



/**
 *
 */
@ToString(includeNames=true)
@Slf4j
class FeatureSetRecipeIngredient extends MarkdownRenderer implements DescribedEntity {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    int defaultLevel = 3

    /** */
    FeatureSetRecipeStep outputOf


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    String toMarkdown(Map args = [:]) {
        def sp = printer(args)
        sp.printHeader(this, 'INGREDIENT')
        sp.finalRender()
    }
}


/** */
@ToString(includeNames=true)
class StringPrinter {
    StringWriter sw = new StringWriter()
    PrintWriter pw = new PrintWriter(sw)
    String finalRender() {
        def str = this.sw.toString()
        this.close()
        str
    }
    String render() {
        this.sw.toString()
    }
    void close() {
        this.sw.close()
        this.pw.close()
    }
    void print(String str) { pw.print(str) }
    void println(String str) { pw.println(str) }
    void println() { pw.println() }
}


/** */
@ToString(includeNames=true)
class MarkdownPrinter extends StringPrinter {
    int level = 1
    void setLevel(int i) { level = i }
    void incrementLevel() { level++ }
    void decrementLevel() { level-- }
    String hashes() { 
        def buf = new StringBuffer()
        (1..level).each { buf.append('#') }
        buf.toString()
    }
    void printHeader(String str) {
        println "${hashes()} $str"
    }
    MarkdownPrinter subPrinter() {
        new MarkdownPrinter(level:this.level+1)
    }
    void printSub(Closure cl) {
        def mp = subPrinter()
        cl(mp)
        this.print(mp.finalRender())
    }
    void printHeader(DescribedEntity desc, String tag = null) {
        def str = tag ? "${tag}: " : ""
        str = "${str}${desc.name}"
        printHeader(str)
        if (desc.description) println "${desc.description}"
        println()
    }
}


/** */
trait IdentifiedEntity {
    String eid = randomUUID().toString().replaceAll('-','')
}

/** */
trait NamedEntity extends IdentifiedEntity {
    String name
}

/** */
trait DescribedEntity extends NamedEntity {
    String description
}


/** */
abstract class MarkdownRenderer {
    
    abstract String toMarkdown()
    
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
    NUMBER('a real number'), 
    RATIONAL('a rational number'), 
    INTEGER('an integer'),
    
    NON_NEGATIVE('a non-negative number'),
    POSITIVE('a positive number'),
    NEGATIVE('a negative number'),
    ABSOLUTE_VALUE('the magnitude of a number without regard to its sign'),
    COUNT('represents a count of things'),

    BOOLEAN('a Boolean true/false value'), 
    STRING('a sequence of alphanumeric characters'),
    DATE('a date at least as specific as the day of year, potentially without a time of day component'),
    DATETIME('a date with a time of day component'),
    CODE('a value whose meaning is defined in a dictionary')

    String description

    FeatureDataType(String description) {
        this.description = description
    }
}



/** */
class EntityCounter {
    Map<String,Integer> counts = new HashMap<String,Integer>()
    void count(String str) {
        if (!counts.containsKey(str)) counts.put(str, 0)
        counts.out(str, counts.get(str)+1)
    }
    void count(Enum en) { count(en.name()) }
}






