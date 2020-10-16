package test



import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.graph.EdgeDefTrait
import carnival.graph.PropertyDefTrait
import carnival.graph.VertexDefTrait
import carnival.core.graph.Core


/** */
class TestModel {

    /** */
    static enum VX implements VertexDefTrait {
        APPLICATION(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true, index:true),
            ]
        ),
        TEST_THING (
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true, index:true),
                PX.TEST_PROP
            ]
        )

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** */
    static enum EX implements EdgeDefTrait {
        TEST_RELATIONSHIP (
            domain:[VX.TEST_THING], 
            range:[VX.TEST_THING]
        )

        private EX() {}
        private EX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** */
    static enum PX implements PropertyDefTrait {
        TEST_PROP
    }

}
