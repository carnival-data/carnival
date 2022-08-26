package test.coregraphspec



import carnival.graph.VertexDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.EdgeDefinition
import carnival.core.Core
import carnival.graph.VertexModel
import carnival.graph.EdgeModel




/**
 * Defined outside the carnival.* package structure so it will not be
 * initialized automatically by Carnival infastructure... should probably
 * change the automatic initialization.
 *
 */
class GraphModel {

    static enum VX implements VertexDefinition {
        DOG_CLASS,
        COLLIE_CLASS (
            superClass:VX.DOG_CLASS
        ),
        DOG

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v }}
    }

    @VertexModel(global="true")
    static enum VXG {
        DOG_CLASS_GLOBAL
    }

    @EdgeModel
    static enum EX {
        BARKS_AT
    }


    /*
    static enum PX implements PropertyDefinition {
    }

    static enum EX implements EdgeDefinition {
    }
    */

}