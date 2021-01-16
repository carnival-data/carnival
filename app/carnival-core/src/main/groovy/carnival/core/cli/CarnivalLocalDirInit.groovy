package carnival.core.cli



import carnival.util.Defaults



/**
 * Create the directory structure in the default location.
 *
 * Run:
 * gradle carnivalLocalDirInit
 */
class CarnivalLocalDirInit {

    /**
     * Create the directory structure in the default location.
     *
     */
    static void main(String[] args) {
        Defaults.initDirectories()
    }
}
