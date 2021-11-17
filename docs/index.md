# Carnival

**Carnival** is a semantically driven informatics toolkit that enables the aggregation of data from disparate sources into a unified property graph and provides mechanisms to model and interact with the graph in well-defined ways.

## Key Features
- A data caching mechanism to ease the computational burden of data aggregation during the development process and promotes data provenance
- A graph modelling framework that ensures graph data remain consistent
- A lightweight graph algorithm framework that promotes graph building recipes with automated provenance

## Data Aggregation
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

## Graph Modelling
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
def personV = MyDomain.PERSON.instance().create(graph)

// create a NAME vertex if one with matching properties does not already exist.
def nameV = MyDomain.NAME.instance().withProperties(
    MyDomain.PX.FIRST, 'Alice',
    MyDomain.PX.LAST, 'Smith'
).ensure(graph, g)

// link personV and nameV
MyDomain.EX.IS_NAMED.instance().from(personV).to(nameV).create()
```

To learn more about graph modelling, see the section [Graph Model](graph-model.md).



## Graph Algorithms
The ability to programmatically update a graph is part of what makes property graphs so powerful and useful for informatics.  However, it is easy to lose track of which routines were called and what their effects were.  Carnival provides a framework to wrap graph routines so that they can be called and tracked in standardized ways.

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

To learn more about graph methods, see [Graph Methods](graph-method.md).



## Core Development
- [Developer Setup](developer-setup.md)
- [Production Builds](production-buids.md)

## Application Development
- [Data Tables](data-tables.md)
  - [Mapped Data Table](mapped-data-table.md)
  - [Generic Data Table](generic-data-table.md)
- [Vines](vines.md)
- [Graph Model](graph-model.md)
- [Graph Methods](graph-method.md)

## API Documentation
- [Groovy API documentation](groovydoc/index.html)
