package carnival.core.vine


import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared

import carnival.core.util.FilesUtil



class VsVineWithConfig implements Vine {

    static VineConfiguration EXAMPLE_CONFIG = new VineConfiguration(
        cache: new VineConfiguration.Cache(
            mode: CacheMode.REQUIRED.name(),
            directory: '/path/to/directory',
            directoryCreateIfNotPresent: false
        )
    )

    VineConfiguration vineConfiguration = EXAMPLE_CONFIG

    @ToString(includeNames=true)
    static class Person { String name }

    class PersonVineMethod extends JsonVineMethod<Person> { 
        Person fetch(Map args) { new Person(name:args.p1) }
    }
}



class VineSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////

    def "override the default vine configuration"() {
        when:
        def vine = new VsVineWithConfig()

        then:
        vine.vineConfiguration == VsVineWithConfig.EXAMPLE_CONFIG
        vine.vineConfiguration.cache.mode == CacheMode.REQUIRED.name()
        vine.vineConfiguration.cache.directory == '/path/to/directory'
        vine.vineConfiguration.cache.directoryCreateIfNotPresent == false
    }

}