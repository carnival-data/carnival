package carnival.core.util


/**
 * Exception class expected to be thrown when elements of a model expected to
 * be unique are found to be duplicates; distinct from data, this exception is
 * intended for the model.
 */
class DuplicateModelException extends Exception {

    /**
     * Consctuctor with a string message. 
     * @param msg The message string.
     */
    DuplicateModelException(String msg) {
        super(msg)
    }

}