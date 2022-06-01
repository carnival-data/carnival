package carnival.graph



import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import java.lang.reflect.Field
import org.codehaus.groovy.transform.GroovyASTTransformationClass


/** 
 * Used to specify vertex definitions in a graph model.
 * 
 * @see carnival.graph.VertexDefinition
 * @see carnival.graph.ModelTransformation
 * */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass(["carnival.graph.VertexModelTransformation"])
public @interface VertexModel {
    public String global() default "";
}

