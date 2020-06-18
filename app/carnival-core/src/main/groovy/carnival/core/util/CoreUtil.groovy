package carnival.core.util



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils
import carnival.util.StringUtils



@Slf4j
class CoreUtil {

    /** */
    static public String standardizedUniquifier(String seed) {
        assert seed != null
        assert seed.trim().length() > 0

        DigestUtils.md5(seed.bytes).encodeHex().toString()
    }


    /** */
    static public String standardizedFileName(Object obj) {
        assert obj != null
        standardizedFileName(obj.class)
    }


    /** */
    static public String standardizedFileName(Class cl) {
        String cn = cl.name
        //println "cn: $cn"

        def name = cn.reverse()
        if (name.contains('$')) name = name.substring(0, name.indexOf('$')) //name.takeBefore('$')
        else if (name.contains('.')) name = name.substring(0, name.indexOf('.')) // name.takeBefore('.')
        name = name.reverse()
        name = StringUtils.toKebabCase(name)
        //println "name: $name"

        return name
    }


    /** */
    public static Set<Class> allClasses(Object obj) {
        Set<Class> classes = new HashSet<Class>()
        Class cl = obj.class
        classes << cl
        while (cl != null) {
            cl = cl.superclass
            if (cl != null) classes << cl
        }
        return classes
    }    

}