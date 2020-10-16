---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
title: Application Programmer Interface
nav_order: 2
has_children: true
---

# Carnival Application Programmer Interface (API)

**Carnival** contains a variety of components to aid in data aggregation in a bounded property graph.

## Utility Classes

### Data Tables

**Carnival** has two implementations of data tables for tabular data. The **MappedDataTable** class supports data tables that have a unique primary key. The **GenericDataTable** class supports tabular that does not have a primary key.

## Vines

The term **vine** applies to classes that read data from source systems such as relational databases or REST APIs.

## Graph Model

**Carnival** provides an interface to define valid patterns of vertices, edges, and properties.
