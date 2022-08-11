package carnival.graph



import groovy.transform.CompileStatic
import org.codehaus.groovy.macro.methods.MacroGroovyMethods
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MixinNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.ast.expr.*



/** 
 * Superclass for AST definition transformations that add traits to enum
 * definitions.  NOTE - the compile phase must be at least as early
 * as SEMANTIC_ANALYSIS. Any later, and the groovy machinery will not
 * apply the trait, only the interface.
 * 
 * @see carnival.graph.VertexDefinitionTransformation
 * @see carnival.graph.EdgeDefinitionTransformation
 * @see carnival.graph.PropertyDefinitionTransformation
 *
 */
abstract class DefinitionTransformation extends AbstractASTTransformation {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static void addTrait(ClassNode classNode, Class traitClass) {
        ClassNode[] interfaces = classNode.getInterfaces()
        ClassNode traitClassNode = new ClassNode(traitClass)
        if (interfaces.contains(traitClassNode)) return

        List<ClassNode> finalInterfaces = new ArrayList<ClassNode>()
        finalInterfaces.add(traitClassNode)
        finalInterfaces.addAll(interfaces)

        ClassNode[] finalInterfacesArray = finalInterfaces.toArray(Class[])
        classNode.setInterfaces(finalInterfacesArray)
    }


    /** */
    static addNoArgConstructor(ClassNode classNode, Statement stmt = new EmptyStatement()) {
        ConstructorNode noArgConstructor = 
            new ConstructorNode(
                ClassNode.ACC_PRIVATE, 
                [] as Parameter[],
                [] as ClassNode[],
                stmt
        ) 
        classNode.addConstructor(noArgConstructor)                        
    }

    
    /** */
    static addMapConstructor(ClassNode classNode, BlockStatement stmt) {
        Parameter mapParam = new Parameter(
            new ClassNode(Map), 
            "m", 
        )
        ConstructorNode mapConstructor = 
            new ConstructorNode(
                ClassNode.ACC_PRIVATE, 
                [mapParam] as Parameter[],
                [] as ClassNode[],
                stmt
        ) 
        classNode.addConstructor(mapConstructor) 
    }


    ///////////////////////////////////////////////////////////////////////////
    // INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    abstract Class getDefTraitClass()


    ///////////////////////////////////////////////////////////////////////////
    // TRANSFORM IMPLEMENTATION
    ///////////////////////////////////////////////////////////////////////////

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        // get the relevant nodes
        AnnotationNode annotNode = nodes[0]
        ClassNode classNode = (ClassNode) nodes[1]

        // statement to set global to true
        BlockStatement globalTrueStmt = macro(true) {
            this.global = true
        }

        // statement mimicing the default Groovy map constructor
        BlockStatement mapAssignmentStmt = macro(true) { 
            for (entry in m) { this."${entry.getKey()}" = entry.getValue() } 
        }

        // check if global by annotation
        boolean isGlobal = false
        Expression globalExp = annotNode.getMember("global")
        if (globalExp) isGlobal = Boolean.valueOf(globalExp.value)

        // add the def trait
        addTrait(classNode, getDefTraitClass())

        // add no argument constructor
        BlockStatement noArgConstStmt = new BlockStatement()
        if (isGlobal) noArgConstStmt.addStatement(globalTrueStmt)
        addNoArgConstructor(classNode, noArgConstStmt)

        // add map constructor
        BlockStatement mapConstStmt = new BlockStatement()
        mapConstStmt.addStatement(mapAssignmentStmt)
        if (isGlobal) mapConstStmt.addStatement(globalTrueStmt)
        addMapConstructor(classNode, mapConstStmt) 
    }

}



