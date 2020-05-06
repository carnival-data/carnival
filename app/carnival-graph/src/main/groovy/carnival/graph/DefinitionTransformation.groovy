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
import org.codehaus.groovy.ast.MixinNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.ast.expr.*



/** */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class VertexDefinitionTransformation extends DefinitionTransformation {

    Class getDefTraitClass() { return carnival.graph.VertexDefTrait }

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

/** */
abstract class DefinitionTransformation extends AbstractASTTransformation {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static void addTrait(ClassNode classNode, Class traitClass) {
        ClassNode[] interfaces = classNode.getInterfaces()
        //println "interfaces: $interfaces"
        ClassNode traitClassNode = new ClassNode(traitClass)
        if (interfaces.contains(traitClassNode)) return
        //println "traitClassNode: $traitClassNode"
        List<ClassNode> finalInterfaces = new ArrayList<ClassNode>()
        finalInterfaces.add(traitClassNode)
        finalInterfaces.addAll(interfaces)
        //println "finalInterfaces: $finalInterfaces"
        ClassNode[] finalInterfacesArray = finalInterfaces.toArray(Class[])
        //println "finalInterfacesArray: $finalInterfacesArray"
        classNode.setInterfaces(finalInterfacesArray)
    }

    /** */
    static addNoArgConstructor(ClassNode classNode) {
        ConstructorNode noArgConstructor = 
            new ConstructorNode(
                ClassNode.ACC_PRIVATE, 
                [] as Parameter[],
                [] as ClassNode[],
                new EmptyStatement()
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
        ClassNode classNode = (ClassNode) nodes[1]

        // add the def trait
        addTrait(classNode, getDefTraitClass())

        // add no argument constructor
        addNoArgConstructor(classNode)

        // add map constructor
        BlockStatement simplestCode = macro(true) { for (entry in m) { this."${entry.getKey()}" = entry.getValue() } }
        addMapConstructor(classNode, simplestCode) 
    }

}
