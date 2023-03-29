///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@GrabResolver(name='io.github.carnival-data', root='https://s01.oss.sonatype.org/content/repositories/snapshots/')
@Grab('io.github.carnival-data:carnival-core:3.0.1')
@Grab('io.github.carnival-data:carnival-vine:3.0.1')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.10')
@Grab('com.fasterxml.jackson.core:jackson-databind:2.14.1')


///////////////////////////////////////////////////////////////////////////////
// IMPORTS
///////////////////////////////////////////////////////////////////////////////

import java.nio.file.Path
import java.nio.file.Paths
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

import groovy.transform.ToString

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge

import carnival.vine.Vine
import carnival.vine.CacheMode
import carnival.vine.JsonVineMethod
import carnival.core.Carnival
import carnival.core.CarnivalTinker
import carnival.core.CarnivalNeo4j
import carnival.core.CarnivalNeo4jConfiguration
import carnival.core.graph.GraphMethods
import carnival.core.graph.GraphMethod
import carnival.graph.VertexModel
import carnival.graph.EdgeModel
import carnival.graph.PropertyModel

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature


///////////////////////////////////////////////////////////////////////////////
// In this example, the works of the author Stephenie Meyer are loaded from a 
// public API, the books and the characters in the books are expanded into 
// vertices, and some sample queries are provided.
///////////////////////////////////////////////////////////////////////////////


///////////////////////////////////////////////////////////////////////////////
// DATA AGGREGATION
///////////////////////////////////////////////////////////////////////////////

@ToString(includePackage=false,includeNames=true)
class Document {
    String key
    String type
    String name
    String birth_date
    Integer work_count
}

/** Result of an author searcn. */
@ToString(includePackage=false,includeNames=true)
class AuthorSearchResult {
    Integer numFound

    List<Document> docs
}

/** An author */
@ToString(includePackage=false,includeNames=true)
class Author {
    String name
    String description
    String key
}

/** An author's work. */
@ToString(includePackage=false,includeNames=true)
class Work {
    String title
    //String description
    String key
    List<String> subject_people
}

/** Author's works search result. */
@ToString(includePackage=false,includeNames=true)
class AuthorWorkResult {
    Integer size
    List<Work> entries
}


/** Declare a Carnival Vine for the OpenLibrary API endpoint */
class OpenLibraryVine implements Vine { 

    /** to be used to communicate with the OpenLibrary API */
    HttpClient client

    /** Jackson object mapper for JSON de-serialization */
    ObjectMapper objectMapper

    /** constructor */
    public OpenLibraryVine() {
        // create the HttpClient
        this.client = HttpClient.newBuilder()
            .version(Version.HTTP_2)
            .followRedirects(Redirect.NORMAL)
        .build();

        // create the object mapper
        objectMapper = new ObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) 
    }

    /** JSON Vine method to search for an author */
    class AuthorSearch extends JsonVineMethod<AuthorSearchResult> {
        AuthorSearchResult fetch(Map args = [:]) {
            assert args.query
            String qEnc = URLEncoder.encode(args.query, 'UTF-8') 
            String uri = 'https://openlibrary.org/search/authors.json?q=' + qEnc
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))    
                .GET()
            .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            String json = response.body() 
            AuthorSearchResult authorSearchResult = objectMapper.readValue(json, AuthorSearchResult.class);
            println "AuthorSearch.fetch authorSearchResult: ${authorSearchResult}"
            authorSearchResult
        }
    }

    /** JSON Vine method to get an author's works */
    class AuthorWorks extends JsonVineMethod<AuthorWorkResult> {
        AuthorWorkResult fetch(Map args = [:]) {
            assert args.key
            String uri = "https://openlibrary.org/authors/${args.key}/works.json"
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))    
                .GET()
            .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            String json = response.body() 
            AuthorWorkResult authorWorksResult = objectMapper.readValue(json, AuthorWorkResult.class);
            println "AuthorWorks.fetch authorWorksResult: ${authorWorksResult}"
            authorWorksResult
        }
    }

}

// create an OpenLibrary vine
OpenLibraryVine vine = new OpenLibraryVine()

