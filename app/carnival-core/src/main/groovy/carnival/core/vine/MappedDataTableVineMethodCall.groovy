package carnival.core.vine



import java.io.BufferedReader
import java.io.StringReader
import groovy.util.logging.Slf4j

import carnival.core.util.CoreUtil
import carnival.util.MappedDataTable
import carnival.util.DataTableFiles
import carnival.util.DataTable



@Slf4j
class MappedDataTableVineMethodCall implements VineMethodCall<MappedDataTable> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    static String computedName(MappedDataTableVineMethod vineMethod) {
        assert vineMethod != null
        computedName(vineMethod.class, vineMethod.arguments)
    }

    static String computedName(Class vineMethodClass, Map arguments) {
        String name = CoreUtil.standardizedFileName(vineMethodClass)

        if (arguments != null && arguments.size() > 0) {
            String str = String.valueOf(arguments)
            String uniquifier = CoreUtil.standardizedUniquifier(str)
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


    /** */
    static public MappedDataTableVineMethodCall createFromFiles(DataTableFiles cacheFiles) {
        MappedDataTable mdt = MappedDataTable.createFromFiles(cacheFiles)
        MappedDataTableVineMethodCall methodCall = new MappedDataTableVineMethodCall()
        methodCall.result = mdt
        methodCall.arguments = mdt.vine.arguments
        methodCall.vineMethodClass = Class.forName(mdt.vine.methodClass)
        methodCall
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

    /** the result returned by the call */
    MappedDataTable result

    /** log of files written */
    List<File> writtenTo = new ArrayList<File>()


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - RESULT
    ///////////////////////////////////////////////////////////////////////////

    public MappedDataTable getResult() {
        return this.result
    }

    public void setResult(MappedDataTable result) {
        this.result = result
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - COMPUTED PROPERTIES
    ///////////////////////////////////////////////////////////////////////////

    public String computedName() {
        MappedDataTableVineMethodCall.computedName(this.vineMethodClass, this.arguments)
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
