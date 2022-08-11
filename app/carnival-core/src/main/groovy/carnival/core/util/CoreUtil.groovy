package carnival.core.util



import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils
import carnival.util.StringUtils



@Slf4j
class CoreUtil {


    /** */
    static public String argumentsUniquifier(Map args) {
        assert args != null

        String str = typedStringMap(args)
        standardizedUniquifier(str)
    }


    /** */
    static String nullSafeTypedString(Object v) {
        if (v == null) return String.valueOf(v)
        else return typedString(v)
    }


    /** */
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


    /** */
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


    /** */
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


    /** */
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

        //def name = cn.reverse()
        //if (name.contains('$')) name = name.substring(0, name.indexOf('$')) //name.takeBefore('$')
        //else if (name.contains('.')) name = name.substring(0, name.indexOf('.')) // name.takeBefore('.')
        //name = name.reverse()
        //name = StringUtils.toKebabCase(name)

        def name = cn.replaceAll('\\.', '-').replaceAll('\\$', '-')
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