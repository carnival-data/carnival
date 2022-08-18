package carnival.core.graph



import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import carnival.graph.VertexBuilder



/**
 * gradle test --tests "carnival.core.graph.CarnivalInitializationSpec"
 *
 */
class CarnivalNeo4jConfigurationSpec extends Specification {

    def "default config"() {
        when:
        def conf = CarnivalNeo4jConfiguration.defaultConfiguration()
        println "conf: ${conf}"

        then:
        conf != null
    }


    def "set all values"() {
        when:
        def conf = new CarnivalNeo4jConfiguration()

        conf.gremlin.neo4j.directory = 'cgnd'
        conf.gremlin.neo4j.conf.dbms.unmanaged_extension_classes = 'cgncdu'
        conf.gremlin.neo4j.conf.dbms.directories.plugins = 'plugins'
        conf.gremlin.neo4j.conf.dbms.security.auth_enabled = 'true'
        conf.gremlin.neo4j.conf.dbms.security.procedures.unrestricted = 'something*'
        conf.gremlin.neo4j.conf.dbms.security.procedures.whitelist = 'somethingElse*'

        then:
        conf.gremlin.neo4j.directory == 'cgnd'
        conf.gremlin.neo4j.conf.dbms.unmanaged_extension_classes == 'cgncdu'
        conf.gremlin.neo4j.conf.dbms.directories.plugins == 'plugins'
        conf.gremlin.neo4j.conf.dbms.security.auth_enabled == 'true'
        conf.gremlin.neo4j.conf.dbms.security.procedures.unrestricted == 'something*'
        conf.gremlin.neo4j.conf.dbms.security.procedures.whitelist == 'somethingElse*'
    }


}
