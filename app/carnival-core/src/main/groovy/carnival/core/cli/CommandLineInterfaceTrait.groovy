package carnival.core.cli



import java.nio.file.Path

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import org.reflections.Reflections

import carnival.core.vine.CachingVine
import carnival.core.vine.CachingVine.CacheMode



/** 
 * CommandLineInterfaceTrait contains fields and methods that facilitate the
 * the creation of a stand-alone command line application.
 *
 */
trait CommandLineInterfaceTrait {


	///////////////////////////////////////////////////////////////////////////
	// STATIC
	///////////////////////////////////////////////////////////////////////////

    /** error log */
	static Logger elog = LoggerFactory.getLogger('db-entity-report')

    /** application log */
    static Logger log = LoggerFactory.getLogger('carnival')


	///////////////////////////////////////////////////////////////////////////
	// FIELDS
	///////////////////////////////////////////////////////////////////////////

    /** for reading input from the command line */
    def stdin = System.in.newReader()

    /** all commands */
    List<Command> allCommands = new ArrayList<Command>()

    /** commands currently available */
    List<Command> availableCommands = new ArrayList<Command>()


	///////////////////////////////////////////////////////////////////////////
	// METHODS
	///////////////////////////////////////////////////////////////////////////
    
    /** 
     * Filter the commands in allCommands as per the configuration in conf such
     * that only the commands permitted by conf are returned.
     *
     */
    List<Command> filterAvailableCommands(Map conf, Collection<Command> allCommands) {
        def configuredTasks = conf.get('tasks')
        if (configuredTasks == null || configuredTasks.size() == 0)
            throw new RuntimeException('no configured tasks')

        List<Command> availableCommands = new ArrayList<Command>()
        if (configuredTasks.contains(Command.ALL_COMMANDS)) {
            availableCommands.addAll(allCommands)
        } else {
            configuredTasks.each { uuid ->
                def cmd = allCommands.find { uuid == it.uuid }
                if (cmd) availableCommands << cmd
            }
        }
        availableCommands.sort { a, b ->
            if (a.category == b.category) return a.prompt <=> b.prompt
            def cato = [CommandCategory.GRAPH_MODIFICATION, CommandCategory.REPORT, CommandCategory.STATUS]
            def ia = cato.indexOf(a.category)+1
            def ib = cato.indexOf(b.category)+1
            return ia - ib
        } 

        return availableCommands       
    }


    /** 
     * Try to run the commands identified by commandNames in order using the 
     * provided graph and graph traversal source.
     *
     */
    void tryRunCommands(List<String> commandNames, Graph graph, GraphTraversalSource g) {
        commandNames.each { mn ->
            log.trace "trying to run $mn"

            def cmd = allCommands.find { it.methodName == mn || it.class.simpleName == mn }
            if (cmd == null) {
                log.trace "could not find command $mn"
                return
            }

            tryRunCommand(cmd, graph, g)
        }
    }
    

    /** */
    void tryRunCommand(Command cmd, Graph graph, GraphTraversalSource g) {
        log.trace "tryRunCommand: ${cmd}"
        try {
            if (cmd.methodClosure != null) cmd.methodClosure(graph, g)
            else if (cmd.methodName != null) this."${cmd.methodName}"()
            else throw new RuntimeException("invalid command $cmd")
        } catch (Throwable t) {
            log.error "error calling ${cmd.methodName}", t
        }        
    }


    /** */
    Collection<Command> loadCommandInstances() {
        def thisClass = this.class
        log.debug "thisClass: $thisClass"

        def thisPackage = thisClass.package
        log.debug "thisPackage: $thisPackage ${thisPackage.name}"

        Reflections reflections = new Reflections(thisPackage.name)
        Set<Class<Command>> subTypes = reflections.getSubTypesOf(Command.class)
        log.debug "subTypes: $subTypes"

        Set<Command> commandInstances = new HashSet<Command>()
        subTypes.each { commandClass ->
            commandInstances << commandClass.newInstance()
        }

        return commandInstances
    }


    /** */
    Map<Integer,String> createStringOptMap(List<String> opts) {
        Map<Integer,String> optMap = [:]
        opts.eachWithIndex { str, idx ->
            optMap.put(idx+1, str)
        } 
        return optMap
    }


