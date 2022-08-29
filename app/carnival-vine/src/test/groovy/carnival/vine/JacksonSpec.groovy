package carnival.vine



import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeInfo.As



class JacksonSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // classes
    ///////////////////////////////////////////////////////////////////////////

    @ToString(includeNames=true)
    static class Person { String name }

    static class PersonVineMethod extends JsonVineMethod<Person> { 
        Person fetch(Map args) { new Person(name:args.p1) }
    }

    static class PersonsVineMethod extends JsonVineMethod<List<Person>> { 
        List<Person> fetch(Map args) { 
            [
                new Person(name:'alice'),
                new Person(name:'bob') 
            ]
        }
    }

    static class PersonHolderNoJsonType {
        Person person
    }

    static class PersonHolder {
        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        Person person
    }

    static class PersonHolderVineMethod extends JsonVineMethod<PersonHolder> { 
        PersonHolder fetch(Map args) { 
            new PersonHolder(person: new Person(name:args.p1)) 
        }
    }

    //@JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    static class PersonsHolder {
        
        List<Person> persons
    }

    static class PersonsHolderVineMethod extends JsonVineMethod<PersonsHolder> { 
        PersonsHolder fetch(Map args) { 
            new PersonsHolder(
                persons: [
                    new Person(name:'alice'), 
                    new Person(name:'bob')
                ]
            ) 
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////




    def "jackson deserialization"() {
        when:
        Person p = new Person(name:"hi")
        PersonHolder ph = new PersonHolder(person:p)
        ObjectMapper mapper = new ObjectMapper()
        def writer = mapper.writerWithDefaultPrettyPrinter()
        String out = writer.writeValueAsString(ph)
        println "out: $out"

        then:
        out != null
    }


    def "jackson map render with annotation"() {
        when:
        Map args = [p1:"hello", l1:[1,2,3], x1:58]
        ObjectMapper mapper = new ObjectMapper()
        def writer = mapper.writerWithDefaultPrettyPrinter()
        String out = writer.writeValueAsString(args)
        println "out: $out"

        then:
        out != null

        when:
        String js = '''{
  "person" : [ "carnival.vine.JacksonSpec$Person", {
    "name" : "hi"
  } ]
}'''        
        PersonHolder ph2 = mapper.readValue(js, PersonHolder.class)
        println "ph2: $ph2"

        then:
        ph2 instanceof PersonHolder
        ph2.person != null
        ph2.person instanceof Person
    }


    def "jackson map render"() {
        when:
        Map args = [p1:"hello", l1:[1,2,3], x1:58]
        ObjectMapper mapper = new ObjectMapper()
        def writer = mapper.writerWithDefaultPrettyPrinter()
        String out = writer.writeValueAsString(args)
        println "out: $out"

        then:
        out != null

        when:
        String js = '''{
  "person" : {
    "name" : "hi"
  }
}'''        
        PersonHolderNoJsonType ph2 = mapper.readValue(js, PersonHolderNoJsonType.class)
        println "ph2: $ph2"

        then:
        ph2 instanceof PersonHolderNoJsonType
        ph2.person != null
        ph2.person instanceof Person
    }

}
