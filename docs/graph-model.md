# Graph Model

Fundamental to Carnival is the ability to model graph elements. Vertices, edges, and properties can all be modelled.

_Example: Vertex definition_

```groovy
@VertexDefinition
static enum VX {
    PERSON (
        propertyDefs:[
            PX.IS_ALIVE.withConstraints(index:true),
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
- The `PERSON` vertex has one allowed property.  IS_ALIVE is not required to be present nor to be unique, but will be indexed for quicker searching.

Given the above vertex definition, we can create a vertex as follows:

```groovy
Vertex person1 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)
```

- person1 is a variable that references a Vertex with label Person and two properties, nameSpace: VX and isAlive: true.

The name space of a vertex will be prepended by the package and name of the enclosing class, eg. my.package.TheClass$VX.

_Example: Edge definition_

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

## Vertex Labels
The Carnival graph model will set the label of vertices based on the verte definition.  Vertex definitions are expected to be in snake case.  The corresponding label used in the graph will be camel case.

Vertex Definition | Vertex Label
--- | ---
PERSON | Person
PERSON\_WITH\_HAT | PersonWithHat

## Name Spaces
Vertex definitions in Carnival are name-spaced.  When a vertex is created using the Carnival graph model, a `Base.PX.NAME_SPACE` property will be added as a vertex property.  The name space will be computed based on the package, class, and enum name of the vertex definition. 

For example, given a definition of:

```groovy
@VertexDefinition
static enum VX {
    PERSON
}
```
in a class named `AwesomeClass` with package `my.fun.package`, the `Base.PX.NAME_SPACE` would be `my.fun.package.AwesomeClass$VX`.

Namespaces allow disparate graph models to be represented in the same graph.  However, vertex labels are not currently scoped by namespace.  So, vertices created from `VX.PERSON` vertex definitions defined in different classes will share the `Person` label in the graph.  If the a person means something very different across definitions, the result could be a confusing graph.  Future implementation of Carnival will likely introduce a feature to automatically scope vertex labels so each definition of `Person` gets its own vertex label.

## Vertex Properties
By default, properties must be defined on vertices.

```groovy
@VertexDefinition
static enum VX {
    PERSON (
        propertyDefs:[
            PX.NAME.withConstraints(required:true, index:true, unique:true),
            PX.IS_ALIVE.withConstraints(index:true),
            NOTES
        ]
    ),
    THING_WITH_ANY_PROPERTIES(propertiesMustBeDefined:false,
    THING_WITH_NO_PROPERTIES,
}
```
- The `PERSON` vertex has three allowed properties.  NAME is required, must be unique, and will be indexed by the database engine.  IS_ALIVE is not required to be present nor to be unique, but will be indexed for quicker searching.  NOTES has no constraints and will not be indexed.
- The `THING_WITH_ANY_PROPERTIES` vertex can have any properties due to the argument `propertiesMustBeDefined:false'.  By default, all vertex properties must be defined.
- The `THING_WITH_NO_PROPERTIES` vertex has no defined properties and therefore will accept none.

## Classes
Vertices can be used to represent class structures, which can be useful for data representation.  

```groovy
@VertexDefinition
static enum VX {
    
    // isClass tells Carnival CLASS_OF_DOGS is a class
    CLASS_OF_ALL_DOGS (
        isClass:true
    ),
    
    // by convention, a definition that ends in _CLASS is considered a class
    COLLIE_CLASS (
        
        // set the superlass of COLLIE_CLASS
        superClass: CLASS_OF_ALL_DOGS
    ),

    SHIBA_INU_CLASS (
        superClass: CLASS_OF_ALL_DOGS
    ),
}
```
- When this model is instanted in the graph, a singleton vertex will be created for `COLLIE_CLASS` and `CLASS_OF_ALL_DOGS` with a `Base.EX.IS_SUBCLASS_OF` relationship between them.

There is no special handling of "class" vertices beyond what is described here.  Representing a class structure in a graph is useful for computation and searching.  Given the above graph model, it woudl be straightforward to find all the shiba inus and collies even though their vertex labels do not denote that they are both dogs.  


## Instances
The `instanceOf` property of a vertex definition can be used to declare the class of an instance vertex.

```groovy
@VertexDefinition
static enum VX {    
    SHIBA_INU_CLASS,
    SHIBA_INU (
        instanceOf:VX. SHIBA_INU_CLASS
    )
}

def rover = VX.SHIBA_INU.instance().create(graph)
```
- A `Base.EX.IS_INSTANCE_OF` edge will be created from `rover` to the vertex representing `VX.SHIBA_INU_CLASS`.

