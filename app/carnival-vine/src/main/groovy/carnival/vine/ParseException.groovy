package carnival.vine


/**
 * Class to represent exceptions parsing cache data.
 *
 */
class ParseException extends Exception { 

    /**
     * Consctuctor with a string message. 
     * @param msg The message string.
     */
    ParseException(String msg) {
        super(msg)
    }

}