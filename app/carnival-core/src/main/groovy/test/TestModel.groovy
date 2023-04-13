package test



import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import carnival.graph.EdgeDefinition
import carnival.graph.PropertyDefinition
import carnival.graph.VertexDefinition
import carnival.core.Core


/** 
 * Model used in testing.
 */
class TestModel {

    /** Test vertex model */
    static enum VX implements VertexDefinition {

        /** Application */
        APPLICATION(
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true, index:true),
            ]
        ),

        /** Test thing */
        TEST_THING (
            vertexProperties:[
                Core.PX.NAME.withConstraints(required:true, index:true),
                PX.TEST_PROP
            ]
        )

        private VX() {}
        private VX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** Test edge model */
    static enum EX implements EdgeDefinition {

        /** Test relationship */
        TEST_RELATIONSHIP (
            domain:[VX.TEST_THING], 
            range:[VX.TEST_THING]
        )

        private EX() {}
        private EX(Map m) {m.each { k,v -> this."$k" = v } }
    }


    /** Test property model */
    static enum PX implements PropertyDefinition {
        /** Test prop */
        TEST_PROP
    }

}
