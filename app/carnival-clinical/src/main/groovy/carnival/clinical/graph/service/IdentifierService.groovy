package carnival.clinical.graph.service



import java.text.SimpleDateFormat

import groovy.transform.ToString

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.step.*
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import static org.apache.tinkerpop.gremlin.process.traversal.step.TraversalOptionParent.Pick.*
import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of

import carnival.core.graph.CoreGraph
import carnival.core.graph.CarnivalTraversalSource
import carnival.util.DataTable
import carnival.util.GenericDataTable
import carnival.util.TabularReport
import carnival.util.FeatureReport
import carnival.util.FeatureSetDescriptor
import carnival.util.FeatureDataType
import carnival.util.Log
import carnival.core.graph.service.ReportService
import carnival.core.graph.Core
import carnival.util.Defaults
import carnival.clinical.graph.Clinical



/**
 *
 *
 */
class IdentifierService extends ReportService {

    ///////////////////////////////////////////////////////////////////////////
    // LOGGING
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static final Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** */
    static final Logger log = LoggerFactory.getLogger(IdentifierService)



    ///////////////////////////////////////////////////////////////////////////
    // STATIC CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    @ToString(includeNames=true)
    static class Services {
        CoreGraph appGraph
        boolean isValid() {
            (appGraph)
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS
    ///////////////////////////////////////////////////////////////////////////


    /** 
     *
     *
     */
    static public Map autoGenerateScopedIdentifiers(Map args = [:]) {

        // services
        assert args.services
        assert args.services instanceof Services
        assert args.services.isValid()

        // graph traversal 
        def graph = args.services.appGraph.graph
        def g = graph.traversal()

        // args
        assert args.existingIdentifierClassV
        assert args.subjectDef
        assert args.newIdentifierClassV
        assert args.newIdentifierScopeV
        assert args.newIdentifierPrefix
        def existingIdentifierClassV = args.existingIdentifierClassV
        def subjectDef = args.subjectDef
        def newIdentifierClassV = args.newIdentifierClassV
        def newIdentifierScopeV = args.newIdentifierScopeV
        def newIdentifierPrefix = args.newIdentifierPrefix

        // check that input vertices have the right labels
        assert existingIdentifierClassV.label() == Core.VX.IDENTIFIER_CLASS.label
        assert newIdentifierClassV.label() == Core.VX.IDENTIFIER_CLASS.label

        // check that existingIdentifierClassV != newIdentifierClassV
        assert existingIdentifierClassV != newIdentifierClassV : "existing and new identifier classes must be different: ${existingIdentifierClassV} == ${newIdentifierClassV}"

        // check that newIdentifierClassV hasScope:true
        assert Core.PX.HAS_SCOPE.valueOf(newIdentifierClassV) == true : "new identifier class must be scoped: ${newIdentifierClassV} ${newIdentifierClassV.value(Core.PX.HAS_SCOPE.label)}"


        // check that there are subjects identified by identifiers
        // that are instances of existingIdentifierClassV
        def subjectVs = g.V()
            .hasLabel(subjectDef.label).as('subjects')
            .out(Core.EX.IS_IDENTIFIED_BY.label)
            .hasLabel(Core.VX.IDENTIFIER.label)
            .out(Core.EX.IS_INSTANCE_OF.label)
            .is(existingIdentifierClassV)
            .select('subjects')
        .toList()
        assert subjectVs.size() > 0


        // verify that each identifier of class existingIdentifierClassV
        // identifies at most one subject
        def subjectsById = g.V()
            .hasLabel(Core.VX.IDENTIFIER.label).as('id')
            .out(Core.EX.IS_INSTANCE_OF.label)
            .is(existingIdentifierClassV)
            .select('id')
            .group().by(
                __.in(Core.EX.IS_IDENTIFIED_BY.label)
                .hasLabel(subjectDef.label)
            )
        .next()

        subjectsById.each { k, v -> log.trace "subjectsById: $k $v" }
        def multiIds = subjectsById.findAll { k, v -> v.size() > 1 }
        assert multiIds.size() == 0 : "the following subjects are identifid by >1 identifier of the existing identifier class: ${existingIdentifierClassV} ${multiIds}"


        // verify that each subject identified by existingIdentifierClassV
        // is identified by at most one identifier of that class
        def idsBySubject = g.V()
            .hasLabel(subjectDef.label)
            .group().by(
                __.out(Core.EX.IS_IDENTIFIED_BY.label).as('id')
                .out(Core.EX.IS_INSTANCE_OF.label)
                .is(existingIdentifierClassV)
                .select('id')
            )
        .next()
        idsBySubject.each { k, v -> log.trace "idsBySubject: $k $v" }
        def multiSubjects = idsBySubject.findAll { k, v -> v.size() > 1 }
        assert multiSubjects.size() == 0 : "the following identifiers of the existing identifier class identify >1 subjects: ${existingIdentifierClassV} ${multiSubjects}"


        // verify that no subjects are identified by an indentifier of class
        // newIdentifierClassV where the identifier is scoped by 
        // newIdentifierScopeV
        def conflictNewIdVs = g.V()
            .hasLabel(subjectDef.label).as('sub')
            .out(Core.EX.IS_IDENTIFIED_BY.label)
            .hasLabel(Core.VX.IDENTIFIER.label).as('id')
            .out(Core.EX.IS_INSTANCE_OF.label)
            .is(newIdentifierClassV)
            .select('id')
            .out(Core.EX.IS_SCOPED_BY.label)
            .is(newIdentifierScopeV)
            .select('sub')
        .toSet()
        assert conflictNewIdVs.size() == 0 : "the following subjects are identified by identifiers of class ${existingIdentifierClassV} scoped by ${newIdentifierScopeV}: ${conflictNewIdVs}"


        // create a list of new identifier values
        Map<Vertex,String> idVals = new HashMap<Vertex,String>()
        int numSubjects = subjectVs.size()
        int pad = "$numSubjects".length()
        subjectVs.eachWithIndex { sV, idx ->
            def paddedNum = "${idx+1}".padLeft(pad, '0')
            idVals.put(sV, "${newIdentifierPrefix}${paddedNum}".toString())
        }
        log.trace "idVals: $idVals"


        // create and associate new identifiers
        List<Vertex> newIdVs = new ArrayList<Vertex>()
        idVals.each { sV, idVal ->
            def newIdV = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, idVal).createVertex(graph)
            newIdVs << newIdV
            sV.addEdge(Core.EX.IS_IDENTIFIED_BY.label, newIdV)
            newIdV.addEdge(Core.EX.IS_SCOPED_BY.label, newIdentifierScopeV)
            newIdV.addEdge(Core.EX.IS_INSTANCE_OF.label, newIdentifierClassV)
        }


        // write identifier file to target directory
        TabularReport report = new TabularReport(name:'generated-identifiers')
        report.start()

        final String subjectType = subjectDef.label
        final String lookupIdentifierType = Core.PX.NAME.valueOf(existingIdentifierClassV)
        final String newIdentifierType = Core.PX.NAME.valueOf(newIdentifierClassV)
        def scopeKey
        if (newIdentifierScopeV.label() == Clinical.VX.STUDY.label) {
            scopeKey = 'NEW_IDENTIFIER_STUDY'
        } else if (newIdentifierScopeV.label() == Core.VX.IDENTIFIER_SCOPE.label) {
            scopeKey = 'NEW_IDENTIFIER_SCOPE'
        } else {
            throw new IllegalArgumentException("unrecognized scope class. must be one of [${Clinical.VX.STUDY}, ${Core.VX.IDENTIFIER_SCOPE}]")
        }
        assert scopeKey

        idVals.each { sV, idVal ->
            def lookupIdV = subjectsById.get(sV)
            assert lookupIdV
            def rec = [
                SUBJECT_TYPE:subjectType,
                LOOKUP_IDENTIFIER_TYPE:lookupIdentifierType,
                LOOKUP_IDENTIFIER_VALUE:Core.PX.VALUE.valueOf(lookupIdV),
                NEW_IDENTIFIER_TYPE:newIdentifierType,
                NEW_IDENTIFIER_VALUE:idVal
            ]
            rec.put(scopeKey, Core.PX.NAME.valueOf(newIdentifierScopeV))
            report.dataAdd(rec)
        }

        // close and return
        if (g) g.close()
        return [
            newIdVs:newIdVs,
            report:report
        ]
    }



}