[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/pennbiobank/pennai/carnival-public/master/LICENSE)
[![Carnival CI](https://github.com/pmbb-ibi/carnival/actions/workflows/carnival_ci.yml/badge.svg)](https://github.com/pmbb-ibi/carnival/actions/workflows/carnival_ci.yml)
<a href='https://coveralls.io/github/pmbb-ibi/carnival?branch=master'><img src='https://coveralls.io/repos/github/pmbb-ibi/carnival/badge.svg?branch=master' alt='Coverage Status' /></a>
[![Maven Central](https://img.shields.io/maven-central/v/io.github.carnival-data/carnival-core.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.carnival-data/carnival-core)

# Carnival

*It's a party of information!*

**Carnival** Carnival is an open source JVM data unification framework that allows for a large variety of extract, transform, and load (ETL), integration, and analysis tasks related to relational data and property graphs. Some key functionality includes aggregation of data from disparate sources into a unified property graph and tools to reason over and interact with graph data using bounded operations. Carnival includes an architecture for tracking the provenance of data and providing evidence chains for conclusions or reasoning made on those data.

While Carnival is a general purpose tool that is domain agnostic, there are extensions ([carnival-clinical](https://github.com/carnival-data/carnival-clinical), [carnival-openspecimen](https://github.com/carnival-data/carnival-openspecimen)) that provide models and algorithms specific to the clinical biobanking domain.

## External Resources

-   [Usage Website](https://carnival-data.github.io/carnival/)
-   [GroovyDoc API Documentation](https://carnival-data.github.io/carnival/groovydoc/index.html)
-   [Demonstration Project](https://github.com/carnival-data/carnival-micronaut)

## Contents
- [Overview](#overview)
- [Packages](#packages)
- [Contribution Guide](README.md#contribution-guide)

## <a name="overview"></a> Overview

Carnival has three principal components: a graph modeling architecture, a caching facility for aggregating data from disparate data sources, and a framework for implementing graph algorithms.  The graph modeling architecture is a layer over Java enumerations and Tinkerpop that allow a graph model to be modeled and consumed by Tinkerpop traversals.  The caching facility supports the aggregation and caching of data from relational database and JSON API sources.  The graph algorithm framework provides a structured way to define and execute algorithms that operate over the property graph.


## <a name="packages"></a> Packages

### Core Packages

Name | Description
--- | ---
carnival-core | Basic Carnival framework. Implements the basic Carnival framework classes (vines, reapers, reasonsers, etc). Defines the basic carnival graph schema (processes, databases). [Core model](https://github.com/pmbb-ibi/carnival/blob/master/app/carnival-core/src/main/groovy/carnival/core/graph/Core.groovy)
[carnival-graph](app/carnival-graph/README.md) | Framework for defining carnival graph schemas (vertex and edge definitions). Contains the basic vertex, edge, and property classes.
[carnival-util](app/carnival-util/README.md) | Standalone package that contains utility and helper classes such as data tables, reports, and SQL utilties, which are primarily used for dealing with relational data.
[carnival-gradle](app/carnival-gradle/README.md) | Gradle plugin for building Carnival applications and libraries.




### Extensions

Name | Description
--- | ---
[carnival-clinical](https://github.com/carnival-data/carnival-clinical) | Extension of carnival-core for clinical data and biobanking operations. Contains model extensions for concepts such as patients, patient cohorts and healthcare encounters. Implements algorithms for generating case-control patient cohorts. [Clinical Model Extension](https://github.com/carnival-data/carnival-clinical/blob/main/src/main/groovy/carnival/clinical/graph/Clinical.groovy)
[carnival-openspecimen](https://github.com/carnival-data/carnival-openspecimen) | Extension of carnival-core that implements vines for interfacing with [OpenSpecimen](https://www.openspecimen.org/) inventory management system.

## <a name="contribution-guide"></a> Contribution Guide
Carnival is an open source project and welcomes contributions! Here are some ways to help:

* Writing and improving the documentation
* Reporting or fixing bugs
* Contributing features or enhancements
* Creating carnival extensions for more domains
* Creating demonstration projects or tutorials



