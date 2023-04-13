package carnival.util



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils
import carnival.util.StringUtils



/**
 * Static utility methods
 */
@Slf4j
class CoreUtil {


    /** 
     * Return a unique fingerprint computed from the provided map of args.
     * @param args Map of arguments
     * @return A unique fingerprint as a string
     */
    static public String argumentsUniquifier(Map args) {
        assert args != null

        String str = typedStringMap(args)
        standardizedUniquifier(str)
    }


    /** 
     * Return a string representation of the provided object tagged with the
     * type of the object; Null provided objects are accepted.
     * @param v The source object
     * @return The string representation
     */
    static String nullSafeTypedString(Object v) {
        if (v == null) return String.valueOf(v)
        else return typedString(v)
    }


    /** 
     * Return a string representation of the provided object tagged with the
     * type of the object.
     * @param v The source object
     * @return The string representation
     */
    static String typedString(Object v) {
        assert v != null

        if (v instanceof List) return typedStringList(v)
        if (v instanceof Map) return typedStringMap(v)

        String tv = typeTag(v)

        StringBuffer buf = new StringBuffer()
        buf.append(tv)
        buf.append(v)

        return buf.toString()
    }


    /** 
     * Return a string representation of the provided object map.
     * @param v The source map
     * @return The string representation
     */
    static String typedStringMap(Map map) {
        assert map != null

        StringBuffer buf = new StringBuffer()
        buf.append('[')
        
        map.each { k, v ->
            buf.append(typedString(k))
            buf.append(':')
            buf.append(nullSafeTypedString(v))
        }

        buf.append(']')
    }


    /** 
     * Return a string representation of the provided object list.
     * @param v The source list
     * @return The string representation
     */
    static String typedStringList(List list) {
        assert list != null

        StringBuffer buf = new StringBuffer()
        buf.append('[')
        list.each { v ->
            buf.append(',')
            buf.append(nullSafeTypedString(v))
        }
        buf.append(']')

        buf.toString()
    }


    /**
     * The type tag for the provided object.
     * @param v The source object
     * @param The type tag as a string 
     */
    static String typeTag(Object v) {
        assert v != null

        String tv
        if (v instanceof String) tv = 's'
        else if (v instanceof Integer) tv = 'i'
        else if (v instanceof Long) tv = 'l'
        else if (v instanceof Double) tv = 'd'
        else if (v instanceof Float) tv = 'f'
        else if (v instanceof Boolean) tv = 'b'
        else tv = 'u'

        return tv
    }


    /** 
     * Return a standard unique fingerprint for the provided string.
     * @param seed The source string
     * @return The unique fingerprint as a string
     */
    static public String standardizedUniquifier(String seed) {
        assert seed != null
        assert seed.trim().length() > 0

        DigestUtils.md5(seed.bytes).encodeHex().toString()
    }


    /** 
     * Return a standard computed filename for the provided object.
     * @param obj The source object
     * @return The standard filename as a string
     */
    static public String standardizedFileName(Object obj) {
        assert obj != null
        standardizedFileName(obj.class)
    }


    /** 
     * Return a standard computed filename for the provided class.
     * @param cl The source class
     * @return The filename as a string
     */
    static public String standardizedFileName(Class cl) {
        String cn = cl.name
        //println "cn: $cn"

        //def name = cn.reverse()
        //if (name.contains('$')) name = name.substring(0, name.indexOf('$')) //name.takeBefore('$')
        //else if (name.contains('.')) name = name.substring(0, name.indexOf('.')) // name.takeBefore('.')
        //name = name.reverse()
        //name = StringUtils.toKebabCase(name)

        def name = cn.replaceAll('\\.', '-').replaceAll('\\$', '-')
        //println "name: $name"

        return name
    }


    /** 
     * Return all the classes that apply to the provided object.
     * @param obj The source object
     * @return A set of class objects
     */
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