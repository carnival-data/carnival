# Graph API

Carnival defines a graph API layered over the standard Tinkerpop API whose goal is to provide a more semantic approach to property graph operations.  Graph objects are modelled in Carnival with enums to which traits have been applied.  EdgeDefTrait provides methods to work with defined traits including a hook to an EdgeBuilder class that attaches logic to the creation of edges.  There are analogous classes for the creation and manipulation of Vertices.

The underlying Tinkerpop classes, including Vertex and Edge, are always available.  A Carnival graph is just a property graph.  The facilities of the graph database engine and the full Tinkerpop API can be used to operate over the graph.  However, in order to keep the graph properly formatted, using the Carnival API is recommended.

## Vertices

### Creating a vertex

Vertices are created by going through the defined graph model.  Given the following model:

```groovy
@VertexDefinition
static enum VX {
    PERSON (
        propertyDefs:[
            PX.NAME.withConstraints(index:true),
            PX.IS_ALIVE.withConstraints(index:true),
        ]
    )
}

@PropertyDefinition
static enum PX {
    IS_ALIVE
}
```

vertices can be created in the following ways.

#### Vertex with no properties

```groovy
Vertex v1 = VX.PERSON.instance().create(graph)
```

- Caling the `instance()` method on a vertex definition hooks into the vertex builder logic.
- The `create()` method instructs the builder to create the vertex in the supplied graph and return it.

#### Vertex with properties

```groovy
Vertex v1 = VX.PERSON.instance()     (1)
    .withProperty(PX.NAME, 'adam')   (2)
.create(graph)                       (3)
```

1. Caling the `instance()` method on a vertex definition hooks into the vertex builder logic.
2. Tell the vertex builder to create a vertex with the property `PX.IS_ALIVE` set to `true`.
3. The `create()` method instructs the builder to create the vertex in the supplied graph and return it.

```groovy
Vertex v1 = VX.PERSON.instance().withProperties( (1)
    PX.NAME, 'adam',
    PX.IS_ALIVE, true
).create(graph)
```

1. `withProperties` allows multiple properties to be set at once

