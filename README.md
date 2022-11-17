[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/pennbiobank/pennai/carnival-public/master/LICENSE)
[![Carnival CI](https://github.com/pmbb-ibi/carnival/actions/workflows/carnival_ci.yml/badge.svg)](https://github.com/pmbb-ibi/carnival/actions/workflows/carnival_ci.yml)
<a href='https://coveralls.io/github/pmbb-ibi/carnival?branch=master'><img src='https://coveralls.io/repos/github/pmbb-ibi/carnival/badge.svg?branch=master' alt='Coverage Status' /></a>
[![Maven Central](https://img.shields.io/maven-central/v/io.github.carnival-data/carnival-core.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.carnival-data/carnival-core)

# Carnival

**Carnival** Carnival is an open source JVM data unification framework that allows for a large variety of extract, transform, and load (ETL), integration, and analysis tasks related to relational data and property graphs. Some key functionality includes a graph model specification, the aggregation of data from disparate sources into a unified property graph, and tools to reason over and interact with graph data using bounded operations. 

## External Resources

-   [Usage Website](https://carnival-data.github.io/carnival/)
-   [GroovyDoc API Documentation](https://carnival-data.github.io/carnival/groovydoc/index.html)
-   [Demonstration Project](https://github.com/carnival-data/carnival-micronaut)

## Contents
- [Overview](#overview)
- [Packages](#packages)
- [Contribution Guide](README.md#contribution-guide)

## <a name="overview"></a> Overview

Carnival has three principal components: a graph modeling architecture, a caching facility for aggregating data from disparate data sources, and a framework for implementing graph algorithms.  The graph modeling architecture is a layer over Java enumerations and Tinkerpop that allow a graph to be modeled and consumed by Tinkerpop traversal steps.  The caching facility supports the aggregation and caching of data from relational database and JSON API sources.  The graph algorithm framework provides a structured way to define and execute algorithms that operate over the property graph.


## <a name="packages"></a> Packages

### Core Packages

Name | Description
--- | ---
carnival-core | Basic Carnival framework. Implements the basic Carnival framework classes (vines, reapers, reasonsers, etc). Defines the basic carnival graph schema (processes, databases). [Core model](https://github.com/pmbb-ibi/carnival/blob/master/app/carnival-core/src/main/groovy/carnival/core/graph/Core.groovy)
carnival-vine | Mechanisms to faciliate the aggregation of data with data caching.  
[carnival-graph](app/carnival-graph/README.md) | Framework for defining carnival graph schemas (vertex and edge definitions). Contains the basic vertex, edge, and property classes.
[carnival-util](app/carnival-util/README.md) | Standalone package that contains utility and helper classes such as data tables, reports, and SQL utilties, which are primarily used for dealing with relational data.
[carnival-gradle](app/carnival-gradle/README.md) | Gradle plugin for building Carnival applications and libraries.




### Extensions

Name | Description
--- | ---
[carnival-openspecimen](https://github.com/carnival-data/carnival-openspecimen) | Extension of carnival-core that implements vines for interfacing with [OpenSpecimen](https://www.openspecimen.org/) inventory management system.

## <a name="contribution-guide"></a> Contribution Guide
Carnival is an open source project and welcomes contributions! Here are some ways to help:

* Writing and improving the documentation
* Reporting or fixing bugs
* Contributing features or enhancements
* Creating carnival extensions for more domains
* Creating demonstration projects or tutorials