/** */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class VertexDefinitionTransformation extends DefinitionTransformation {

    Class getDefTraitClass() { return carnival.graph.VertexDefTrait }


    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        // do the superclass stuff
        super.visit(nodes, source)

        // get the relevant nodes
        AnnotationNode annotNode = nodes[0]
        ClassNode classNode = (ClassNode) nodes[1]

        // statement to add all defs in the enum
        BlockStatement propDefsAssignmentStmt = macro(true) {
            if (enumClass.isEnum()) {
                def vals = enumClass.values()
                for (int i=0; i<vals.size(); i++) {
                    this.propertyDefs.add(vals[i])
                }
            } 
        }

        // statement to call no-arg constructor
        BlockStatement noArgConstStmt = macro(true) {
            this()
        }

        // add all properties constructor
        BlockStatement constructorStmt = new BlockStatement()
        constructorStmt.addStatement(noArgConstStmt)
        constructorStmt.addStatement(propDefsAssignmentStmt)

        Parameter enumClassParam = new Parameter(
            new ClassNode(Class), 
            "enumClass", 
        )
        ConstructorNode constructor = 
            new ConstructorNode(
                ClassNode.ACC_PRIVATE, 
                [enumClassParam] as Parameter[],
                [] as ClassNode[],
                constructorStmt
        ) 
        classNode.addConstructor(constructor) 

    }

}


/** */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class EdgeDefinitionTransformation extends DefinitionTransformation {

    Class getDefTraitClass() { return carnival.graph.EdgeDefTrait }

}


/** */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class PropertyDefinitionTransformation extends DefinitionTransformation {

    Class getDefTraitClass() { return carnival.graph.PropertyDefTrait }

}


/** 
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class DefinitionTransformationPost extends AbstractASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        println "[DTP] nodes: $nodes"

        ClassNode classNode = (ClassNode) nodes[1]
        println "[DTP] classNode: $classNode"
        println "[DTP] classNode.fields: ${classNode.fields*.name}"
        println "[DTP] classNode.properties: ${classNode.properties}"

        AnnotationNode annotNode = nodes[0]
        println "[DTP] annotNode: $annotNode"
        println "[DTP] annotNode.members: ${annotNode.members}"
        println "[DTP] annotNode.classNode: ${annotNode.classNode}"
        println "[DTP] annotNode.classNode.fields: ${annotNode.classNode.fields}"
        println "[DTP] annotNode.classNode.properties: ${annotNode.classNode.properties}"

        Expression globalExp = annotNode.getMember("global")
        if (globalExp != null) {
            println "[DTP] globalExp: $globalExp"
            println "[DTP] globalExp.value: ${globalExp.value}"
            println "[DTP] globalExp.value (boolean): ${Boolean.valueOf(globalExp.value)}"

            FieldNode fieldGlobal = classNode.getField("carnival_graph_VertexDefTrait__global")
            println "[DTP] fieldGlobal: ${fieldGlobal}"
            println "[DTP] fieldGlobal.initialExpression: ${fieldGlobal?.initialExpression}"
            println "[DTP] fieldGlobal.initialValueExpression: ${fieldGlobal?.initialValueExpression}"
            println "[DTP] fieldGlobal.owner: ${fieldGlobal?.owner}"
            println "[DTP] fieldGlobal.instance: ${fieldGlobal?.instance}"
        }

    }

}
*/



/**
@GroovyASTTransformation(phase = CompilePhase.CLASS_GENERATION)
class GlobalElementDefinitionTransformation extends AbstractASTTransformation {

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        ClassNode classNode = (ClassNode) nodes[1]
        println "[GEDT] classNode: $classNode"

        ClassNode[] interfaces = classNode.getInterfaces()
        println "[GEDT] interfaces: $interfaces"

        ClassNode traitClassNode = new ClassNode(carnival.graph.VertexDefTrait)
        if (!interfaces.contains(traitClassNode)) return

        println "[GEDT] traitClassNode: ${traitClassNode}"
        println "[GEDT] traitClassNode.fields: ${traitClassNode.fields}"
        println "[GEDT] traitClassNode.properties: ${traitClassNode.properties}"
        println "[GEDT] traitClassNode.methods: ${traitClassNode.methods*.name}"

        FieldNode fieldGlobal = traitClassNode.getField('global')
        println "[GEDT] fieldGlobal: $fieldGlobal"
        println "[GEDT] fieldGlobal.initialValueExpression: ${fieldGlobal?.getInitialValueExpression()}"

    }

}
*/