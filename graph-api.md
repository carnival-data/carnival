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
Vertex v1 = VX.PERSON.instance().withProperties(  (1)
    PX.NAME, 'adam',
    PX.IS_ALIVE, true
).create(graph)
```

1. `withProperties` allows multiple properties to be set at once

### Singleton Vertices

To create a vertex only if a matching vertex does not already exist, use the `ensure()` method.  This can be useful for vertices that will be used throughout an application that are not closely tied to inputs.  For example, an application that models students in school might rely on singleton vertices to represent the schools that are known entities.  

```groovy
Vertex p1 = VX.PERSON.instance().withProperties(  (1)
    PX.NAME, 'adam',
    PX.IS_ALIVE, true
).create(graph)                                   (2)

Vertex s1 = VX.SCHOOL.instance().withProperties(  (3)
    PX.NAME, 'School 1'
).ensure(graph, g)                                (4)

EX.ATTENDS.instance().from(p1).to(s1).create()    (5)
```
1. Create a person vertex to represent the student
2. Pass in the graph where the vertex is to be created
3. Look-up or create the vertex representing School 1
4. The `ensure()` method requires a graph traversal source to use to search for pre-existing vertex
3. Connect the student with the school via a relationship.  Since the person vertex is new, we know there is no pre-existing relationship.  The `EdgeBuilder create()` method does not require any parameters.

There are similar uses for edges between vertices that express relationships.  Onlye a single `ATTENDS` edge between `p1` and `s1` is necessary to express the relationship.  There is an `ensure` method on edges to prevent the creation of multiple edges, which can needlessly complicate graph traversals.

See the following for more details:

- [VertexDefTrait](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/VertexDefTrait.html)
- [VertexBuilder](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/VertexBuilder.html)
- [WithPropertyDefsTrait](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/WithPropertyDefsTrait.html)
