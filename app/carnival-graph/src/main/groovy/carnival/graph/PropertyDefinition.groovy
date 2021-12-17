package carnival.graph



import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import java.lang.reflect.Field
import org.codehaus.groovy.transform.GroovyASTTransformationClass


/**
 * Used to specify property definitions in a graph model. 
 * */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass(["carnival.graph.PropertyDefinitionTransformation"])
public @interface PropertyDefinition {
    //public String value() default "";
}
