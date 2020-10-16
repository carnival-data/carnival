---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
title: Data Tables
nav_order: 1
has_children: true
parent: Application Programmer Interface
---

# Data Tables

Carnival contains a DataTable class with two sub-classes, MappedDataTable and GenericDataTable.
MappedDataTable is like a database table with a primary key. The value of the primary key field of each row
is enforced to be unique within the scope of the data table. GenericDataTable is like a database table with
no primary key. For data interchange, it is safer to use MappedDataTable, assuming your data set has a
primary key. If there is no primary key, use GenericDataTable.
