package carnival.core.vine


import groovy.util.logging.Slf4j

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.impl.SimpleIRI
import org.eclipse.rdf4j.model.impl.SimpleLiteral
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RepositoryManager
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

import carnival.core.config.RdfConfig

import org.eclipse.rdf4j.query.BooleanQuery
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult


/**
 * Convenience class that offers useful utilities for interacting with GraphDb RDF triplestores <http://graphdb.ontotext.com/>.
 *
 */
@Slf4j
abstract class GraphDbVine extends Vine {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////
    RdfConfig rdfConfig

    RepositoryManager repositoryManager
    Repository repository
    RepositoryConnection connection

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////
    /**
    * Initilize RDF4J repositoryManager and open a connection to the repository
    *
    * @See <http://graphdb.ontotext.com/documentation/free/using-graphdb-with-the-rdf4j-api.html#rdf4j-api>
    */
    public GraphDbVine(RdfConfig rdfConfig) {
        super()
        assert rdfConfig
        this.rdfConfig = rdfConfig

        this.repositoryManager = RemoteRepositoryManager.getInstance(rdfConfig.url, rdfConfig.user, rdfConfig.password)
        repositoryManager.initialize()
    }

    public void open() {
        this.repository = repositoryManager.getRepository(rdfConfig.repository)
        assert this.repository : "RepositoryManager ${repositoryManager} unable to open repository ${rdfConfig.repository}"

        this.connection = repository.getConnection()
    }

    public close() {
    	// Shutdown connection, repository and manager
		if (this.connection) this.connection.close()
		//repository.shutDown()
		//repositoryManager.shutDown()
    }


    ///////////////////////////////////////////////////////////////////////////
    // GENERIC QUERY METHODS
    ///////////////////////////////////////////////////////////////////////////
 	public Boolean isEmpty() {
 		return connection.isEmpty()
 	}

    public clearRepository() {
    	connection.clear()
    }

    public clearNamedGraph(String namedGraph) {
        IRI graph = SimpleValueFactory.getInstance().createIRI(namedGraph)
        return clearNamedGraph(graph)
    }

    public clearNamedGraph(IRI namedGraph) {
        connection.clear(namedGraph)
    }


    // TODO: wrap in try/catch loop, add transaction handling
    //public addTriple(Resource subjectStr, IRI predicateStr, Value objectStr) {
    public addLiteralTriple(String subjectStr, String predicateStr, String objectStr) {
    	def sub = new SimpleIRI(subjectStr)
    	def pred = new SimpleIRI(predicateStr)
    	def obj = new SimpleLiteral(objectStr)

    	connection.add(sub, pred, obj)
    }

    public addIriTriple(String subjectStr, String predicateStr, String objectStr) {
        def sub = new SimpleIRI(subjectStr)
        def pred = new SimpleIRI(predicateStr)
        def obj = new SimpleIRI(objectStr)

        connection.add(sub, pred, obj)
    }

    public addModel(Model m) {
        //connection.begin() // begin transaction
        connection.add(m)
        //connection.commit()
    }

    public removeTriple(Resource subject, IRI predicate, Value object) {
    	connection.remove(subject, predicate, object)
    }

    public Boolean launchSparqlBooleanQuery(String query) {
        BooleanQuery boolResult = connection.prepareBooleanQuery(query)
        return boolResult.evaluate()
    }

    public TupleQueryResult launchSparqlSelectQuery(String query) {
        TupleQuery tupleQuery = connection.prepareTupleQuery(query)
        return tupleQuery.evaluate()
    }
}