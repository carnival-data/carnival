package carnival.vine



import java.io.BufferedReader
import java.io.StringReader
import groovy.util.logging.Slf4j


/**
 * VineMethodCall is a parameterized interface that represents a completed call
 * to a vine method.
 *
 */
interface VineMethodCall<T> {

    ///////////////////////////////////////////////////////////////////////////
    // METHODS - RESULT
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Get the result of this vine method call.
     * @return The result of the call.
     */
    T getResult()

    /**
     * Set the result of this vine method call.
     * @param result The result of the call
     */
    void setResult(T result)


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - COMPUTED PROPERTIES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return a computed name for the vine method call, which may be used to
     * compute a name for a cache file 
     */
    String computedName() 


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Write this object to one or more files in the provided directory.
     * @param dir The directory in which to write files.
     * @return A list of written files.
     */
    List<File> writeFiles(File dir)

}