    /** */
    Map<Integer,Command> createCommandOptMap(List<Command> opts) {
        Map<Integer,Command> optMap = [:]
        opts.eachWithIndex { cmd, idx ->
            optMap.put(idx+1, cmd)
        } 
        return optMap
    }


    /** */
    Map<Integer,Vertex> createVertexOptMap(List<Vertex> opts, String propName) {
        def sortedOpts = opts.sort { it.property(propName).orElse("$it") }

        Map<Integer,Vertex> optMap = [:]
        sortedOpts.eachWithIndex { cmd, idx ->
            optMap.put(idx+1, cmd)
        } 
        return optMap
    }


    /** */
    CachingVine.CacheMode getCacheMode() {
        def cacheMode = CachingVine.CacheMode.OPTIONAL

        def confMode = conf.get('cache-mode')
        if (confMode) cacheMode = Enum.valueOf(CachingVine.CacheMode, confMode)

        return cacheMode
    }


    /** */
    Object withCacheMode(CacheMode mode, CachingVine vine, Closure cl) {
        def priorCacheMode = vine.cacheMode
        vine.cacheMode = mode
        def rval = cl(vine)
        vine.cacheMode = priorCacheMode
        rval
    }

    /** */
    String prompt(String text) {
        print "\n$text\n\n> "
        return stdin.readLine()?.trim()
    }


    /** */
    String prompt(String text, String defaultValue) {
        print "\n$text ($defaultValue)\n\n> "
        def val = stdin.readLine()?.trim()

        (val) ?: defaultValue
    }


    /** */
    void echo(String text, String lineHeader = '> ') {
        println "\n\n${lineHeader}${text}\n\n"
    }


    /** */
    Command findCommandForMethodName(String methodName) {
        allCommands.find { it.methodName == methodName }
    }


    /** */
    String promptForCommandName(String methodName) {
        findCommandForMethodName(methodName).prompt
    }


    /** */
    String promptAnyKey() {
        prompt("Hit RETURN to continue")
    }


    /** */
    void echoCmdPrompt(String methodName) {
        echo(promptForCommandName(methodName))
        //def cmd = allCommands.find { it.methodName == methodName }
        //if (cmd) echo(cmd.prompt)
    }


    /** */
    String menuStrings(List<String> opts, String prompt = null) {
        //log.trace "menuStrings(List<String> opts, String prompt = null)"

        if (!prompt) prompt = "CHOOSE AN OPTION:"
        if (!prompt.endsWith(':')) prompt += ":"
        
        println "\n${prompt}"

        Map<Integer,String> optMap = createStringOptMap(opts)

        optMap.each { k, v ->
            println "($k) $v"
        }

        def choice
        while (choice == null) {
            print "\n> "

            def r = stdin.readLine()?.trim()
            if (r.isInteger()) {
                def ri = Integer.valueOf(r)
                if (optMap.containsKey(ri)) choice = optMap.get(ri)
                else println("Your input of " + ri + " is not one of the choices. Please pick again.")
            }
            else println("Your input of " + r + " is not a valid integer. Please pick again.")
        }

        return choice
    }


    /** */
    Vertex menuVertices(List<Vertex> opts, String vertexPropName, String prompt = "") {
        if (!prompt) prompt = "CHOOSE AN OPTION:"
        if (!prompt.endsWith(':')) prompt += ":"
        
        println "\n${prompt}"

        Map<Integer,Vertex> optMap = createVertexOptMap(opts, vertexPropName)

        optMap.each { k, v ->
            def optstr = "($k) "
            optstr += v.property(vertexPropName).orElse("$v")
            println optstr
        }

        def choice
        while (choice == null) {
            print "\n> "

            def r = stdin.readLine()?.trim()
            if (r.isInteger()) {
                def ri = Integer.valueOf(r)
                if (optMap.containsKey(ri)) choice = optMap.get(ri)
            }
        }

        return choice
    }


