package carnival.clinical.graph.service



import groovy.sql.*
import groovy.mock.interceptor.StubFor
import groovy.util.AntBuilder

import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T

import org.apache.commons.io.FileUtils

import carnival.core.graph.CoreGraphSpecification
import carnival.graph.VertexDefTrait
import carnival.core.graph.Core
import carnival.core.graph.CoreGraphNeo4j



/**
 * gradle test --tests "carnival.core.graph.service.IdentifierServiceSpec"
 *
 *
 */
class IdentifierServiceSpec extends Specification {

    ///////////////////////////////////////////////////////////////////////////
    // GRAPH MODEL
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static enum VX implements VertexDefTrait {
        THING,
        SPECIAL_IDENTIFIER (
            vertexProperties:[
                Core.PX.VALUE.withConstraints(required:true, index:true)
            ]
        )

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    
    @Shared coreGraph
    @Shared graph
    @Shared g

    @Shared controlledInstances = [

        Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'existingIdClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ),

        Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'basicIdClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, false
        ),

        Core.VX.IDENTIFIER_CLASS.instance().withProperties(
            Core.PX.NAME, 'scopedIdClass',
            Core.PX.HAS_CREATION_FACILITY, false,
            Core.PX.HAS_SCOPE, true
        ),

        Core.VX.IDENTIFIER_SCOPE.instance().withProperty(Core.PX.NAME, 'scope1')

        //Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "1"),
        //Core.VX.IDENTIFIER.controlledInstance().withProperty(Core.PX.VALUE, "2"),
    ]

    @Shared svc
    @Shared existingIdClassV
    @Shared basicIdClassV
    @Shared scopedIdClassV
    @Shared scope1V


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////

    def setupSpec() {
        CoreGraphNeo4j.clearGraph()
        coreGraph = CoreGraphNeo4j.create(controlledInstances:controlledInstances)
        graph = coreGraph.graph

        svc = new IdentifierService.Services(appGraph:coreGraph)
    } 

    def setup() {
        g = graph.traversal()

        existingIdClassV = g.V()
            .hasLabel(Core.VX.IDENTIFIER_CLASS.label)
            .has(Core.PX.NAME.label, 'existingIdClass')
        .next()

        basicIdClassV = g.V()
            .hasLabel(Core.VX.IDENTIFIER_CLASS.label)
            .has(Core.PX.NAME.label, 'basicIdClass')
        .next()

        scopedIdClassV = g.V()
            .hasLabel(Core.VX.IDENTIFIER_CLASS.label)
            .has(Core.PX.NAME.label, 'scopedIdClass')
        .next()

        scope1V = g.V()
            .hasLabel(Core.VX.IDENTIFIER_SCOPE.label)
            .has(Core.PX.NAME.label, 'scope1')
        .next()
    }

    def cleanup() {
        if (g) g.close()
        if (coreGraph) coreGraph.graph.tx().rollback()
    }

    def cleanupSpec() {
        if (coreGraph) coreGraph.graph.close()
    }


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////


    def "single code"() {
        def res
        Throwable e

        when:
        def s1 = VX.THING.createVertex(graph, g)

        def id1V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1V, existingIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id1V)

        res = IdentifierService.autoGenerateScopedIdentifiers(
            services:svc,
            existingIdentifierClassV:existingIdClassV,
            subjectDef:VX.THING,
            newIdentifierClassV:scopedIdClassV,
            newIdentifierScopeV:scope1V,
            newIdentifierPrefix:'pre'
        )
        println "s1:$s1"
        println "scopedIdClassV:$scopedIdClassV"

        then:
        g.V(s1)
            .out(Core.EX.IS_IDENTIFIED_BY.label)
            .hasLabel(Core.VX.IDENTIFIER.label).as('id')
            .out(Core.EX.IS_INSTANCE_OF.label)
            .is(scopedIdClassV)
            .select('id')
            .out(Core.EX.IS_SCOPED_BY.label)
            .is(scope1V)
        .tryNext().isPresent()

        when:
        def newIdVs = g.V(s1)
            .out(Core.EX.IS_IDENTIFIED_BY.label)
            .hasLabel(Core.VX.IDENTIFIER.label).as('id')
            .out(Core.EX.IS_INSTANCE_OF.label)
            .is(scopedIdClassV)
            .select('id')
            .out(Core.EX.IS_SCOPED_BY.label)
            .is(scope1V)
            .select('id')
        .toList()

        then:
        newIdVs.size() == 1
        newIdVs[0].value(Core.PX.VALUE.label) == 'pre1'

        when:
        def file = res.file

