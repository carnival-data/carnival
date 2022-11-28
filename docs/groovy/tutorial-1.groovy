///////////////////////////////////////////////////////////////////////////////
// DEPENDENCIES
///////////////////////////////////////////////////////////////////////////////

@Grab('io.github.carnival-data:carnival-core:3.0.0-SNAPSHOT')
@Grab('io.github.carnival-data:carnival-vine:3.0.0-SNAPSHOT')
@Grab('org.apache.tinkerpop:gremlin-core:3.4.10')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.14.1')


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
// In this example, the works of the author J.K. Rowling are loaded from a 
// public API, the characters in the books are expanded into subject vertices,
// and some sample queries are provided.
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


/** declare a Carnival Vine for the OpenLibrary API endpoint */
class OpenLibraryVine implements Vine { 

    HttpClient client

    public OpenLibraryVine() {
        // modify the default configuration to set the cache mode
        this.vineConfiguration.cache.mode = CacheMode.OPTIONAL
        
        // create an HttpClient
        this.client = HttpClient.newBuilder()
            .version(Version.HTTP_2)
            .followRedirects(Redirect.NORMAL)
        .build();
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
            println(json)

            ObjectMapper objectMapper = new ObjectMapper()
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) 
            
            AuthorSearchResult authorSearchResult = objectMapper.readValue(json, AuthorSearchResult.class);
            println "authorSearchResult: ${authorSearchResult}"
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
            println(json)

            ObjectMapper objectMapper = new ObjectMapper()
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) 
            
            AuthorWorkResult authorWorksResult = objectMapper.readValue(json, AuthorWorkResult.class);
            println "authorWorksResult: ${authorWorksResult}"
            authorWorksResult
        }
    }

}

// create an OpenLibrary vine
OpenLibraryVine vine = new OpenLibraryVine()

// search for authors matching j k rowling
AuthorSearchResult asr = vine
    .method('AuthorSearch')
    .arguments(query:'j k rowling')
    .call()
.result
println "asr: ${asr}"

// pick the result with the highest work count
Document topAuthorDoc = asr.docs
    .findAll({it.work_count > 0})
    .sort({ a, b -> a.work_count <=> b.work_count })
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
List<Work> worksWithPeople = awr.entries.findAll({it.subject_people})
worksWithPeople.each { work ->
    println "\n${work.title}"
    work.subject_people.each { sp ->
        println "- ${sp}"
    }
}


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
// UTILITY
///////////////////////////////////////////////////////////////////////////////

void printGraph(GraphTraversalSource g) {
    def vs = g.V().valueMap(true)
    vs.each { v -> println "$v" }

    def es = g.E()
    es.each { e -> println "$e" }
}


///////////////////////////////////////////////////////////////////////////////
// CREATE A CARNIVAL
///////////////////////////////////////////////////////////////////////////////

Carnival carnival = CarnivalTinker.create()

/*
Path currentRelativePath = Paths.get("")
Path neo4jGraphDir = currentRelativePath.resolve('carnival-home/neo4j-dir-custom')
String neo4jGraphDirString = neo4jGraphDir.toAbsolutePath().toString()

CarnivalNeo4jConfiguration carnivalNeo4jConf = CarnivalNeo4jConfiguration.defaultConfiguration()
carnivalNeo4jConf.gremlin.neo4j.directory = neo4jGraphDirString

CarnivalNeo4j.clearGraph(carnivalNeo4jConf)
Carnival carnival = CarnivalNeo4j.create(carnivalNeo4jConf)
*/



class Loaders implements GraphMethods {
    
    class LoadBooks extends GraphMethod {

        void execute(Graph graph, GraphTraversalSource g) {
            assert args.works
            println "Loaders.LoadBooks.execute args.works:${args.works}"
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

}
Loaders loaders = new Loaders()

carnival.withGremlin { graph, g ->
    loaders
        .method('LoadBooks')
        .arguments(works:worksWithPeople)
    .ensure(graph, g)
}

carnival.withGremlin { graph, g ->
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

    /*g.V().isa(VX.COAPPEARANCE).each { caV ->
        println "caV persons: " + g.V(caV).in(EX.PARTICIPATES_IN).isa(VX.PERSON).properties(PX.NAME).value().toList()
        println "caV books: " + g.V(caV).out(EX.OCCURS_IN).isa(VX.BOOK).properties(PX.TITLE).value().toList()
    }*/

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
    
    println "people who appeared in the most books together"
    cas1.take(15).each { rec ->
        println "" + rec.personVs.collect({PX.NAME.valueOf(it)}).join(', ')
        rec.bookVs.each { bookV -> println "" + PX.TITLE.valueOf(bookV) }
    }

    println "people who appeared in only one book"
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

carnival.withGremlin { graph, g ->
    //printGraph(g)
}

carnival.close()
