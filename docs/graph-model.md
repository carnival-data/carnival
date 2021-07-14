# Graph Model

Fundamental to Carnival is the ability to model graph elements. Vertices, edges, and properties can all be modelled.

_Example: Vertex definition_

```groovy
@VertexDefinition
static enum VX {
    PERSON (
        propertyDefs:[
            PX.IS_ALIVE.withConstraints(index:true)
        ]
    )
}

@PropertyDefinition
static enum PX {
    IS_ALIVE
}
```

- `@VertexDefinition` tells Carnival that VX is a vertex definition.
- There are no rules governing the naming of definition enums. `VX`, `EX`, and `PX` are merely conventions.
- The `PERSON` vertex has a single allowed property, which is not required, nor must it be unique, but is indexed.

Given the above vertex definition, we can create a vertex as follows:

```groovy
Vertex person1 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)
```

- person1 is a variable that references a Vertex with label Person and two properties, nameSpace: VX and isAlive: true.

The name space of a vertex will be prepended by the package and name of the enclosing class, eg. my.package.TheClass$VX.

```groovy
@EdgeDefinition
static enum EX {
    IS_FRIENDS_WITH(
        domain:[VX.PERSON],
        range:[VX.PERSON]
    )
}
```

- `@EdgeDefinition` tells Carnival that EX is an edge definition.
- IS_FRIENDS_WITH is an edge where the outgoing and incoming vertices must be VX.PERSON vertices

Given the above, we can create a second person and a directional IS_FRIENDS_WITH edge as follows.

```groovy
Vertex person2 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)
Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).create()
```
