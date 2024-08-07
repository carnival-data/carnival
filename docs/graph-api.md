# Graph API

Carnival defines a graph API layered over the standard [TinkerPop API](https://tinkerpop.apache.org/docs/current/reference/) whose goal is to provide a more semantic approach to property graph operations.  Graph objects (vertices, edges, and properties) are modelled in Carnival with enums to which traits have been applied (see [carnival graph modeling](graph-model.md) for details on how to define a graph model).  The graph API provides ways to interact with the graph using the model. For example, EdgeDefTrait provides methods to work with defined traits including a hook to an EdgeBuilder class that attaches logic to the creation of edges.  There are analogous classes for the creation and manipulation of Vertices.

The underlying TinkerPop classes, including Vertex and Edge, are always available.  A Carnival graph is just a property graph.  The facilities of the graph database engine and the full Tinkerpop API can be used to operate over the graph.  However, in order to keep the graph properly formatted, using the Carnival API is recommended.

Basic knowledge of the Tinkerpop API will be helpful, and introduction can be found [here](https://tinkerpop.apache.org/docs/current/tutorials/getting-started/).

## Contents
- [Vertices](#vertices)
- [Edges](#edges)
- [Gremlin Extensions](#gremlin)

## <a name="vertices">Vertices</a>

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

#### Vertex 

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

#### Model
```groovy
@VertexDefinition
static enum VX {
    SCHOOL (
        propertyDefs:[
            PX.NAME.withConstraints(index:true),
        ]
    ),
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

@EdgeDefinition
static enum EX {
    ATTENDS(
        domain:[VX.PERSON],
        range:[VX.SCHOOL]
    )
}
```

#### Graph Builders
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
3. Look-up or create the vertex representing "School 1"
4. The `ensure()` method requires a graph traversal source to use to search for pre-existing vertex
3. Connect the student with the school via a relationship.  Since the person vertex is new, we know there is no pre-existing relationship.  The `EdgeBuilder create()` method does not require any parameters.

Please note that if there are already multiple duplicate vertices in the graph, `ensure()` will not throw an error.  Rather, it will terminate without creating an additional vertex.

There are similar uses for edges between vertices that express relationships.  Onlye a single `ATTENDS` edge between `p1` and `s1` is necessary to express the relationship.  There is an `ensure` method on edges to prevent the creation of multiple edges, which can needlessly complicate graph traversals.

See the following for more details:

- [VertexDefTrait](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/VertexDefTrait.html)
- [VertexBuilder](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/VertexBuilder.html)
- [WithPropertyDefsTrait](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/WithPropertyDefsTrait.html)


## <a name="edges">Edges</a>

### Creating an edge

As with vertices, edges are created by going through the defined graph model.  Given the following model:

```groovy
@VertexDefinition
static enum VX {
    SCHOOL (
        propertyDefs:[
            PX.NAME.withConstraints(index:true),
        ]
    ),
    PERSON (
        propertyDefs:[
            PX.NAME.withConstraints(index:true),
        ]
    )    
}

@EdgeDefinition
static enum EX {
    ATTENDS(
        domain:[VX.PERSON],
        range:[VX.SCHOOL],
        propertyDefs:[
            PX.FULL_TIME
        ]
    )
}

@PropertyDefinition
static enum PX {
    FULL_TIME
}
```

edges can be created in the following ways.

```groovy
Vertex p1 = VX.PERSON.instance().create(graph)
Vertex s1 = VX.SCHOOL.instance().create(graph)

EX.ATTENDS.instance()        (1)
    .from(p1)                (2)
    .to(s1)                  (3)
    .withProperty(           (4)
        PX.FULL_TIME, true  
    )
.create()                    (5)
```
1. The `instance()` method returns an `EdgeBuilder`
2. The `from()` method accepts a vertex. If there is a domain set for the edge, validation is applied.
3. The `to` method accepts a vertes.  If there is a range set for the edge, validation is applied.
4. Set the `FULL_TIME` property of the edge to `true`
5. The `create` method of `EdgeBuilder` does not require any parameters.


### Singleton Edges

Singleton edges are useful to express semantic relationships.  In an RDF Triple Store, repeated statement invocations do not result in duplicate data.  For example, saving the RDF triple `'student1 :attends school1'` multiple times does not result in multiple entries in the triple store.  There is only the single fact `'student1 :attends school1'` in the database, regardless of how many times it is asserted.  This is one of the features of RDF Triple Stores that makes them useful as knowledge bases.

Property graphs can have multiple duplicate graphs between vertices.  In the example above, repeated calls to:

```groovy
EX.ATTENDS.instance().from(p1).to(s1).create()
```

would result in multiple duplicate edges between the vertices `p1` and `s1`. 

```mermaid
graph TD
    A[p1] -->|ATTENDS| B(s1)
    A[p1] -->|ATTENDS| B(s1)
```

This behavior is likely not desired in a knowledge graph.  To overcome this difficulty, Carnival includes an `ensure()` method on `EdgeBuilder` analogous to the `ensure()` method of `VertexBuilder`.

```groovy
EX.ATTENDS.instance().from(p1).to(s1).ensure(g)  (1)
```
1. The `ensure()` method will check for the existence of the edge and create it only if it is not present.  A graph traversal source is required to check for a pre-existing edge.

Please note that if there are already multiple duplicate edges, the `ensure()` method will not throw an error.  Rather, it will return without creating an additional edge.

See the following for more details:

- [EdgeDefTrait](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/EdgeDefTrait.html)
- [EdgeBuilder](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/EdgeBuilder.html)
- [WithPropertyDefsTrait](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/WithPropertyDefsTrait.html)


## <a name="gremlin">Gremlin Extensions</a>

Carnival implements a number of extensions to the Tinkerpop Gremlin graph traversal language that enable the use of Carnival objects in Gremlin treversals.


### in, out, both
The in(), out(), and both() traversal steps have been extended to work with Carnival graph model objects.  The following example demonstrates out().  in() and both() honor the logic of the corresponding Gremlin traversals.

```groovy
@VertexDefinition
static enum VX { THING }

@EdgeDefinition
static enum EX { IS_NOT }

def v1 = VX.THING.instance().create(graph, g)
def v2 = VX.THING.instance().create(graph, g)
EX.IS_NOT.instance().from(v1).to(v2).create()

g.V(v1).out(EX.IS_NOT).tryNext().isPresent()
g.V(v1).out(EX.IS_NOT).next() == v2
!g.V(v2).out(EX.IS_NOT).tryNext().isPresent()
```


### isa

`isa` matches against vertices or edges of a given definition.

```groovy
// all VX.PERSON vertices
g.V().isa(VX.PERSON).toList()

// all EX.ATTENDS edges
g.V().bothE().isa(EX.ATTENDS).toList()
```


### has, hasNot

The has() and hasNot() traversal steps have been extended to work with Carnival property definitions and enums.

```groovy
@VertexDefinition
static enum VX {
    THING(
        vertexProperties:[PX.ID]
    )
}

@PropertyDefinition
static enum PX { ID }

static enum LOCAL_ID { ID1 }
```

#### has
```groovy
def v1 = VX.THING.instance().withProperty(PX.ID, LOCAL_ID.ID1).create(graph, g)
g.V(v1).has(PX.ID).tryNext().isPresent()
```

```groovy
def v1 = VX.THING.instance().withProperty(PX.ID, 'someval').create(graph)
g.V(v1).has(PX.ID, 'someval').tryNext().isPresent()
```

```groovy
def v1 = VX.THING.instance().withProperty(PX.ID, LOCAL_ID.ID1).create(graph)
g.V(v1).has(PX.ID, LOCAL_ID.ID1).tryNext().isPresent()
```

#### hasNot
```groovy
def v1 = VX.THING.instance().withProperty(PX.ID, LOCAL_ID.ID1).create(graph)
def v2 = VX.THING.instance().create(graph)
!g.V(v1).hasNot(PX.ID).tryNext().isPresent()
g.V(v2).hasNot(PX.ID).tryNext().isPresent() 
```

### matchesOn

matchesOn() is a traversal step that matches on property values.  If the property is present, matches must have the property with the same value.  If the property is not present, matches must not have the property -- a null value will not match.

```groovy
@VertexDefinition
static enum VX {
    THING(
        vertexProperties:[PX.ID]
    )
}

def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
def v2 = VX.THING.instance().withProperty(PX.ID, '59').create(graph)
def v3 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
def v4 = VX.THING.instance().create(graph)

def matches = g.V().matchesOn(PX.ID, v1).toList()

matches.size() == 2
matches.contains(v1)
matches.contains(v3)
```

```groovy
@VertexDefinition
static enum VX {
    THING(
        vertexProperties:[PX.ID, PX.NAME]
)
}

def v1 = VX.THING.instance().withProperty(PX.ID, '58').create(graph)
def v2 = VX.THING.instance().withProperty(PX.ID, '59').create(graph)
def v3 = VX.THING.instance().withProperty(PX.NAME, '58').create(graph)
def v4 = VX.THING.instance().create(graph)

def matches = g.V().matchesOn(PX.NAME, v1, PX.ID).toList()

matches.size() == 1
matches.contains(v3)
```

See the following for more details:

- [Groovy Extension Modules](https://groovy-lang.org/metaprogramming.html#_extension_modules)
- [TinkerpopTraversalExtension](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/ext/TinkerpopTraversalExtension.html)
- [TinkerpopAnonTraversalExtension](https://carnival-data.github.io/carnival/groovydoc/carnival/graph/ext/TinkerpopAnonTraversalExtension.html)

