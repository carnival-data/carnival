package carnival.clinical.util



import groovy.util.AntBuilder

import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph

import carnival.clinical.graph.Clinical
import carnival.core.graph.Core
import carnival.util.DataTable
import carnival.util.DataTable.MetaData



/**
 * Collection of static utility methods.
 *
 */
class TestUtils {



    /**
     * Copy source test data files to a cache directory for testing.
     *
     */
    static Map setUpCacheForVineMethod(Map args) {
        assert args.g
        assert args.tag
        assert args.vine
        assert args.methodName
        //assert args.methodArgs
        assert args.sourceDataFileName

        // set up test data cache directory
        def tagd = args.tag.replaceAll(' ', '-').trim()
        def ant = new AntBuilder()
        def testdir = new File("build/test/${tagd}")
        if (!testdir.exists()) ant.mkdir(dir:testdir)

        // set cacheDirectory of vine
        args.vine.cacheDirectory = testdir

        // create an empty data table
        def vm = args.vine.createVineMethodInstance(args.methodName)
        def methodArgs = args.methodArgs ?: [:]
        def dt = vm.createEmptyDataTable(methodArgs)

        // write the meta file to the test directory
        dt.writeMetaFile(testdir)

        // copy the data file to the test directory
        DataTable.MetaData meta = vm.meta(methodArgs)
        ant.copy( 
            file:"data/test/${tagd}/${args.sourceDataFileName}.csv", 
            tofile:"${testdir.canonicalPath}/${meta.name}.csv"
        )        

        // return a result
        return [:]
    }




    /**
     * Create patients for testing as per the various args.
     * Note that ids are converted to Strings
     *
     * Example:
     *     def patVs = TestUtils.createPatients(
     *            graph:graph, 
     *            g:g, 
     *            idClass:"generic_patient_id", 
     *            idFacilityName:idFacility, 
     *            ids:ids)
     *
     */
    static Collection<Vertex> createPatients(Map args) {
        assert args.idClass
        assert args.ids
    	assert args.graph
        assert args.g

        def idc = args.idClass
        def ids = args.ids
        def graph = args.graph
        def g = args.g
        def data = args.data

        if (ids instanceof Range) ids = ids.toList()
        ids = ids.collect({ String.valueOf(it) })

        def idClassV = Core.VX.IDENTIFIER_CLASS.instance()
            .withProperties(
                Core.PX.NAME, idc,
                Core.PX.HAS_SCOPE, args.containsKey('idScopeName'),
                Core.PX.HAS_CREATION_FACILITY, args.containsKey('idFacilityName')
            )
        .vertex(graph, g)

        def patIdScopeV = args.idScopeName ? Core.VX.IDENTIFIER_SCOPE.instance().withProperty(Core.PX.NAME, args.idScopeName).vertex(graph, g) : null

        def patIdFacilityV = args.idFacilityName ? Core.VX.IDENTIFIER_FACILITY.instance().withProperty(Core.PX.NAME, args.idFacilityName).vertex(graph, g) : null

        def patVs = []
        ids.collect({ String.valueOf(it) }).each { id ->
            def patV = Clinical.VX.PATIENT.createVertex(graph)
            def patIdV = Core.VX.IDENTIFIER.instance().withProperty(Core.PX.VALUE, id).vertex(graph, g)
            Core.EX.IS_INSTANCE_OF.relate(g, patIdV, idClassV)
            if (patIdScopeV) Core.EX.IS_SCOPED_BY.relate(g, patIdV, patIdScopeV)
            if (patIdFacilityV) Core.EX.WAS_CREATED_BY.relate(g, patIdV, patIdFacilityV)
            Core.EX.IS_IDENTIFIED_BY.relate(g, patV, patIdV)

            patVs << patV
        }

        return patVs
    }

}