package carnival.util




/**
 * Utility class with application level helper methods.
 *
 */
class AppUtil {

	/**
	 * Return true iff all of the following:
	 *   - the given prop exists
	 *   - the string representation of the prop is not zero length
	 *   - the Boolean value of the string maps to false 
	 *
	 * Useful if some default behavior is expected to be enabled, but can be
	 * disabled by setting a property to false.
	 *
	 */
	public static boolean sysPropFalse(String propName) {
        def propVal = System.getProperty(propName)
        return (propVal != null && String.valueOf(propVal).size() > 0 && !Boolean.valueOf(propVal))
	}

}



