package carnival.core.vine


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.impl.SimpleIRI
import org.eclipse.rdf4j.model.impl.SimpleLiteral
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.manager.RepositoryManager
import org.eclipse.rdf4j.model.impl.SimpleValueFactory

import carnival.core.config.RdfConfig



/**
 * Convenience class that offers useful utilities for interacting with GraphDb RDF triplestores <http://graphdb.ontotext.com/>.
 *
 */
abstract class GraphDbVine extends Vine {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////
    static Logger log = LoggerFactory.getLogger('carnival')

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

}