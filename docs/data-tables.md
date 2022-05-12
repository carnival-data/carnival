# Data Tables

Carnival contains a DataTable class with two sub-classes, MappedDataTable and GenericDataTable.
MappedDataTable is like a database table with a primary key. The value of the primary key field of each row
is enforced to be unique within the scope of the data table. GenericDataTable is more like a spreadsheet. There is no key field. For data interchange, it is safer to use MappedDataTable, assuming your data set has a
primary key. If your data do not contain a primary key, use GenericDataTable.

The Carnival DataTable objects are analogous to DataFrame objects in the Pandas library.  They are used by the Carnival vine infrastructure to contain results of vine operations and to read and write those results to cache files.  While they are useful classes, there is no requirement that these classes be used in your application code in other areas.

## Standards

Carnival DataTable objects are opinionated in the way they store and reference data.  They are not meant to be completely generic, but rather to provide mechanisms to store data in a standardized way.  Methods of DataTable classes will transform input data to match these standards.  

```groovy
def mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id')  (1)
mdt.dataAdd(id:'Id1', v1:'v11')                                   (2)
```
1. The identifier field name of the mapped data table will be `ID`, not `id`, to match the convention of DataTable field names.
2. The key `id` will be transformed to `ID` to match the convention of DataTable field names. The `dataAdd()` method will transform `Id1` to `id1` to match the convention of DataTable identifier values.

These standars are included in DataTable objects to help ensure that immaterial anomalies in data do not affect data integrity.  Leading and trailing spaces and case mismatches are some of the most common immaterial anomalies to affect data.  Data systems that rely on these elements in field names and identifier values are brittle and prone to errors that are difficult to diagnose and rectify.  Carnival takes the opinionated stance that they should be avoided.

## Data Types

DataTable objects operate exclusively over sequences of characters or strings.  All data are cast as strings on the way into these objects including identifier values.

```groovy
def mdt = new MappedDataTable(name:'mdt-test', idFieldName:'id')
mdt.dataAdd(id:1, v1:11)                                           (1)
mdt.dataGet('1', 'v1') == '11'                                     (2)
```
1. The numeral 1 will be cast to the string '1'.  The numeral 11 will be cast to the string '11'.
2. The `dataGet()` requires the identifier value to be a string, in this case '1'.

## Formats

### Field Names

By default, field (column) names are trimmed of leading and trailing whitespace and transformed to upper case.  Examples might include: 'FIELD1', 'NAME', 'COUNT'.

### Identifier Values

Identifier values, such as the identifiers used in MappedDataTables are trimmed of leading and trailing whitespace and transformed to lower case.  Examples might include: 'id1', 'abc', '123'.

## Contents
- [Mapped Data Table](mapped-data-table.md)
- [Generic Data Table](generic-data-table.md)
