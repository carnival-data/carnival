package carnival.util



import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes



/**
 * Static utility methods for dealing with files.
 */
class FilesUtil {

    /**
     * Delete all the files starting at the provided start path; if the path is
     * a file, delete the file; if the path is a directory, delete everything
     * in the directory and the directory itself.
     * @param start The path to delete
     * @return The starting path
     */
    static Path delete(Path start) {

        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                throws IOException
            {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }

        })

    }

}