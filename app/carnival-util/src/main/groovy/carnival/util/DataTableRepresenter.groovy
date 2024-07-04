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
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.DumperOptions.FlowStyle
import org.yaml.snakeyaml.DumperOptions



/**
* Used for snakeyaml
* Exclude metaClass property
* Parse GStringImpl under custom tag !GStringImpl
*
* @see https://bitbucket.org/asomov/snakeyaml/wiki/Howto#markdown-header-how-to-skip-a-javabean-property
*/
public class DataTableRepresenter extends Representer {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** Log to use */
    static Logger log = LoggerFactory.getLogger(DataTableRepresenter)


    /**
     * No argument constructor.
     */
    public DataTableRepresenter() {
        super(new DumperOptions())
        
        this.representers.put(
            org.codehaus.groovy.runtime.GStringImpl.class, 
            new RepresentGString()
        )

        def stringRep = this.representers.get(String)
        assert stringRep
        this.representers.put(
            org.codehaus.groovy.runtime.GStringImpl.class, 
            stringRep
        )

        /*
        def representer = new Representer() {{
            this.multiRepresenters.put(
                GString, 
                this.representers.get(String))   
        }}
        */
    }


    /**
     * Class to represent a GString in yaml.
     *
     */
    private class RepresentGString implements Represent {

        /**
         * Represent a GString as a snakeyaml Node.
         *
         */
        public org.yaml.snakeyaml.nodes.Node representData(Object data) {
            //log.trace "RepresentGString.representData $data"
            /*
            String tag = "tag:yaml.org,2002:str"            
            Character style = null
            String value = data.toString()
            if (BINARY_PATTERN.matcher(value).find()) {
                tag = "tag:yaml.org,2002:binary"
                char[] binary
                binary = Base64Coder.encode(value.getBytes())
                value = String.valueOf(binary)
                style = '|'
            }
            return representScalar(new Tag(tag), value, style)
            */

            String tag = "!GStringImpl"
            def args = [values:data.values, strings:data.strings]
            return representMapping(new Tag(tag), args, FlowStyle.FLOW)
        }
    }


    /**
     * Override representJavaBeanProperty to:
     *    Ignore properties that should not be written to yaml.
     *    Catch exceptions and log them.
     *
     */
    @Override
    protected NodeTuple representJavaBeanProperty(
        Object javaBean, 
        Property property,
        Object propertyValue, 
        Tag customTag
    ) {
        //log.trace "DataTableRepresenter.representJavaBeanProperty $javaBean $property"
        
        def ignoreProps = ['metaClass', 'statusUpdateBroadcast', 'superProcess', 'queryProcess', 'writtenTo']
        if (ignoreProps.contains(property.getName())) return null

        def representation
        try {
            representation = super.representJavaBeanProperty(
                javaBean, 
                property, 
                propertyValue,
                customTag
            )
        } catch (org.yaml.snakeyaml.error.YAMLException e) {
            def msg = "representJavaBeanProperty failure javaBean:$javaBean property:$property propertyValue:$propertyValue customTag:$customTag"
            log.error(msg, e)
        }
        
        return representation
    }
}


