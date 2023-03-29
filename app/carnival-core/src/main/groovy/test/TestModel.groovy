package test



import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.VertexDefinition
import carnival.core.Core


/** */
class TestModel {

    /** */
    static enum VX implements VertexDefinition {
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
    static enum EX implements EdgeDefinition {
        TEST_RELATIONSHIP (
            domain:[VX.TEST_THING], 
            range:[VX.TEST_THING]
        )

        private EX() {}
        private EX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** */
    static enum PX implements PropertyDefinition {
        TEST_PROP
    }

}
