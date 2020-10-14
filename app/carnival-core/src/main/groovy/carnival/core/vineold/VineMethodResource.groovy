package carnival.core.vineold



import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import java.lang.reflect.Field



/** */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface VineMethodResource {
    //public String value() default "";
}
