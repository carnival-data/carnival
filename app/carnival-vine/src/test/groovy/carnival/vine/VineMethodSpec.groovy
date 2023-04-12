package carnival.vine


import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import groovy.transform.ToString
import spock.lang.Specification
import spock.lang.Shared



class SomeVineMethod extends VineMethod { 
    static VineConfiguration EXAMPLE_CONFIG = new VineConfiguration(
        cache: new VineConfiguration.Cache(
            mode: CacheMode.REQUIRED,
            directory: Paths.get('/path/to/directory'),
            directoryCreateIfNotPresent: false
        )
    )

    VineConfiguration vineConfiguration = EXAMPLE_CONFIG

    Object fetch(Map args) { new Integer(1) }

    VineMethodCall call() { }
    VineMethodCall call(Map args) { }
    VineMethodCall call(CacheMode cacheMode) { }
    VineMethodCall call(CacheMode cacheMode, Map args) { }
}



class VineMethodSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // tests
    ///////////////////////////////////////////////////////////////////////////

    def "override the default vine configuration"() {
        when:
        def vm = new SomeVineMethod()

        then:
        vm.vineConfiguration == SomeVineMethod.EXAMPLE_CONFIG
        vm.vineConfiguration.cache.mode == CacheMode.REQUIRED
        vm.vineConfiguration.cache.directory == Paths.get('/path/to/directory')
        vm.vineConfiguration.cache.directoryCreateIfNotPresent == false
    }

}