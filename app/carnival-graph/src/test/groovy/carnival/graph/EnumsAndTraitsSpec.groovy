package carnival.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared




trait TraitZero {
    String field1
    String field2

    public withField2(String val) {
        TraitZero newObj = new TraitZeroHolder(this)
        newObj.field2 = val
        newObj
    }
}


class TraitZeroHolder implements TraitZero {
    TraitZero source

    public TraitZeroHolder(TraitZero source) {
        this.source = source
    }

    def methodMissing(String name, def args) {
        source.invokeMethod(name, args)
    }
}



class EnumsAndTraitsSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // BUILDING BLOCKS
    ///////////////////////////////////////////////////////////////////////////

    static enum PX implements TraitZero {
        PROP_A(field1:'v1'),
        PROP_B(field1:'v2')

        PX() {}
        PX(Map m) {m.each { k,v -> this."$k" = v }}
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////

    def "holder delegates missing methods to source two layers down"() {
        when:
        def pa = PX.PROP_A.withField2('f2v1').withField2('f2v2')
        def name = pa.name()

        then:
        noExceptionThrown()
        name == 'PROP_A'
        pa.field2 == 'f2v2'
    }


    def "holder delegates missing methods to source"() {
        when:
        def pa = PX.PROP_A.withField2('f2v1')
        def name = pa.name()

        then:
        noExceptionThrown()
        name == 'PROP_A'
        pa.field2 == 'f2v1'
    }


    def "holder maintains base trait"() {
        when:
        def pa = PX.PROP_A.withField2('f2v1')

        then:
        pa != null
        pa instanceof TraitZero
        pa.source == PX.PROP_A
    }


    def "cannot clone an enum"() {
        when:
        def pa = PX.PROP_A

        then:
        pa != null
        pa.field1 == 'v1'
    }


}

