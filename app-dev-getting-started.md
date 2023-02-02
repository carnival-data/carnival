# Application Development

There is a Gradle plugin to aid in the development of Groovy applications and libraries that use Carnival.  

 
## Library
In this context, a library is a package of domain specific functionality implemented using Carnival components that is intended to be included in an application.  

The three main components of Carnival that are designed to be used in this way are [Vine](https://github.com/carnival-data/carnival/blob/d0495a40dbbf4e6caa535345653a0e79e60611ac/app/carnival-core/src/main/groovy/carnival/core/vine/Vine.groovy), [GraphMethod](https://github.com/carnival-data/carnival/blob/d0495a40dbbf4e6caa535345653a0e79e60611ac/app/carnival-core/src/main/groovy/carnival/core/graph/GraphMethod.groovy), and the graph modelling machinery found in **carnival/app/carnival-graph/src/main/groovy/carnival/graph/**.

An example of a domain specific library could be package containing vine methods to pull data from a data source, a graph model that defines how the concepts in the pulled data will be instantiated in a graph, and graph methods to instantiate and operate over the graph data.  

[Carnival-openspecimen](https://github.com/carnival-data/carnival-openspecimen) is an example of a domain specific library that contains a vine to pull data from the OpenSpecimen relational database.

- [Create a library](app-dev-library.md)

## Application
In this context, an application is code that uses Carnival components and libraries and can be executed in a JVM.   

[Carnival-demo-biomedical](https://github.com/carnival-data/carnival-demo-biomedical) is an example of a Micronaut application that exercises all the core compoments of Carnival and demonstrates a canonical use case.

- [Create an application](app-dev-application.md)