// modify the default configuration to set the cache mode
vine.vineConfiguration.cache.mode = CacheMode.OPTIONAL

// search for an author
AuthorSearchResult asr = vine
    .method('AuthorSearch')
    .arguments(query:'stephenie meyer')
    .call()
.result
println "asr: ${asr}"

// pick the result with the highest work count
Document topAuthorDoc = asr.docs
    .findAll({it.work_count > 0})
    .sort({ a, b -> a.work_count <=> b.work_count })
    .reverse()
.first()
println "topAuthorDoc: ${topAuthorDoc}"

// get the works of the top result
AuthorWorkResult awr = vine
    .method('AuthorWorks')
    .arguments(key:topAuthorDoc.key)
    .call()
.result
println "awr: ${awr}"

// filter the works for only those with subject_people
println "\nworks:"
awr.entries.sort({ a, b -> a.title <=> b.title }).each { work ->
    println "${work.title}"
}
List<Work> worksWithPeople = awr.entries.findAll({it.subject_people})
println "\nworks with people:"
worksWithPeople.each { work ->
    println "${work.title}"
    work.subject_people.each { sp ->
        println "- ${sp}"
    }
}



///////////////////////////////////////////////////////////////////////////////
// UTILITY
///////////////////////////////////////////////////////////////////////////////

/** Utility method to print the graph elements to the console */
void printGraph(GraphTraversalSource g) {
    def vs = g.V().valueMap(true)
    vs.each { v -> println "$v" }

    def es = g.E()
    es.each { e -> println "$e" }
}


///////////////////////////////////////////////////////////////////////////////
// CREATE A CARNIVAL
//
// This example demonstrated the use of TinkerGrapha and Neo4j carnivals.  Only
// one carnival need be creted.
///////////////////////////////////////////////////////////////////////////////

// Using TinkerGraph 

// create the carnival
Carnival carnival = CarnivalTinker.create()

// Using Neo4j

// choose a custom directory for the neo4j graph
Path currentRelativePath = Paths.get("")
Path neo4jGraphDir = currentRelativePath.resolve('carnival-home/neo4j-dir-custom')
String neo4jGraphDirString = neo4jGraphDir.toAbsolutePath().toString()
CarnivalNeo4jConfiguration carnivalNeo4jConf = CarnivalNeo4jConfiguration.defaultConfiguration()
carnivalNeo4jConf.gremlin.neo4j.directory = neo4jGraphDirString

// optionally delete an existing neo4j graph directory
//CarnivalNeo4j.clearGraph(carnivalNeo4jConf)

// create the carnival
//Carnival carnival = CarnivalNeo4j.create(carnivalNeo4jConf)


///////////////////////////////////////////////////////////////////////////////
// DEFINE A GRAPH MODEL
///////////////////////////////////////////////////////////////////////////////

@VertexModel
enum VX {
    BOOK (
        propertyDefs:[
            PX.TITLE
        ]
    ),
    PERSON (
        propertyDefs:[
            PX.NAME
        ]
    ),
    COAPPEARANCE
}

@PropertyModel
enum PX {
    TITLE,
    NAME
}

@EdgeModel
enum EX {
    IS_DERIVED_FROM,
    APPEARS_IN,
    PARTICIPATES_IN,
    OCCURS_IN
}


///////////////////////////////////////////////////////////////////////////////
// ADD THE MODELS
///////////////////////////////////////////////////////////////////////////////

carnival.addModel(VX)
carnival.addModel(EX)


///////////////////////////////////////////////////////////////////////////////
// DEFINE GRAPH METHODS
///////////////////////////////////////////////////////////////////////////////

/** 
 * OpenLibraryMethods contains graph methods to operate over the data loaded
 * from the OpenLibrary API.
 *
 */
class OpenLibraryMethods implements GraphMethods {
    
    /**
     * Load the books and subject_people data into the graph.
     *
     */
    class LoadBooks extends GraphMethod {
        void execute(Graph graph, GraphTraversalSource g) {
            assert args.works
            println "OpenLibraryMethods.LoadBooks.execute args.works:${args.works}"
            args.works.each { work ->
                Vertex bookV = VX.BOOK.instance().withProperties(
                    PX.TITLE, work.title
                ).ensure(graph, g)
                work.subject_people.each { sp ->
                    Vertex subjectPersonV = VX.PERSON.instance().withProperties(
                        PX.NAME, sp
                    ).ensure(graph, g)
                    EX.APPEARS_IN.instance().from(subjectPersonV).to(bookV).ensure(g)
                }
            }
        }
    }

