package carnival.util



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared



public class CoreUtilSpec extends Specification {


    void "arguments uniquifier recurse into maps"() {
        when:
        def u1 = CoreUtil.argumentsUniquifier(a:[b:1])
        def u2 = CoreUtil.argumentsUniquifier(a:[b:'1'])

        then:
        u1 != null
        u2 != null
        u1 != u2
    }


    void "arguments uniquifier recurse into lists"() {
        when:
        def u1 = CoreUtil.argumentsUniquifier(a:[1])
        def u2 = CoreUtil.argumentsUniquifier(a:['1'])

        then:
        u1 != null
        u2 != null
        u1 != u2
    }


    void "arguments uniquifier string boolean"() {
        when:
        def u1 = CoreUtil.argumentsUniquifier(a:'true')
        def u2 = CoreUtil.argumentsUniquifier(a:true)

        then:
        u1 != null
        u2 != null
        u1 != u2
    }


    void "arguments uniquifier string integer"() {
        when:
        def u1 = CoreUtil.argumentsUniquifier(a:1)
        def u2 = CoreUtil.argumentsUniquifier(a:'1')

        then:
        u1 != null
        u2 != null
        u1 != u2
    }


    void "arguments uniquifier"() {
        when:
        def u1 = CoreUtil.argumentsUniquifier(a:1)
        def u2 = CoreUtil.argumentsUniquifier(a:2)

        then:
        u1 != null
        u2 != null
        u1 != u2
    }


}