        then:
        file != null

/*
LOOKUP_IDENTIFIER_TYPE  LOOKUP_IDENTIFIER_VALUE NEW_IDENTIFIER_SCOPE    NEW_IDENTIFIER_TYPE NEW_IDENTIFIER_VALUE    SUBJECT_TYPE
existingIdClass id1 scope1  scopedIdClass   pre1    Thing
*/
        when:
        CsvIterator csvIterator = parseCsv(file.text)

        then:
        csvIterator.hasNext()

        when:
        def fileRows = csvIterator.toList()

        then:
        fileRows.size() == 1

        when:
        def rec = fileRows[0].toMap()

        then:
        rec.get('LOOKUP_IDENTIFIER_TYPE') == 'existingIdClass'
        rec.get('LOOKUP_IDENTIFIER_VALUE') == 'id1'
        rec.get('NEW_IDENTIFIER_SCOPE') == 'scope1'
        rec.get('NEW_IDENTIFIER_TYPE') == 'scopedIdClass'
        rec.get('NEW_IDENTIFIER_VALUE') == 'pre1'
        rec.get('SUBJECT_TYPE') == 'Thing'
    }



    def "no new ids pre-exist"() {
        def res
        Throwable e

        when:
        def s1 = VX.THING.createVertex(graph, g)

        def id1V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1V, existingIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id1V)

        def id2V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id2').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id2V, scopedIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id2V)
        Core.EX.IS_SCOPED_BY.relate(g, id2V, scope1V)

        res = IdentifierService.autoGenerateScopedIdentifiers(
            services:svc,
            existingIdentifierClassV:existingIdClassV,
            subjectDef:VX.THING,
            newIdentifierClassV:scopedIdClassV,
            newIdentifierScopeV:scope1V,
            newIdentifierPrefix:'pre'
        )

        then:
        e = thrown()
        e.printStackTrace()
    }


    def "existing ids identify only one subject"() {
        def res
        Throwable e

        when:
        def s1 = VX.THING.createVertex(graph, g)

        def id1V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1V, existingIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id1V)

        def s2 = VX.THING.createVertex(graph, g)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s2, id1V)

        res = IdentifierService.autoGenerateScopedIdentifiers(
            services:svc,
            existingIdentifierClassV:existingIdClassV,
            subjectDef:VX.THING,
            newIdentifierClassV:scopedIdClassV,
            newIdentifierScopeV:scope1V,
            newIdentifierPrefix:'pre'
        )

        then:
        e = thrown()
        e.printStackTrace()
    }


    def "existing ids only one per subject"() {
        def res
        Throwable e

        when:
        def s1 = VX.THING.createVertex(graph, g)
        def id1V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1V, existingIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id1V)

        def id2V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id2').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id2V, existingIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id2V)

        res = IdentifierService.autoGenerateScopedIdentifiers(
            services:svc,
            existingIdentifierClassV:existingIdClassV,
            subjectDef:VX.THING,
            newIdentifierClassV:scopedIdClassV,
            newIdentifierScopeV:scope1V,
            newIdentifierPrefix:'pre'
        )

        then:
        e = thrown()
        e.printStackTrace()
    }


    def "subjects must exist"() {
        def res
        Throwable e

        when:
        res = IdentifierService.autoGenerateScopedIdentifiers(
            services:svc,
            existingIdentifierClassV:existingIdClassV,
            subjectDef:VX.THING,
            newIdentifierClassV:scopedIdClassV,
            newIdentifierScopeV:scope1V,
            newIdentifierPrefix:'pre'
        )

        then:
        e = thrown()
        e.printStackTrace()
    }



    def "new id class is scoped"() {
        def res
        Throwable e

        when:
        def s1 = VX.THING.createVertex(graph, g)
        def id1V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1V, existingIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id1V)

        res = IdentifierService.autoGenerateScopedIdentifiers(
            services:svc,
            existingIdentifierClassV:existingIdClassV,
            subjectDef:VX.THING,
            newIdentifierClassV:basicIdClassV,
            newIdentifierScopeV:scope1V,
            newIdentifierPrefix:'pre'
        )

        then:
        e = thrown()
        e.printStackTrace()
    }



    def "existing and new id classes are different"() {
        def res
        Throwable e

        when:
        def s1 = VX.THING.createVertex(graph, g)
        def id1V = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, 'id1').vertex(graph, g)
        Core.EX.IS_INSTANCE_OF.relate(g, id1V, existingIdClassV)
        Core.EX.IS_IDENTIFIED_BY.relate(g, s1, id1V)

        res = IdentifierService.autoGenerateScopedIdentifiers(
            services:svc,
            existingIdentifierClassV:existingIdClassV,
            subjectDef:VX.THING,
            newIdentifierClassV:existingIdClassV,
            newIdentifierScopeV:scope1V,
            newIdentifierPrefix:'pre'
        )

        then:
        e = thrown()
        e.printStackTrace()
    }

    

}



