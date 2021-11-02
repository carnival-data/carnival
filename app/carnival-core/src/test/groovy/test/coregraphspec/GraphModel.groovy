package test.coregraphspec



import carnival.graph.VertexDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.EdgeDefTrait
import carnival.core.graph.Core
import carnival.graph.VertexDefinition
import carnival.graph.EdgeDefinition




/**
 * Defined outside the carnival.* package structure so it will not be
 * initialized automatically by Carnival infastructure... should probably
 * change the automatic initialization.
 *
 */
class GraphModel {

    static enum VX implements VertexDefTrait {
        DOG_CLASS,
        COLLIE_CLASS (
            superClass:VX.DOG_CLASS
        ),
        DOG

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v }}
    }

    @VertexDefinition(global="true")
    static enum VXG {
        DOG_CLASS_GLOBAL
    }

    @EdgeDefinition
    static enum EX {
        BARKS_AT
    }


    /*
    static enum PX implements PropertyDefTrait {
    }

    static enum EX implements EdgeDefTrait {
    }
    */

}