    /** */
    List<Vertex> menuVerticesMultiple(List<Vertex> allOpts, String vertexPropName, String prompt = "") {
        if (!prompt) prompt = "CHOOSE AN OPTION:"
        if (!prompt.endsWith(':')) prompt += ":"
        
        println "\n${prompt}"

        List<Vertex> opts = allOpts.toList()
        List<Vertex> choices = new ArrayList<Vertex>()

        Map<Integer,Vertex> optMap = createVertexOptMap(opts, vertexPropName)
        final String DONE = 'DONE'

        def choice
        while (choice != DONE && optMap.size() > 0) {

            optMap.each { k, v ->
                def optstr = "($k) "
                optstr += v.property(vertexPropName).orElse("$v")
                println optstr
            }

            def doneIndex = optMap.max({it.key}).key + 1
            println "($doneIndex) $DONE"

            choice = null
            while (choice == null) {
                print "\n> "

                def r = stdin.readLine()?.trim()
                if (r.isInteger()) {
                    def ri = Integer.valueOf(r)
                    if (optMap.containsKey(ri)) choice = optMap.get(ri)
                    else if (ri == doneIndex) choice = DONE
                }
            }

            if (choice != DONE) {
                choices << choice
                opts.removeElement(choice)
                optMap = createVertexOptMap(opts, vertexPropName)
            }
        }

        return choices
    }         


    /** */
    Command menuCommands(List<Command> opts, String prompt = null) {
        if (!prompt) prompt = "CHOOSE AN OPTION:"
        if (!prompt.endsWith(':')) prompt += ":"
        
        println "\n${prompt}"

        Map<Integer,Command> optMap = createCommandOptMap(opts)

        optMap.each { k, v ->
            def optstr = "($k)"
            if (![CommandCategory.APP].contains(v.category)) optstr += " ${v.category.label} -"
            optstr += " ${v.prompt}"

            println optstr
        }

        def choice
        while (choice == null) {
            print "\n> "

            def r = stdin.readLine()?.trim()
            if (r.isInteger()) {
                def ri = Integer.valueOf(r)
                if (optMap.containsKey(ri)) choice = optMap.get(ri)
            }
        }

        return choice
    }


    /** */
    Enum menuStrings(Class enumType, String prompt = null) {
        //log.trace "menuStrings(Class enumType, String prompt = null)"

        def enums = EnumSet.allOf(enumType)
        menuStringsEnums(enums, prompt)
    }


    /** */
    Enum menuStrings(Set<Enum> enums, String prompt = null) {
        //log.trace "menuStrings(Set<Enum> enums, String prompt = null)"

        menuStringsEnums(enums, prompt)
    }


    /** */
    Enum menuStringsEnums(Collection<Enum> enums, String prompt = null) {
        //log.trace "Enum menuStringsEnums(Collection<Enum> enums, String prompt = null)"

        Map<String,Enum> optMap = new LinkedHashMap<String,Enum>()
        enums.each {
            //log.debug "${it} ${it.class.simpleName} ${it.name()} ${it.description}" 
            def str = it.name()
            if (it.hasProperty('description')) str += " - ${it.description}"
            optMap.put(str, it) 
        }
        //def opts = enums*.name()
        def opts = optMap.keySet().toList()
        def opt = menuStrings(opts, prompt)
        return optMap.get(opt)
    }


    /** */
    List<String> multiLineInput(String prompt) {
        println "\n\n$prompt\n"

        List<String> lines = []
        def line
        while (line == null || line.trim().size() > 0) {
            line = stdin.readLine()?.trim()
            if (line) lines << line
        }

        return lines
    }


    /** */
    String toString(Collection<String> lines) {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        lines.each { pw.println(it) }
        return sw.toString()
    }



    /** */
    Properties loadGradleProperties() {
        final Properties properties = new Properties()
        final InputStream stream = this.class.getResourceAsStream("/version.properties")
        if (stream == null) {
            log.warn "could not load resource 'version.properties'"
            return null
        }
        try {
            properties.load(stream);
            /* or properties.loadFromXML(...) */
        } finally {
            if (stream) stream.close()
        }
        return properties
    }

    
    /** */
    String getAppVersion(Properties props) {
        if (props == null) {
            log.warn "props is null"
            return null
        }
        def k = 'version'
        return (props.containsKey(k)) ? props.getProperty(k) : null 
    }


    /** */
    String getAppVersion() {
        def props = loadGradleProperties()
        if (props) return getAppVersion(props)
        else return System.property('app.version')
    }


