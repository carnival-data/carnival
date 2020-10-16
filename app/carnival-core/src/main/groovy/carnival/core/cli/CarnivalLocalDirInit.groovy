package carnival.core.cli



import carnival.util.Defaults



/**
 * Create the directory structure in $env.CARNIVAL_HOME
 *
 * Run:
 * gradle carnivalLocalDirInit
 */
class CarnivalLocalDirInit {

    /**
     * Create the directory structure in $env.CARNIVAL_HOME
     *
     */
    static void main(String[] args) {
        Defaults.initDirectories()
    }
}
