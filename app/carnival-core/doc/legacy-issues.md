# Carnival Legacy Issues

## Overview
Carnival legacy issues are found in the package *carnival.issue*.  They were the original scripts written in Carnival to produce results for specific Github issues. They do not use the graph.  Some have been generalized and made configurable to facilitate re-use.

## ICD By Date
```
# icd by date

# a name to attach to report files, etc.
name: icdByDateTest

# the icd code groups
codes:

    # the format of the code file
    # there are two formats supported, 1 and 2
    format: 2

    # the full path to the code file
    file: /Users/augustearth/Desktop/test.xlsx

    # optional overrides of column names
    columns:
    
        # the optional name of the group column, defaults to group_name
        group_name: Group

        # the optional name of the code ref column, defaults to code_ref
        code_ref: ICD Code(s)

# the date comparator to use to find the encounter(s) of interest
# one of: 
#   LT, LTEQ, EQ, GT, GTEQ, ALL, 
#   LT_MOSTRECENT, LTEQ_MOSTRECENT, EQ_MOSTRECENT, GT_MOSTRECENT, GTEQ_MOSTRECENT, 
#   ALL_MOSTRECENT, ALL_CLOSEST
# see SqlUtils.groovy
single_value_date_comparator: ALL_CLOSEST

# true to attempt to load the icd feature from a pre-existing file
load_icd_feature_from_target: false

# the icd features to include or exclude
icd_features:

    # include ONLY these icd features
    include:
        - enc_date

# optional features to attach to each row of the report
optional_features:

    # patient demographics
    - demo

    # body mass index
    - bmi    

```

## ICD Ever Never Basic

### Basic Configuration

```
# icd ever never

# a name to attach to report files, etc.
name: icdEverNeverTest

# the icd code groups
codes:

    # the format of the code file
    # there are two formats supported, 1 and 2
    format: 2

    # the full path to the code file
    file: /Users/augustearth/Desktop/test.xlsx

    # optional overrides of column names
    columns:
    
        # the optional name of the group column, defaults to group_name
        group_name: Group

        # the optional name of the code ref column, defaults to code_ref
        code_ref: ICD Code(s)

# optional features to attach to each row of the report
optional_features:

  # patient demographics
    - demo
```


## ICD Code Group Files

### Format 1
Group names can repeat.  Only * is accepted as a wildcard character.

```
group_name, code_ref
a         , 1.1*
a         , 2.12
b         , 3*
```

### Format 2
Group names do not repeat.  * or % is accepted as wildcard character.  Code refs are separated by 'white-space or white-space'

```
group_name, code_ref
a         , 1.1* OR 1.2 OR 5.8*
b         , 3% OR 2.4 OR 5% 
```