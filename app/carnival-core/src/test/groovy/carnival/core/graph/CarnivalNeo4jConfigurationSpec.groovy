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


    def "set all values"() {
        given:
        def graph = TinkerGraph.open()
        def graphSchema = new DefaultGraphSchema()
        def graphValidator = new DefaultGraphValidator()
        def vertexBuilders = new HashSet<VertexBuilder>()

        when:
        def conf = new CarnivalNeo4jConfiguration()

        conf.graph = graph
        conf.graphSchema = graphSchema
        conf.graphValidator = graphValidator
        conf.vertexBuilders = vertexBuilders

        conf.gremlin.neo4j.directory = 'cgnd'
        conf.gremlin.neo4j.conf.dbms.unmanaged_extension_classes = 'cgncdu'
        conf.gremlin.neo4j.conf.dbms.directories.plugins = 'plugins'
        conf.gremlin.neo4j.conf.dbms.security.auth_enabled = 'true'
        conf.gremlin.neo4j.conf.dbms.security.procedures.unrestricted = 'something*'
        conf.gremlin.neo4j.conf.dbms.security.procedures.whitelist = 'somethingElse*'

        then:
        conf.graph == graph
        conf.graphSchema == graphSchema
        conf.graphValidator == graphValidator
        conf.vertexBuilders == vertexBuilders

        conf.gremlin.neo4j.directory == 'cgnd'
        conf.gremlin.neo4j.conf.dbms.unmanaged_extension_classes == 'cgncdu'
        conf.gremlin.neo4j.conf.dbms.directories.plugins == 'plugins'
        conf.gremlin.neo4j.conf.dbms.security.auth_enabled == 'true'
        conf.gremlin.neo4j.conf.dbms.security.procedures.unrestricted == 'something*'
        conf.gremlin.neo4j.conf.dbms.security.procedures.whitelist == 'somethingElse*'

        cleanup:
        if (conf.graph) conf.graph.close()
    }


}
