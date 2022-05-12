# Data Tables

Carnival contains a DataTable class with two sub-classes, MappedDataTable and GenericDataTable.
MappedDataTable is like a database table with a primary key. The value of the primary key field of each row
is enforced to be unique within the scope of the data table. GenericDataTable is more like a spreadsheet. There is no key field. For data interchange, it is safer to use MappedDataTable, assuming your data set has a
primary key. If there is no primary key, use GenericDataTable.

## Contents
- [Mapped Data Table](mapped-data-table.md)
- [Generic Data Table](generic-data-table.md)
