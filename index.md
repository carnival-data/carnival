# Carnival


## Contents

### Introduction
- [Summary](#summary)
- [Statement of Need](#statement-need)
- [Data Aggregation](#data-aggregation)
- [Graph Modelling](#graph-model)
- [Graph Algorithms](#graph-algo)

### Development
- [Core Development](#core-development)
- [Application Development](#app-development)
- [Application Programmer Interface](#api)

## <a name="summary"></a>Summary
Loading, cleansing, and organizing data can dominate the time spent on a data science project [[Forbes]](https://www.forbes.com/sites/gilpress/2016/03/23/data-preparation-most-time-consuming-least-enjoyable-data-science-task-survey-says/?sh=5eebf58e6f63)[[Anaconda]](https://www.anaconda.com/state-of-data-science-2020?utm_medium=press&utm_source=anaconda&utm_campaign=sods-2020&utm_content=report).  This phenomenon is exacerbated in human subjects research at an academic medical institution where data are very complex, reside in disparate repositories with varying levels of accesibility, are coded by separate yet overlapping coding systems, frequently rely on manual data entry, and change over time.  Data provenance and reproducibility of results are important factors in human subjects research.  It is no easy task to implement a robust consistent data pipeline with clear data provenance that can be rerun when source data change.  While there are several mature libraries and toolkits that enable visualization and statistical computation once the analytical data set is generated, there are comparatively fewer informatics offerings to address these concerns.  

Existing ETL technologies such as [Microsoft SQL Server Integration Services](https://docs.microsoft.com/en-us/sql/integration-services/sql-server-integration-services?view=sql-server-ver15) help with data staging.  Similarly, data manipulation tools like [pandas](https://pandas.pydata.org) facilitate transformation of series and matrix data.  **Carnival** distinguishes itself by offering a lightweight data caching mechanism coupled with data manipulation services built on a property graph rather than arrays and data frames.  This unique combination empowers informatics programmers to build pipelines, utilities, and applications that are comparatively richer in semantics and provenance.

Knowledge bases in Resource Description Framework (RDF) triplestores can be valuable tools to harmonize and enrich complex data.  While there are relational to RDF mappers, transforming source relational data into RDF triples is challenging.  Property graphs offer a middle ground between relational and RDF.  They lack the native ability to benefit from ontologies represented in RDF, but are more friendly to algorithmic computation.  

**Carnival** was developed to provide programmers with tools to go from source data to a well formatted property graph and onward to RDF with rich semantics and provenance.  There are two main components to Carnival.  The first is a data caching mechanism that supports the efficient aggregation of data from disparate sources.  The second is a layer built on top of [Apache Tinkerpop](https://tinkerpop.apache.org) that seeks to provide more standardized and semantically driven methods of interacting with a property graph.


## <a name="statement-need"></a>Statement of need

**Carnival** is a semantically driven informatics toolkit that enables the aggregation of data from disparate sources into a unified property graph and provides mechanisms to model and interact with the graph in well-defined ways.

### Key Features
- A data caching mechanism to ease the computational burden of data aggregation during the development process and promotes data provenance
- A graph modelling framework that ensures graph data remain consistent
- A lightweight graph algorithm framework that promotes graph building recipes with automated provenance

### Uses

#### Production of analytical data sets
Carnival was initially developed to facilitate the production of analytical data sets for human subjects research.  The source data repositories included a relational data warehouse accessible by SQL, a [REDCap](https://www.project-redcap.org) installation accessible by API, and manually curated data files in CSV format.  Data pertaining to the set of study subjects was striped across each of these data sources.  Using Carnival, a data pipeline was implemented to pull data from the data sources, instantiate them in a property graph, clean and harmonize them, and produce analytical data sets at required intervals.

#### Queries over enriched data
A key challenge of human subjects research is to locate patients to recruit to a study, frequently done by searching a research data set containing raw patient data.  Potential recruits need to be stratified by attributes, such as age, race, and ethnicity, matched against inclusion criteria, such as the presence of a diagnosis code, and filtered by exclusion criteria, such as a treatment modality.  **Carnival** has been used effectively in this area by loading the relevant raw data into a graph, stratifying and categorizing patients by the relevant criteria, then using graph traversals to extract the patients who are potential recruits.

[A novel tool for standardizing clinical data in a semantically rich model](https://doi.org/10.1016/j.yjbinx.2020.100086)
[Carnival: A Graph-Based Data Integration and Query Tool to Support Patient Cohort Generation for Clinical Research](https://ebooks.iospress.nl/volumearticle/51943)

#### System integrations
**Carnival's** ability to integrate data from disparate resources into a flexible computational resource enables data driven system integrations.  For example, **Carnival** has been used effectictively to integrate a custom help desk ticketing system with Monday.com.  The help desk ticketing system was developed locally with a back-end relational database.  Monday.com is accessible via its API for reads and writes.  By modelling the help desk and Monday.com data as separate graph models, then using a third graph model to integrate the two, a **Carnival** integration application was developed to integrate the two data sets and compute changes that needed to occur based on the state of the data.  In this example, **Carnival** was partnered with [Micronaut](https://micronaut.io) and deployed as a [Docker](https://www.docker.com) container on [Microsoft Azure](https://azure.microsoft.com/).  The service would build an in-memory graph at regular minute intervals, compute the changes that were required, then call the appropriate web services to execute the logic of the integration.



## <a name="data-aggregation"></a>Data Aggregation
Carnival supports data aggregation by caching the results of queries and API calls.    This can be beneficial during the development cycle by removing unnecessary and costly query operations.

The following pseudo-code demonstrates how caching works.

```groovy
package mypackage


/**
 * By implementing Vine, the methods in this class will be supported with
 * caching.
 * 
 */
class MyDataAggregator implements Vine {

    /* A utility method to connect to a database resource */
    Sql connect() {
        Sql.newInstance(
            driver: 'the.database.driver',
            url: "the.database.url",
            user: 'the.user',
            password: 'the.password'
        )
    }
    

    /**
     * Data aggregation methods are implemented as sub-classes.  By extending
     * MappedDataTableVineMethod, we are telling Carnival that this method will
     * return a data table with a primary key.
     * 
     */
    class GetData extends MappedDataTableVineMethod { 

        // the data aggregation logic goes inside a fetch method
        MappedDataTable fetch(Map args) {

            // obtain a SQL connection
            def sql = connect()

            // create an empty data table
            def mdt = createDataTable(idFieldName:'ID')

            try {

                // run the query and iterate over each row result
                sql.eachRow(query) { row ->

                    // add the entire row to the data table
                    mdt.dataAdd(row)
                }

            } finally {
                if (sql) sql.close()
            }

            // return the data table
            mdt
        }

    }
    
}


// our vine method can be called as follows
MyDataAggregator myDataAggregator = new MyDataAggregator()
def mdt = myDataAggregator
    .method('GetData')
    .call()
.result

```

The first time the vine method is called, the query will be excuted and the result cached in the configurable cache directory as files that would look something like the following.

```
mypackage-MyDataAggregator-GetData.yaml
mypackage-MyDataAggregator-GetData.csv
```

The Yaml file contains information about the method invocation, such as the class, method, and arguments that were used and the datetime the query completed. The CSV file contains the actual data that were returned.  When caching is turned on, future calls to GetData will return the cached data rather than re-doing the query.

For more information about writing data aggregation methods, see the sections on [Data Tables](data-tables.md) and [Vines](vines.md).

## <a name="graph-model"></a>Graph Modelling
The schema-less nature of property graphs is both a strength and a weakness.  The lack of schema enforcement enables rapid development.  However, that same lack of enforcement can lead to data inconsistencies.  Carnival includes a graph modelling mechanism that seeks to balance flexibility and consistency.

```groovy
/**
 * Graph models are defined in the context of a class.
 * 
 */
class MyDomain {

    /**
     * The following enum defines two vertex types: PERSON and NAME.  PERSON
     * accepts no properties, while NAME has two required properties, FIRST
     * and LAST.
     * 
     */ 
    @VertexDefinition
    static enum VX {
        PERSON,

        NAME(
            propertyDefs:[
                PX.FIRST.constraints(index:true, required:true, unique:true),
                PX.LAST.constraints(index:true, required:true, unique:true),
            ]
        ),
    }


    /**
     * The following enum defines an edge type that has no properties, but does
     * have domain and range restrictions.
     * 
     */ 
    @EdgeDefinition
    static enum EX {
        IS_NAMED(
            domain:[VX.PERSON],
            range:[VX.NAME]
        )
    }


    /**
     * The following enum defines allowed properties.
     * 
     */ 
    @PropertyDefinition
    static enum PX {
        FIRST,
        LAST,
        HAS_COOL_NAME
    }

}


// create a PERSON vertex
def personV = MyDomain.VX.PERSON.instance().create(graph)

// create a NAME vertex if one with matching properties does not already exist.
def nameV = MyDomain.VX.NAME.instance().withProperties(
    MyDomain.PX.FIRST, 'Alice',
    MyDomain.PX.LAST, 'Smith'
).ensure(graph, g)

// link personV and nameV
MyDomain.EX.IS_NAMED.instance().from(personV).to(nameV).create()
```

To learn more about graph modelling, see the section [Graph Model](graph-model.md).


**Add a graph image?**



## <a name="graph-algo"></a>Graph Algorithms
The ability to programmatically update a graph is part of what makes property graphs so powerful and useful for informatics.  However, it is easy to lose track of which routines were called and what their effects were.  Carnival provides a framework to wrap graph routines so that they can be called and tracked in standardized ways.

**Mention that graph methods create Process vertices in the graph??**

```groovy
/**
 * By implementing GraphMethods, the graph methods in the Reasoners class can
 * take advantage of Carnival's standardized invocation and tracking.
 * 
 */ 
class Reasoners implements GraphMethods {

    /**
     * Graph methods are implemented as sub-classes and must extend 
     * GraphMethod.
     * 
     */ 
    class HasCoolName extends GraphMethod {

        /**
         * The logic of a graph method is implemented inside an execute method.
         * 
         */ 
        void execute(Graph graph, GraphTraversalSource g) {

            // traverse every person -[:is_named]-> name graph fragment
            g.V()
                .isa(MyDomain.VX.PERSON).as('p')
                .out(MyDomain.EX.IS_NAMED)
                .isa(MyDomain.VX.NAME).as('n')
                .select('p', 'n')
            .each {

                // if the firs name is Alice, then the person has a cool name.
                if (MyDomain.PX.FIRST.valueOf(m.n) == 'Alice') {
                    MyDomain.PX.HAS_COOL_NAME.set(m.p, true)
                }
                
            }

        }

    }

}
```

**Add a graph image?**

To learn more about graph methods, see [Graph Methods](graph-method.md).



## <a name="core-development"></a>Core Development
- [Developer Setup](developer-setup.md)
- [Production Builds](production-buids.md)
- [Building Documentation](documentation.md)
- [Default Carnival Schemas](schema.md)

## <a name="app-development"></a>Application Development
- [Getting Started](app-dev-getting-started.md)
    - [Create a library](app-dev-library.md)
    - [Create an application](app-dev-application.md)
- [Data Tables](data-tables.md)
  - [Mapped Data Table](mapped-data-table.md)
  - [Generic Data Table](generic-data-table.md)
- [Vines](vines.md)
- [Graph Model](graph-model.md)
- [Graph API](graph-api.md)
- [Graph Methods](graph-method.md)

## <a name="api"></a> Application Programmer Interface 
- [Groovy API documentation](groovydoc/index.html)
