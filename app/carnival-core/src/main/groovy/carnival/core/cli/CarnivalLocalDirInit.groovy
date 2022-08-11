package carnival.core.cli



import carnival.core.config.Defaults



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