    /**
     * Prompt the user to choose a file in the current relative path.
     *
     * If selected, copy the file to the target directory and return original file.
     * If no files found or the user chooses to cancel, return null.
     * 
     * @return File 
     */
    File promptForReportConfigFile(String extension = '.xlsx') {
        // look for potential config files in the current directory
        Path currentRelativePath = Paths.get("")
        String curpath = currentRelativePath.toAbsolutePath().toString()
        log.debug "Current relative path is: $curpath"   
        File curdir = new File(curpath)
        assert curdir.exists()
        assert curdir.isDirectory()
        def files = curdir.listFiles()
        def excelFiles = files.findAll { it.name.endsWith(extension) }
        log.debug "excelFiles: $excelFiles"

        def reportConfig

        // get a report config from selected config file
        if (excelFiles.size() > 0) {
            Map<String,File> optMap = new TreeMap<String,File>()
            excelFiles.each { optMap.put(it.name, it) }

            List<String> opts = optMap.keySet().toList()

            def cancel = "Cancel"
            opts += cancel

            def opt = menuStrings(opts, 'Choose a configuration file:')
            log.debug "opt: $opt"

            if (opt == cancel) return
            else {
                def reportConfigFile = optMap.get(opt)
                log.debug "reportConfigFile: $reportConfigFile"

                File targetDir = Defaults.targetDirectory
                Path reportConfigFilePath = Paths.get(reportConfigFile.canonicalPath)
                Path reportConfigFileTargetPath = Paths.get("${targetDir.canonicalPath}/${reportConfigFile.name}")
                Files.copy(reportConfigFilePath, reportConfigFileTargetPath, StandardCopyOption.REPLACE_EXISTING)

                return reportConfigFile
            }
        }
        else {
            prompt("No files with the extension '$extension' found on path $curpath.")
            return
        }
    }


    /** */
    public String getSplashScreen() {

        def logos = []
        logos.add( '''
     _/_/_/    _/_/    _/_/_/    _/      _/  _/_/_/  _/      _/    _/_/    _/     
  _/        _/    _/  _/    _/  _/_/    _/    _/    _/      _/  _/    _/  _/      
 _/        _/_/_/_/  _/_/_/    _/  _/  _/    _/    _/      _/  _/_/_/_/  _/       
_/        _/    _/  _/    _/  _/    _/_/    _/      _/  _/    _/    _/  _/        
 _/_/_/  _/    _/  _/    _/  _/      _/  _/_/_/      _/      _/    _/  _/_/_/_/
''')
                
        logos.add($/
________  ________  ________  ________   ___  ___      ___ ________  ___          
|\   ____\|\   __  \|\   __  \|\   ___  \|\  \|\  \    /  /|\   __  \|\  \         
\ \  \___|\ \  \|\  \ \  \|\  \ \  \\ \  \ \  \ \  \  /  / | \  \|\  \ \  \        
 \ \  \    \ \   __  \ \   _  _\ \  \\ \  \ \  \ \  \/  / / \ \   __  \ \  \       
  \ \  \____\ \  \ \  \ \  \\  \\ \  \\ \  \ \  \ \    / /   \ \  \ \  \ \  \____  
   \ \_______\ \__\ \__\ \__\\ _\\ \__\\ \__\ \__\ \__/ /     \ \__\ \__\ \_______\ 
    \|_______|\|__|\|__|\|__|\|__|\|__| \|__|\|__|\|__|/       \|__|\|__|\|_______|
/$) 

        logos.add( $/
                           .__              .__   
  ____ _____ _______  ____ |__|__  _______  |  |  
_/ ___\\__  \\_  __ \/    \|  \  \/ /\__  \ |  |  
\  \___ / __ \|  | \/   |  \  |\   /  / __ \|  |__
 \___  >____  /__|  |___|  /__| \_/  (____  /____/
     \/     \/           \/               \/      
/$)


        // random logo
        Collections.shuffle logos
        def str = """
${logos.first()}"""

        // tagline
        str += """

    A Party of Information              
"""

        // try to get the app version
        def appVersion = getAppVersion()
        if (appVersion) str += """
    Version ${appVersion}
"""

        return str
    }    

}