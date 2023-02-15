package carnival.vine



import java.io.BufferedReader
import java.io.StringReader
import groovy.util.logging.Slf4j

import carnival.util.GenericDataTable
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles
import carnival.util.DataTable
import carnival.util.CoreUtil




/**
 * A partial concretization of VineMethodCall for vine methods that return data
 * tables.  
 *
 */
@Slf4j
abstract class DataTableVineMethodCall<T> implements VineMethodCall<T> {

    /**
     * Return the computed name for the provided vine method.
     * @see computedName(Class, Map)
     * @param vineMethod The vine method.
     * @return The computed name as a string.
     */
    static String computedName(DataTableVineMethod vineMethod) {
        assert vineMethod != null
        computedName(vineMethod.class, vineMethod.arguments)
    }


    /**
     * Return the computed name for the provided vine method and arguments.
     * @param vineMethod The vine method.
     * @param arguments A map of arguments.
     * @return The computed name as a string.
     */ 
    static String computedName(Class vineMethodClass, Map arguments) {
        String name = CoreUtil.standardizedFileName(vineMethodClass)

        if (arguments != null && arguments.size() > 0) {
            String uniquifier = CoreUtil.argumentsUniquifier(arguments)
            name += "-${uniquifier}"
        }

        return name
    }


    /** 
     * Find the files for the given vine method class and arguments in the
     * provided directory.
     * @see carnival.util.DataTable#findDataTableFiles(java.io.File, String)
     * @see computedName(Class, Map)
     * @param dir The directory in which to look.
     * @param vineMethodClass The vine method class.
     * @param args A map of arguments.
     * @return A DataTableFiles object.
     */
    static public DataTableFiles findFiles(File dir, Class vineMethodClass, Map args) {
        assert dir != null
        assert vineMethodClass != null
        assert args != null

        String name = computedName(vineMethodClass, args)
        DataTable.findDataTableFiles(dir, name)
    }    


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** class of this object **/
    Class thisClass = this.class

    /** the class of the vine method */
    Class vineMethodClass

    /** the arguments that were provided when calling the vine method */
    Map arguments

    /** log of files written */
    List<File> writtenTo = new ArrayList<File>()

    ///////////////////////////////////////////////////////////////////////////
    // METHODS - COMPUTED PROPERTIES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the computed name of this vine method call object.
     * @see computedName(Class, Map)
     * @return The computed name as a string.
     */
    public String computedName() {
        DataTableVineMethodCall.computedName(this.vineMethodClass, this.arguments)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Write the cache files for this vine method call in the provided
     * directory.
     * @see carnival.util.DataTable#writeFiles(java.io.file, Map)
     * @param dir The directory in which to write the files.
     * @return The list of files written.
     */
    public List<File> writeFiles(File dir) { 
        assert dir != null
        assert dir.exists()
        assert dir.isDirectory()
        assert dir.canWrite()

        List<File> files = this.result.writeFiles(dir)
        writtenTo.addAll(files)
        files
    }


    /**
     * Write the cache files for this vine method call in the provided
     * directory return a DataTableFiles object.
     * @see carniva.util.DataTable#writeDataTableFiles(java.io.File)
     * @param dir The directory in which to write the files.
     * @return A DataTableFiles for the files written.
     */
    public DataTableFiles writeDataTableFiles(File dir) { 
        assert dir != null
        assert dir.exists()
        assert dir.isDirectory()
        assert dir.canWrite()

        this.result.writeDataTableFiles(dir)
    }

}
