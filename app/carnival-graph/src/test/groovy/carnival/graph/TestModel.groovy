package test.carnival.graph



import carnival.graph.VertexDefinition
import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition



class TestModel {

    @VertexDefinition(global="true")
    static enum VXG {
        THING,
        THING_1(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        )
    }


    @VertexDefinition
    static enum VX {
        THING,

        THING_1(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        )
    }


    @EdgeDefinition
    static enum EX {
    	IS_NOT(
            domain:[VX.THING], 
            range:[VX.THING_1]            
        )
    }


    @PropertyDefinition
    static enum PX {
        PROP_A,
        PROP_B,
        PROP_C
    }

}

