package carnival.clinical.graph



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.core.graph.Core
import carnival.graph.Base



/**
 *
 *
 */
trait ClinicalGraphTrait {

	///////////////////////////////////////////////////////////////////////////
	// STATIC FIELDS
	///////////////////////////////////////////////////////////////////////////

	/** Carnival log*/
	static Logger log = LoggerFactory.getLogger('carnival')


	///////////////////////////////////////////////////////////////////////////
	// REASONING
	///////////////////////////////////////////////////////////////////////////

	/** */
	public void connectPatientsAndEncounters() {
		log.trace "connectPatientsAndEncounters()"
		assert graph
		def g = graph.traversal()

      	// encounters that have the same identifiers as patients are encounters for that patient
		def res = g.V()
			.match(
	            __.as('patient').hasLabel(Clinical.VX.PATIENT.label).out(Core.EX.IS_IDENTIFIED_BY.label).as('id'),
	            __.as('id').in(Core.EX.IS_IDENTIFIED_BY.label).hasLabel(Clinical.VX.BIOBANK_ENCOUNTER.label).as('encounter'),
	            __.not(__.as('patient').out(Clinical.EX.PARTICIPATED_IN_ENCOUNTER.label).as('encounter')))
	        .addE(Clinical.EX.PARTICIPATED_IN_ENCOUNTER.label)
	        .property(Base.PX.NAME_SPACE.label, Clinical.EX.PARTICIPATED_IN_ENCOUNTER.nameSpace)
	        .from('patient').to('encounter')
        .toList()

        log.trace "Connected ${res.size()} patients and encounters by identifier"


        // encounters that are under the same consent belong to the same patient
		res = g.V()
			.hasLabel(Clinical.VX.PATIENT.label).as('p')
			.out(Clinical.EX.PARTICIPATED_IN_ENCOUNTER.label)
			.hasLabel(Clinical.VX.BIOBANK_ENCOUNTER.label)
			.out(Clinical.EX.PARTICIPATED_IN_FORM_FILING.label).hasLabel('CaseReportForm').as('crf')
			.in(Clinical.EX.IS_UNDER_CONSENT.label)
			.hasLabel(Clinical.VX.BIOBANK_ENCOUNTER.label).as('enc')
			.select('p','enc','crf')
		.toList()

		res.each { m ->
			assert m
			assert m.p
			assert m.enc
			log.debug "m.p: ${m.p} ${m.p.label()}"
			log.debug "m.enc: ${m.enc} ${m.enc.label()}"
			g.V(m.p)
				.out(Clinical.EX.PARTICIPATED_IN_ENCOUNTER.label).as('r')
				.is(m.enc)
				.select('r')
			.tryNext().orElseGet {
				//log.debug "${m.p} participated_in_encounter ${m.enc}"
				Clinical.EX.PARTICIPATED_IN_ENCOUNTER.relate(g, m.p, m.enc)
				//m.p.addEdge(Clinical.EX.PARTICIPATED_IN_ENCOUNTER.label, m.enc)
			}
		}

		log.trace "Found ${res.size()} encounters connected by is_under_consent"
	}

}
