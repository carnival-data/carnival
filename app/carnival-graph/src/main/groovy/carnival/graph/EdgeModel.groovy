package carnival.graph



import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import java.lang.reflect.Field
import org.codehaus.groovy.transform.GroovyASTTransformationClass


/** 
 * Used to specify edge definitions in a graph model.
 * 
 * @see carnival.graph.EdgeDefinition
 * @see carnival.graph.ModelTransformation
 * */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass(["carnival.graph.EdgeModelTransformation"])
public @interface EdgeModel {
    //public String value() default "";
}
