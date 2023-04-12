package carnival.util



/**
 * An encasulation of the files that can persist a data table. 
 *
 */
class DataTableFiles {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Create the data table files in the provided directory for the provided
     * name.
     * @param name The name of the data table
     * @param dir The target directory
     * @return The data table files
     */
    static DataTableFiles create(File dir, String name) {
        File meta = DataTable.metaFile(dir, name)
        File data = DataTable.dataFile(dir, name)
        new DataTableFiles(meta:meta, data:data)
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** The metadata file */
    File meta

    /** The data file */
    File data


    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return true if any of the data table files are null.
     * @return True if any of the data table files are null.
     */
    boolean areNull() { 
        meta == null || data == null 
    }
    

    /**
     * Return true if all data files exist.
     * @return True if all data files exist
     */
    boolean exist() { 
        if (areNull()) return false
        meta.exists() && data.exists()
    }


    /**
     * Return true if all the data table files exist and are files.
     * @return True if all the data table files exist and are files
     */
    boolean areFiles() {
        if (areNull()) return false
        if (!exist()) return false
        meta.isFile() && data.isFile()
    }


    /**
     * Return true if all the data table files exist and are readable.
     * @return True if all the data table files exist and are readable
     */
    boolean areReadable() {
        if (areNull()) return false
        if (!exist()) return false
        meta.canRead() && data.canRead()
    }


    /**
     * Return the data table files in a map.
     * @return The data table files in a map
     */
    Map<String,File> toMap() {
        [meta:meta, data:data]
    }


    /**
     * Return the data table file in a list.
     * @return The data table files in a list
     */
    List<File> toList() {
        [meta, data]
    }


    /**
     * Delete the data table files from the file system.
     */
    void delete() {
        if (meta != null) meta.delete()
        if (data != null) data.delete()
    }


}