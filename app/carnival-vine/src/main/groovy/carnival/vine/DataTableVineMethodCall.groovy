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

    static String computedName(DataTableVineMethod vineMethod) {
        assert vineMethod != null
        computedName(vineMethod.class, vineMethod.arguments)
    }

    static String computedName(Class vineMethodClass, Map arguments) {
        String name = CoreUtil.standardizedFileName(vineMethodClass)

        if (arguments != null && arguments.size() > 0) {
            String uniquifier = CoreUtil.argumentsUniquifier(arguments)
            name += "-${uniquifier}"
        }

        return name
    }


    /** */
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

    public String computedName() {
        DataTableVineMethodCall.computedName(this.vineMethodClass, this.arguments)
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILES
    ///////////////////////////////////////////////////////////////////////////

    public List<File> writeFiles(File dir) { 
        assert dir != null
        assert dir.exists()
        assert dir.isDirectory()
        assert dir.canWrite()

        List<File> files = this.result.writeFiles(dir)
        writtenTo.addAll(files)
        files
    }


    public DataTableFiles writeDataTableFiles(File dir) { 
        assert dir != null
        assert dir.exists()
        assert dir.isDirectory()
        assert dir.canWrite()

        this.result.writeDataTableFiles(dir)
    }

}
