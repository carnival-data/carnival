package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import groovy.transform.EqualsAndHashCode
import groovy.transform.Synchronized

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.*
import org.yaml.snakeyaml.*
import org.yaml.snakeyaml.DumperOptions.FlowStyle
import org.yaml.snakeyaml.constructor.*



/**
 * Used for snakeyaml.
 * Construct GStringImpl from custom tag !GStringImpl
 *
 */
public class DataTableConstructor extends Constructor {


    /**
     * No argument constructor that adds the tag !GStringImpl to the interhal
     * list of yaml constructors.
     *
     */
    public DataTableConstructor() {
        this.yamlConstructors.put(new Tag("!GStringImpl"), new ConstructGStringImpl());
    }


    /**
     * Class to construct a GStringImpl from a snakeyaml node.
     *
     */
    private class ConstructGStringImpl extends AbstractConstruct {

        /**
         * Construct a GStringImpl from a snakeyaml node.
         *
         * @param node The snakeyaml node.
         * @return The GStringImpl.
         *
         */
        public Object construct(org.yaml.snakeyaml.nodes.Node node) {
            Map args =  constructMapping(node)
            java.lang.Object[] values = args.values
            java.lang.String[] strings = args.strings
            return new org.codehaus.groovy.runtime.GStringImpl(values, strings)
        }
    }
}


