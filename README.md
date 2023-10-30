[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/pennbiobank/pennai/carnival-public/master/LICENSE)
[![Carnival CI](https://github.com/carnival-data/carnival/actions/workflows/carnival_ci.yml/badge.svg)](https://github.com/carnival-data/carnival/actions/workflows/carnival_ci.yml)
<a href='https://coveralls.io/github/pmbb-ibi/carnival?branch=master'><img src='https://coveralls.io/repos/github/pmbb-ibi/carnival/badge.svg?branch=master' alt='Coverage Status' /></a>
[![Maven Central](https://img.shields.io/maven-central/v/io.github.carnival-data/carnival-core.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.carnival-data/carnival-core)

# Carnival

**Carnival** Carnival is an open source JVM data unification framework that allows for a large variety of extract, transform, and load (ETL), integration, and analysis tasks related to relational data and property graphs. Some key functionality includes a graph model specification, the aggregation of data from disparate sources into a unified property graph, and tools to reason over and interact with graph data using bounded operations. 

## External Resources

-   [Documentation](https://carnival-data.github.io/carnival/)
-   [API Documentation](https://carnival-data.github.io/carnival/groovydoc/index.html)
-   [Example Demonstration Project](https://github.com/carnival-data/carnival-micronaut)


## <a name="overview"></a> Overview

Carnival has three principal components: a graph modeling architecture, a caching facility for aggregating data from disparate data sources, and a framework for implementing graph algorithms.  The graph modeling architecture is a layer over Java enumerations and Tinkerpop that allow a graph to be modeled and consumed by Tinkerpop traversal steps.  The caching facility supports the aggregation and caching of data from relational database and JSON API sources.  The graph algorithm framework provides a structured way to define and execute algorithms that operate over the property graph.


## <a name="packages"></a> Packages

### Core Packages

API Docs | Source | Description
--- | --- | ---
[carnival-core API](https://carnival-data.github.io/carnival/groovydoc/index.html?carnival/core/package-summary.html) | [app/carnival-core/src/main/groovy/carnival/core](app/carnival-core/src/main/groovy/carnival/core) | Basic Carnival framework. Implements the basic Carnival framework classes (vines, carnival modeling framework, carnival graph algorithm framework, etc). Defines the [core carnival graph model](https://github.com/carnival-data/carnival/blob/master/app/carnival-core/src/main/groovy/carnival/core/graph/Core.groovy). This model defines key carnival concepts such as processes, databases, and namespaces.
[carnival-vine API](https://carnival-data.github.io/carnival/groovydoc/index.html?carnival/vine/package-summary.html) | [app/carnival-vine/src/main/groovy/carnival/vine](app/carnival-vine/src/main/groovy/carnival/vine) | Framework for data adaptors called vines which faciliate loading and aggregation of source data. Implements data caching facilities.
[carnival-graph API](https://carnival-data.github.io/carnival/groovydoc/index.html?carnival/graph/package-summary.html) | [app/carnival-graph/src/main/groovy/carnival/graph](app/carnival-graph/src/main/groovy/carnival/graph) | Framework for defining carnival graph schemas (vertex and edge definitions). Contains the basic vertex, edge, and property classes.
[carnival-util API](https://carnival-data.github.io/carnival/groovydoc/index.html?carnival/util/package-summary.html) | [app/carnival-util/src/main/groovy/carnival/util](app/carnival-util/src/main/groovy/carnival/util) | Standalone package that contains utility and helper classes such as data tables, reports, and SQL utilties, which are primarily used for dealing with relational data.
[Plugin Docs](app/carnival-gradle/README.md) | [app/carnival-gradle](app/carnival-gradle) | Gradle plugin for building Carnival applications and libraries.

## <a name="contribution-guide"></a> Contribution Guide
Carnival is an open source project and welcomes contributions! Please see the [Contribution Guide](CONTRIBUTING.md) for ways to contribute.


