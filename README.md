[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/pennbiobank/pennai/carnival-public/master/LICENSE)
[![Carnival CI](https://github.com/pmbb-ibi/carnival/actions/workflows/carnival_ci.yml/badge.svg)](https://github.com/pmbb-ibi/carnival/actions/workflows/carnival_ci.yml)
<a href='https://coveralls.io/github/pmbb-ibi/carnival?branch=master'><img src='https://coveralls.io/repos/github/pmbb-ibi/carnival/badge.svg?branch=master' alt='Coverage Status' /></a>

# Carnival

*It's a party of information!*

**Carnival** Carnival is an open source JVM data unification framework that allows for a large variety of ETL and analysis tasks related to relational data and property graphs. Some key functionality includes aggregation of data from disparate sources into a unified property graph and tools to reason over and interact with graph data using bounded operations. Carnival includes a robust architecture for tracking the provenance of data and providing evidence chains for conclusions or reasoning made on those data.

While Carnival is a general purpose tool that is domain agnostic, and has [several](https://github.com/carnival-data/carnival-clinical) [extensions](https://github.com/carnival-data/carnival-openspecimen) that provide models and algorithms specific to the clinical biobanking domain.

-   [Website (including documentation)](https://carnival-data.github.io/carnival/)
-   [API Documentation](https://carnival-data.github.io/carnival/groovydoc/index.html)
-   [Demonstration Project](https://github.com/carnival-data/carnival-micronaut)
-   [Contribution Guide](README.md#contribution-guide)

## <a name="overview"></a> Overview

Carnival uses objects called _vines_ to connect to external data sources and _reapers_ encode the domain knowledge specific to that data source. Vines can connect to sources such as MySql or Oracle databases, RedCap projects, and CSV files. Some vine features include:

-   Parameterized SQL queries
-   Utilities to compose iterative SQL from lists of identifiers and codes
-   Caching of query results
-   Incremental caching of long running query result data
-   Monitor thread to estimate time to completion of long running queries
-   Automatic re-establishment of dropped connections
-   API layer for REDCap
-   H2 database wrapper for CSV data

Carnival’s property graph database:

-   Is inherently schema-less enabling the incorporation of new data without restructuring resident data
-   Follows data instantiation patterns built for computational efficiency and inspired by OBO Foundry ontologies
-   Has a query engine capable of executing queries of arbitrary complexity


## <a name="getting-started"></a> Getting Started

See [developer setup](https://carnival-data.github.io/carnival/#DeveloperSetup) for full documentation on how to set up a development environment, and a tutorial for getting started.

## <a name="package-overview"></a> Packages

### Core Packages

Name | Description
--- | ---
carnival-core | Basic carnival framework. Implements the basic carnival framework classes (vines, reapers, reasonsers, etc). Defines the basic carnival graph schema (processes, databases). [Core model](https://github.com/pmbb-ibi/carnival/blob/master/app/carnival-core/src/main/groovy/carnival/core/graph/Core.groovy)
[carnival-graph](app/carnival-graph/README.md) | Framework for defining carnival graph schemas (vertex and edge definitions). Contains the basic vertex, edge, and property classes.
[carnival-util](app/carnival-util/README.md) | Standalone package that contains utility and helper classes such as MappedDataTable, FeatureReport and SqlUtils. Primarily used for dealing with relational data.
[carnival-gradle](app/carnival-gradle/README.md) | Gradle plugin for building a Micronaut app that relies on Carnival.


### Carnvival Domain-Specific Extensions

Name | Description
--- | ---
[carnival-clinical](https://github.com/carnival-data/carnival-clinical) | Extension of carnival-core for clinical data and biobanking operations. Contains model extensions for concepts such as patients, patient cohorts and healthcare encounters. Implements algorithms for generating case-control patient cohorts. [Clinical Model Extension](https://github.com/carnival-data/carnival-clinical/blob/main/src/main/groovy/carnival/clinical/graph/Clinical.groovy)
[carnival-openspecimen](https://github.com/carnival-data/carnival-openspecimen) | Extension of carnival-core that implements vines for interfacing with [OpenSpecimen](https://www.openspecimen.org/) inventory management system.

## Contribution Guide
Carnival is an open source project and welcomes contributions! Here are some ways to help:

* Writing and improving the documentation
* Reporting or fixing bugs
* Contributing features or enhancements
* Creating carnival extensions for more domains
* Creating demonstration projects or tutorials



