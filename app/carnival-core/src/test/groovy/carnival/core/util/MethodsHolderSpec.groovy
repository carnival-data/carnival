package carnival.core.util



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared



interface MhsSomeInterface { }


class MhsParentHolder implements MethodsHolder {

    class Method1 implements MhsSomeInterface { }

}


class MhsChildHolder extends MhsParentHolder {

    class Method2 implements MhsSomeInterface { }

}



public class MethodsHolderSpec extends Specification {



    
    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    void "allMethodClasses finds methods up inheritance chain"() {
        when:
        def mh1 = new MhsParentHolder()
        def mh1classes = mh1.allMethodClasses(MhsSomeInterface)

        then:
        mh1classes.size() == 1
        mh1classes[0] == MhsParentHolder.Method1

        when:
        def mh2 = new MhsChildHolder()
        def mh2classes = mh2.allMethodClasses(MhsSomeInterface)

        then:
        mh2classes.size() == 2
        mh2classes.contains(MhsParentHolder.Method1)
        mh2classes.contains(MhsChildHolder.Method2)
    }
    

}