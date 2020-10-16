package carnival.util


class DataTableFiles {

    static DataTableFiles create(File dir, String name) {
        File meta = DataTable.metaFile(dir, name)
        File data = DataTable.dataFile(dir, name)
        new DataTableFiles(meta:meta, data:data)
    }

    File meta
    File data

    boolean areNull() { 
        meta == null || data == null 
    }
    
    boolean exist() { 
        if (areNull()) return false
        meta.exists() && data.exists()
    }

    boolean areFiles() {
        if (areNull()) return false
        if (!exist()) return false
        meta.isFile() && data.isFile()
    }

    boolean areReadable() {
        if (areNull()) return false
        if (!exist()) return false
        meta.canRead() && data.canRead()
    }

    Map<String,File> toMap() {
        [meta:meta, data:data]
    }

    List<File> toList() {
        [meta, data]
    }

    void delete() {
        if (meta != null) meta.delete()
        if (data != null) data.delete()
    }

    Object each(Closure cl) {
        [meta, data].each(cl)
    }

}