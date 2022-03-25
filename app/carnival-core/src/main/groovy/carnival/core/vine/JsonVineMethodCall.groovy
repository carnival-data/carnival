package carnival.core.vine



import java.io.BufferedReader
import java.io.StringReader
import groovy.util.logging.Slf4j

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonPropertyOrder

import carnival.core.util.CoreUtil



/**
 * JsonVineMethodCall is a concretization of VineMethod deals in objects that 
 * are serialized as JSON in for caching purposes using the jackson library.
 *
 */
@Slf4j
@JsonPropertyOrder(["thisClass", "vineMethodClass", "resultClass", "arguments", "result"])
class JsonVineMethodCall<T> implements VineMethodCall<T> {

    ///////////////////////////////////////////////////////////////////////////
    // CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /* Not used. 
    static class Files {
        File data
        File meta
    }*/

    @JsonPropertyOrder(["thisClass", "vineMethodClass", "resultClass", "arguments"])
    static class Meta {
        Class thisClass
        Class vineMethodClass
        Class resultClass

        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        Map arguments

        static public Meta createFromJson(String json) {
            ObjectMapper mapper = new ObjectMapper();
            Meta mcm = mapper.readValue(json, Meta);
            mcm
        }
    }

    static class Result {
        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        Object value

        static public Result createFromJson(String json) {
            ObjectMapper mapper = new ObjectMapper();
            Result out = mapper.readValue(json, Result);
            out
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS 
    ///////////////////////////////////////////////////////////////////////////


    static public <E extends JsonVineMethodCall> E createFromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();

        BufferedReader bf = new BufferedReader(new StringReader(json))
        String thisClassStr
        String line = bf.readLine()
        while (line != null && thisClassStr == null) {
            def result = (line =~ /\s*"thisClass"\s+:\s+"(\S+)"/).findAll()
            if (result) {
                thisClassStr = result.first().last()
            }

            line = bf.readLine()
        }
        if (thisClassStr == null) {
            throw new ParseException("could not find call class element: thisClassStr")
        }

        Class callClass = Class.forName(thisClassStr)
        assert callClass
        JsonVineMethodCall mc = mapper.readValue(json, callClass)

        mc
    }


    /*static public <E extends JsonVineMethodCall> E createFromJson(String metaJson, resultJson) {
        JsonVineMethodCall mc = new JsonVineMethodCall()

        Meta meta = Meta.createFromJson(metaJson)
        mc.thisClass = meta.thisClass
        mc.vineMethodClass = meta.vineMethodClass
        mc.arguments = meta.arguments
        mc.resultClass = meta.resultClass

        Result resultWrapper = Result.createFromJson(resultJson)
        mc.result = resultWrapper.value

        mc
    }*/


    static public <E> JsonVineMethodCall<E> createFromFile(File file) { 
        assert file != null
        assert file.exists()
        assert file.isFile()
        assert file.length() > 0

        String json = file.text
        assert json != null
        assert json.length() > 0
        log.trace "json.length(): ${json.length()}"

        JsonVineMethodCall mc = createFromJson(json)
        assert mc != null

        return mc
    }

    static String computedName(Class vineMethodClass, Map arguments) {
        String name = CoreUtil.standardizedFileName(vineMethodClass)

        if (arguments != null && arguments.size() > 0) {
            String uniquifier = CoreUtil.argumentsUniquifier(arguments)
            name += "-${uniquifier}"
        }

        name += FILE_EXTENSION_SEPARATOR + FILE_EXTENSION_JSON

        return name
    }

    static public File findFile(File dir, Class vineMethodClass, Map args) { 
        File file = file(dir, vineMethodClass, args)
        if (file.exists() && file.isFile()) return file
        else return null
    }

    static public File file(File dir, Class vineMethodClass, Map args) { 
        String fileName = computedName(vineMethodClass, args)
        new File(dir, fileName)
    }



    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    static final String FILE_EXTENSION_SEPARATOR = '.'
    static final String FILE_EXTENSION_JSON = 'json'


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** class of this object **/
    Class thisClass = this.class

    /** the class of the vine method */
    Class vineMethodClass

    /** the arguments that were provided when calling the vine method */
    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    Map arguments

    /** the result returned by the call */
    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    T result

    /** the class of the result */
    Class resultClass


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - RESULT
    ///////////////////////////////////////////////////////////////////////////

    public T getResult() {
        return this.result
    }

    public void setResult(T result) {
        this.result = result
        this.resultClass = result.class
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - COMPUTED PROPERTIES
    ///////////////////////////////////////////////////////////////////////////

    public String computedName() {
        computedName(this.vineMethodClass, this.arguments)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - JSON SERIALIZATION
    ///////////////////////////////////////////////////////////////////////////

    /*public String metaJson() { 
        ObjectMapper mapper = new ObjectMapper()
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter()
        Meta meta = new Meta(
            thisClass: this.thisClass,
            vineMethodClass: this.vineMethodClass,
            arguments: this.arguments,
            resultClass: this.resultClass
        )
        String out = writer.writeValueAsString(meta)
        out
    }*/


    /*public String resultJson() { 
        ObjectMapper mapper = new ObjectMapper()
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter()
        Result resultWrapper = new Result(value: this.result)
        String out = writer.writeValueAsString(resultWrapper)
        out
    }*/


    public String toJson() { 
        ObjectMapper mapper = new ObjectMapper()
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter()
        String out = writer.writeValueAsString(this)
        out
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILES
    ///////////////////////////////////////////////////////////////////////////

    public List<File> writeFiles(File dir) { 
        assert dir != null
        assert dir.exists()
        assert dir.isDirectory()
        assert dir.canWrite()

        String destFileName = computedName()
        File destFile = new File(dir, destFileName)
        //assert destFile.canWrite() returns false even when file can be written

        String json = this.toJson()
        assert json != null
        assert json.length() > 0

        destFile.write(json)

        [destFile]
    }


    /*
    public Files writeFiles(File dir) { }
    public File writeMetaFile(File dir) { }
    public File writeDataFile(File dir) { }
    public File findMetaFile(File dir) { }
    public File findDataFile(File dir) { }
    */

}
