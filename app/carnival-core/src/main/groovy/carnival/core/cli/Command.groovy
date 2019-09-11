package carnival.core.cli



/** 
 * Superclass for command line functions.
 *
 */
class Command {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static final String ALL_COMMANDS = 'a2072c14-485c-4b58-8d94-d0dc76093b43'

    /** */
    static final Command QUIT = new Command(category:CommandCategory.APP, prompt:"Quit")

    /** the unique identifier of the command object */
    String uuid

    /** a prompt to display on the command line */
    String prompt

    /** the name of the method that contains the log for this Command */
    String methodName

    /** the category of this Command */
    CommandCategory category

    /** the method that contains the command logic */
    Closure methodClosure
}
