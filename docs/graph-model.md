# Graph Model

Fundamental to Carnival is the ability to model graph elements, and the concepts in Carnival graph modeling are heavily influenced by the [Web Ontology Language (OWL)](https://www.w3.org/TR/2012/REC-owl2-primer-20121211/) specification. **Vertices**, **edges**, and **properties** can all be modelled, and certian validation constraints and class relationships can be specified.

## Example Scripts

File | Description
--- | ---
[graph-model-1.groovy](groovy/graph-model-1.groovy) | Graph validation
[graph-model-2.groovy](groovy/graph-model-2.groovy) | Name spaces
[graph-model-3.groovy](groovy/graph-model-3.groovy) | Instances and classes

## Overview
The graph model is specified by creating enums that are annotated with `@PropertyDefinition`, `@EdgeDefinition` or `@VertexDefinition`.  Once VertexDefinition and EdgeDefinition have been created they can be used to add new elements to the graph, for example:

```
Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).create()
```

The `instance()` method invokes a builder class (`EdgeBuilder` or `VertexBuilder`) that the following methods (in this example `from()`, `to()` and `create()`) act on. Some properties and edges to indicate Carnival concepts like the namespace of the element 
or superclass relationships will be automatically created in the graph.

See the API docs for the [carnival.graph package](https://carnival-data.github.io/carnival/groovydoc/index.html?carnival/graph/package-summary.html) for reference.

### Model File Locations
Models can be defined anywhere, as shown in the example scripts. However in a larger application, the convention is to either create a file named in `GraphModel.groovy` the main source directory or to create a subpackage named `model` that contains files with the model definitions.  

See [CoreGraph.initializeGremlinGraph()](https://github.com/carnival-data/carnival/blob/master/app/carnival-core/src/main/groovy/carnival/core/graph/CoreGraph.groovy).

### Annotation Processor
Carnival contains a [Groovy AST transformation](https://groovy-lang.org/metaprogramming.html#developing-ast-xforms) that scans the source tree for `@PropertyDefinition`, `@EdgeDefinition` or `@VertexDefinition` annotations.
Among other things, these transformation applies the traits `PropertyDefTrait`, `EdgeDefTrait`, or `VertexDefTrait` to the enums.  After the AST transformation, annotated enums become part of the Carnival machinery.

See [DefinitionTransformation](https://github.com/carnival-data/carnival/blob/master/app/carnival-graph/src/main/groovy/carnival/graph/DefinitionTransformation.groovy).

## Property Definitions
Both vertices and edges can contain properties.  The first step in graph modelling is to define the properties that will be used.  Property definitions are simple.  They enumerate the properties that will be used in the graph without any further descriptors of the properties.  In this version of Carnival, there is no concept of data type.

```groovy
@PropertyDefinition
static enum PX {
    IS_ALIVE
}
```
- `@PropertyDefinition` tells carnival that the `PX` enum class defines properties that can be used on vertices and edges.
- `PX` is an arbitrary name.
- `PX` defines a single property, `IS_ALIVE`.

As with vertex and edge definitions, anything can be chosen for the name of the enum.  `PX` is a convention for property definitions.  While any name can be chosen, keep in mind that the name will appear in your code whenever the properties are referenced.  So, keeping it terse will promote less verbose code.

When applied to a vertex or an edge, `IS_ALIVE` will be represented as a property labelled `isAlive`.  Carnival expects that properties will follow the Java convention of capital snake case and automatically translates them to camel case for use in the property graph.


## Vertex Definitions
Vertex definitions define the vertex labels that will be used in the graph, the properties that are valid for each label, and other properties associated with vertices.

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

- `@VertexDefinition` tells Carnival that `VX` is a vertex definition.
- There are no rules governing the naming of definition enums. `VX` is a convention for vertex definition enums.
- The `PERSON` vertex has one allowed property, `IS_ALIVE`, which is not required to be present nor to be unique, but will be indexed for quicker searching.



### Vertex Labels
The Carnival graph model will set the label of vertices based on the vertex definition.  Vertex definitions are expected to be in snake case.  The corresponding label used in the graph will be capital camel case.

Vertex Definition | Vertex Label
--- | ---
PERSON | Person
PERSON\_WITH\_HAT | PersonWithHat

### Name Spaces
Vertex definitions in Carnival are name-spaced.  When a vertex is created using the Carnival graph model, a `Base.PX.NAME_SPACE` property will be added as a vertex property.  The name space will be computed based on the package, class, and enum name of the vertex definition. 

For example, given a definition of:

```groovy
@VertexDefinition
static enum VX {
    PERSON
}
```
in a class named `AwesomeClass` with package `my.fun.package`, the `nameSpace` property of any `Person` would be `my.fun.package.AwesomeClass$VX`.

Namespaces allow disparate graph models to be represented in the same graph.  However, please note that property graph engines optimize on vertex label.  Vertices created from `VX.PERSON` vertex definitions defined in different classes will share the `Person` label in the graph.  If the a person means something very different across definitions, the result could be a confusing graph.  Future implementation of Carnival will likely introduce a feature to automatically scope vertex labels so each definition of `Person` gets its own unique vertex label.


### Vertex Properties
By default, properties must be defined on vertices and edges.

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
    THING_WITH_ANY_PROPERTIES(propertiesMustBeDefined:false),
    THING_WITH_NO_PROPERTIES,
}
```
- The `PERSON` vertex has three allowed properties.  NAME is required, must be unique, and will be indexed by the database engine.  `IS_ALIVE` is not required to be present nor to be unique, but will be indexed for quicker searching.  `NOTES` has no constraints and will not be indexed.
- A `THING_WITH_ANY_PROPERTIES` vertex can have any properties due to the argument `propertiesMustBeDefined:false`.  The default requirement that all vertex properties must be defined is removed.
- The `THING_WITH_NO_PROPERTIES` vertex has no defined properties and due to the default behavior will accept none.

### Class Vertices
Vertices can be used to represent class structures (similar to [OWL classes](https://en.wikipedia.org/wiki/Web_Ontology_Language#Classes)) in the graph. Classes can have sub/super class relationships between each other, and other verticies can be defined that are "instances" of that class.

```groovy
@VertexDefinition
static enum VX {
    // isClass tells Carnival CLASS_OF_DOGS is a class
    CLASS_OF_ALL_DOGS (
        isClass:true
    ),
    
    // Carnival assumes a definition that ends in _CLASS is a class
    COLLIE_CLASS (
        // set the superlass of COLLIE_CLASS
        superClass: CLASS_OF_ALL_DOGS
    ),

    SHIBA_INU_CLASS (
        superClass: CLASS_OF_ALL_DOGS
    ),
}
```
- When this model is instantiated in the graph, a singleton vertex will be created for `COLLIE_CLASS`, `CLASS_OF_ALL_DOGS`, and `SHIBA_INU_CLASS` 
- A `Base.EX.IS_SUBCLASS_OF` relationship will be instantiated between `CLASS_OF_ALL_DOGS` and the sub-classes `COLLIE_CLASS` and `SHIBA_INU_CLASS`.

There is no special handling of "class" vertices beyond what is described here.  Representing a class structure in a graph can be useful for computation and searching.  Given the above graph model, it would be straightforward to find all the shiba inus and collies even though their vertex labels do not denote that they are both dogs.  


### Instance Vertices
The `instanceOf` property of a vertex definition can be used to declare the class of an instance vertex.

```groovy
@VertexDefinition
static enum VX {    
    SHIBA_INU_CLASS,
    SHIBA_INU (
        instanceOf:VX.SHIBA_INU_CLASS
    )
}

Vertex rover = VX.SHIBA_INU.instance().create(graph)
```
- A `Base.EX.IS_INSTANCE_OF` edge will be created from `rover` to the vertex representing `VX.SHIBA_INU_CLASS`.


### Instantiating Vertices

Using the Carnival vertex builder, we can create a vertex as follows:

```groovy
Vertex person1 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)
```

- `person1` is a variable that references a Vertex with label `Person` and two properties, `nameSpace: ...$VX` and `isAlive: true`.

The name space of a vertex will be prepended by the package and name of the enclosing class, eg. `my.package.TheClass$VX`.


## Edge Definitions
Edge modelling includes the edge labels as well as the valid properties, domain, and range of edges.

```groovy
@EdgeDefinition
static enum EX {
    IS_FRIENDS_WITH(
        domain:[VX.PERSON],
        range:[VX.PERSON]
    ),
    propertyDefs:[
        PX.STRENGTH_OF_RELATIONSHIP.withConstraints(index:true)
    ]
}

@PropertyDefinition
static enum PX {
    STRENGTH_OF_RELATIONSHIP
}
```

- `@EdgeDefinition` tells Carnival that EX is an edge definition.
- `IS_FRIENDS_WITH` is an edge where the outgoing and incoming vertices must be `VX.PERSON` vertices
- `IS_FRIENDS_WITH` edges accept a single property, `PX.STRENGTH_OF_RELATIONSHIP`

### Instantiating Edges

Given the above, we can create a second person and a directional `IS_FRIENDS_WITH` edge as follows.

```groovy
Vertex person2 = VX.PERSON.instance().withProperty(PX.IS_ALIVE, true).create(graph)
Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).create()
```




