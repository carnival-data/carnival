package carnival.vine



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

import carnival.util.CoreUtil



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

    /**
     * Encompasses the meta or descriptor data for a vine method call.
     *
     */
    @JsonPropertyOrder(["thisClass", "vineMethodClass", "resultClass", "arguments"])
    static class Meta {

        /** class of the "this" object */
        Class thisClass

        /** class of the vine method */
        Class vineMethodClass

        /** class of the result */
        Class resultClass

        /** map of arguments */
        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        Map arguments

        /**
         * Create a Meta object from a JSON string.
         * @param json The JSON text in string form.
         * @return The Meta object.
         */
        static public Meta createFromJson(String json) {
            ObjectMapper mapper = new ObjectMapper();
            Meta mcm = mapper.readValue(json, Meta);
            mcm
        }
    }

    /**
     * Encompasses the result of a vine method call.
     *
     */
    static class Result {

        /** value of the result */
        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        Object value

        /**
         * Create a result object from a JSON string.
         * @param json The JSON text in string form.
         * @return The result object
         */
        static public Result createFromJson(String json) {
            ObjectMapper mapper = new ObjectMapper();
            Result out = mapper.readValue(json, Result);
            out
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS 
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Create a JsonVineMethodCall object from a JSON string.
     * @param json The JSON string.
     * @return The JsonVineMethodCall object.
     *
     */
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


    /**
     * Create a JsonVineMethodCall from a file.
     * @param file The file object to use as a source.
     * @return The JsonVineMethodCall object.
     */
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


    /**
     * Return the computed name for a vine method class and arguments combo.
     * @param vineMethodClass The vine method class
     * @param arguments The arguments supplied to the call
     * @return The computed name as a string
     *
     */
    static String computedName(Class vineMethodClass, Map arguments) {
        String name = CoreUtil.standardizedFileName(vineMethodClass)

        if (arguments != null && arguments.size() > 0) {
            String uniquifier = CoreUtil.argumentsUniquifier(arguments)
            name += "-${uniquifier}"
        }

        name += FILE_EXTENSION_SEPARATOR + FILE_EXTENSION_JSON

        return name
    }


    /**
     * Find the file in a directory for the given vine method class and args
     * returning null if not found.
     * @param dir The directory in which to look.
     * @param vineMethodClass The vine method class.
     * @param args Map of arguments for the call.
     * @return The file object if it exists, otherwise null.
     */
    static public File findFile(File dir, Class vineMethodClass, Map args) { 
        File file = file(dir, vineMethodClass, args)
        if (file.exists() && file.isFile()) return file
        else return null
    }


    /**
     * Find the file in a directory for the given vine method class and args
     * returning the file object whether or not the file exists.
     * @param dir The directory in which to look
     * @param vineMethodClass The vine method class
     * @param args Map of arguments for the call
     * @return The file object
     */
    static public File file(File dir, Class vineMethodClass, Map args) { 
        String fileName = computedName(vineMethodClass, args)
        new File(dir, fileName)
    }



    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** The default file extension separator to use in file names */
    static final String FILE_EXTENSION_SEPARATOR = '.'

    /** The default file extension to use for JSON file names */
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

    /** 
     * Return the result of this vine method call 
     * @return The reuslt object
     */
    public T getResult() {
        return this.result
    }

    /**
     * Set the result of the vine method call
     * @param result The result object
     */
    public void setResult(T result) {
        this.result = result
        this.resultClass = result.class
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - COMPUTED PROPERTIES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the computed name of this JSON vine method call
     * @return The computed name as a string
     */
    public String computedName() {
        computedName(this.vineMethodClass, this.arguments)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - JSON SERIALIZATION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return this object as a JSON string.
     * @return This object as as JSON string
     */
    public String toJson() { 
        ObjectMapper mapper = new ObjectMapper()
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter()
        String out = writer.writeValueAsString(this)
        out
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Write this object as one or more files in the given directory.
     * @param dir The directory in which to write the files.
     * @return The list of files written
     */
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

}
