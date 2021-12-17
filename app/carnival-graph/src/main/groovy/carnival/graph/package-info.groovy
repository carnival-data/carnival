/**
* Contains the framework for defining property, vertex and edge definitons of a Carnival graph model.
* <p>
* The graph model is specified by creating enums that are annotated with &#64;PropertyDefinition, &#64;EdgeDefinition 
* or &#64;VertexDefinition. Among other things, these annotations apply the traits PropertyDefTrait, EdgeDefTrait, 
* or VertexDefTrait to the enums.
* <p>
* Once VertexDefinition and EdgeDefinition have been created they can be used to add new elements to the 
* graph, for example:
* {@code Edge edge1 = EX.IS_FRIENDS_WITH.instance().from(person1).to(person2).create()}.
* The 'instance()' method invokes a builder class (EdgeBuilder or ControlledInstance) that the following methods 
* (in this example 'from()', 'to()' and 'create()') act on.
* <p>
* Some properties and edges to indicate Carnival concepts like the namespace of the element 
* or superclass relationships will be automatically created in the graph.
* <p>
* Example of specifying verticies with class and instanceOf relationships:
* 
*  <pre>{@code
* &#64;VertexDefinition
* static enum VX {
*     CLASS_OF_ALL_DOGS (
*         isClass:true
*     ),
*     // Carnival assumes a definition that ends in _CLASS is a class
*     COLLIE_CLASS (
*         superClass: CLASS_OF_ALL_DOGS
*     ),
*     SHIBA_INU_CLASS (
*         superClass: CLASS_OF_ALL_DOGS
*     ),
*  &rbrace;
* &#64;VertexDefinition
* static enum VX {    
*     SHIBA_INU_CLASS,
*     SHIBA_INU (
*         instanceOf:VX.SHIBA_INU_CLASS
*     )
*  &rbrace;
* Vertex rover = VX.SHIBA_INU.instance().create(graph)
* }</pre>
* 
* <p>
* Example of specifying edges with properties:
*
* <pre>{@code 
* &#64;EdgeDefinition
* static enum EX {
*     IS_FRIENDS_WITH(
*         domain:[VX.PERSON],
*         range:[VX.PERSON]
*     ),
*     propertyDefs:[
*         PX.STRENGTH_OF_RELATIONSHIP.withConstraints(index:true)
*     ]
* &rbrace;
* &#64;PropertyDefinition
* static enum PX {
*     STRENGTH_OF_RELATIONSHIP
* &rbrace;
* 
* }</pre>
*/
package carnival.graph

//used for groovydoc; rest of the file is empty