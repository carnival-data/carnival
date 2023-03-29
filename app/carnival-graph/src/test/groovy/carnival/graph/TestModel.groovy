package test.carnival.graph



import carnival.graph.VertexModel
import carnival.graph.EdgeModel
import carnival.graph.PropertyModel



class TestModel {

    @VertexModel(global="true")
    static enum VXG {
        THING,
        THING_1(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        )
    }


    @VertexModel
    static enum VX {
        THING,

        THING_1(
            vertexProperties:[
                PX.PROP_A.withConstraints(required:true),
                PX.PROP_B
            ]
        )
    }


    @EdgeModel
    static enum EX {
    	IS_NOT(
            domain:[VX.THING], 
            range:[VX.THING_1]            
        )
    }


    @PropertyModel
    static enum PX {
        PROP_A,
        PROP_B,
        PROP_C
    }

}