    /**
     * Traverse the graph, create COAPPEARANCE vertices for pairs of characters 
     * who appear in a book together.
     *
     */
    class CreatCoappearances extends GraphMethod {
        void execute(Graph graph, GraphTraversalSource g) {
            g.V()
                .isa(VX.PERSON).as('p1')
                .out(EX.APPEARS_IN)
                .isa(VX.BOOK).as('b')
                .in(EX.APPEARS_IN)
                .isa(VX.PERSON).as('p2')
                .select('b', 'p1', 'p2')
            .each { m ->
                if (m.p1 == m.p2) return

                Vertex caV = g.V()
                    .isa(VX.COAPPEARANCE).as('ca')
                    .in(EX.PARTICIPATES_IN)
                    .is(m.p1)
                    .select('ca')
                    .in(EX.PARTICIPATES_IN)
                    .is(m.p2)
                    .select('ca')
                .tryNext().orElseGet {
                    def newCaV = VX.COAPPEARANCE.instance().create(graph)
                    EX.PARTICIPATES_IN.instance().from(m.p1).to(newCaV).create()
                    EX.PARTICIPATES_IN.instance().from(m.p2).to(newCaV).create()
                    newCaV
                }
                
                EX.OCCURS_IN.instance().from(caV).to(m.b).ensure(g)
            }
        }
    }

}


///////////////////////////////////////////////////////////////////////////////
// DATA BUILD
///////////////////////////////////////////////////////////////////////////////

// create an OpenLibraryMethods object
OpenLibraryMethods openLib = new OpenLibraryMethods()

// call the relevant graph methods to build the graph
carnival.withGremlin { graph, g ->
    openLib
        .method('LoadBooks')
        .arguments(works:worksWithPeople)
    .ensure(graph, g)

    openLib.method('CreatCoappearances').ensure(graph, g)
}


///////////////////////////////////////////////////////////////////////////////
// QUERIES
//
// These sample queries use Tinkerpop Gremlin traversals, Groovy data
// manipulation methods, and Carnival.  The discrepancies of the OpenLibrary
// data are not addressed.
///////////////////////////////////////////////////////////////////////////////

carnival.withGremlin { graph, g ->

    // query the coappearances to  find the people who appeared in the most
    // books together.
    def cas1 = g.V()
        .isa(VX.COAPPEARANCE)
        .toList()
        .collect({ caV -> 
            def personVs = g.V(caV).in(EX.PARTICIPATES_IN).isa(VX.PERSON).toList()
            [caV:caV, personVs:personVs]
        })
        .collect({ rec ->
            rec.bookVs = g.V(rec.caV).out(EX.OCCURS_IN).isa(VX.BOOK).toList()
            rec
        })
    .sort({ a, b ->
        a.bookVs.size() <=> b.bookVs.size()
    })
    .reverse()
    
    println "\npeople who appeared in the most books together\n"
    cas1.take(15).each { rec ->
        println "" + rec.personVs.collect({PX.NAME.valueOf(it)}).join(', ')
        rec.bookVs.each { bookV -> println "  - " + PX.TITLE.valueOf(bookV) }
    }

    // query the graph to find the people who appeared in only one book
    println "\npeople who appeared in only one book\n"
    g.V().isa(VX.PERSON).toList()
    .collect({ pV ->
        def bookVs = g.V(pV).out(EX.APPEARS_IN).isa(VX.BOOK).toList()
        [pV:pV, bookVs:bookVs]
    })
    .findAll({ rec ->
        rec.bookVs.size() == 1
    })
    .sort({ a, b ->
        PX.NAME.valueOf(a.pV) <=> PX.NAME.valueOf(b.pV)
    })
    .each({ rec ->
        println "" + PX.NAME.valueOf(rec.pV)
    })
}

// close the Carnival graph
carnival.close()
