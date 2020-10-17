---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: page
title: Graph Model
nav_order: 3
has_children: false
parent: Application Programmer Interface
---

# Graph Model

Fundamental to Carnival is the ability to model graph elements. Vertices, edges, and properties can all be modelled.

_Example: Vertex definition_

```groovy
@VertexDefinition
static enum VX {
    PERSON (
        propertyDefs:[
            PX.IS_SPECIMEN_DONOR.withConstraints(index:true)
        ]
    )
}
```

-   `@VertexDefinition` tells Carnival that VX is a vertex definition.
-   There are no rules governing the naming of definition enums. `VX`, `EX`, and `PX` are merely conventions.
-   The `PERSON` vertex has a single allowed property, which is not required, nor must it be unique, but is indexed.
