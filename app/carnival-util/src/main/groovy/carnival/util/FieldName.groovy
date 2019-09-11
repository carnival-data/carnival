package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.stream.IntStream

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import groovy.sql.GroovyRowResult
import groovy.transform.EqualsAndHashCode
import groovy.transform.InheritConstructors
import org.codehaus.groovy.runtime.GStringImpl


/**
 * FieldName offers some some standardization on how to handle field
 * (column) names.
 *
 * NOTE: THIS CLASS IS NOT USED! KEPT AROUND FOR INFORMATIONAL PURPOSES. SEE
 * FieldNameExtensions FOR AN EXAMPLE OF HOW TO ADD FUNCTIONALITY TO A CORE
 * JAVA/GROOVY CLASS THAT INVOLVES FieldName.
 *
 */
@EqualsAndHashCode
class FieldName {

    static Map<String,FieldName> cache = new HashMap<String,FieldName>()

    static FieldName create(String value) {
        assert value
        value = value.trim().toUpperCase()
        
        def existing = cache.get(value)
        if (existing) return existing

        def fn = new FieldName(value)
        cache[value] = fn
        return fn
    }

    final String value

    private FieldName() {
    }

    private FieldName(String value) {
        assert value
        this.value = value.trim().toUpperCase()
    }

    public FieldName getFieldName() {
        return this
    }

    public String toString() {
        return value
    }

}





//@InheritConstructors
/*
class FieldName extends GStringImpl {

    def inValue

    public FieldName(String value) {
        super([], [value.toUpperCase()])
        inValue = value
    }

    public int hashCode() {
        return inValue.hashCode()
    }

}
*/


/*
class FieldName extends GString {

    //String value

    public FieldName(String value) {
        super([value].toArray())
        //this.value = value
    }

    public String[] getStrings() {
        return [value].toArray()
    }

}
*/





//@EqualsAndHashCode
/*
class FieldName implements CharSequence {

    static Map<String,FieldName> cache = new HashMap<String,FieldName>()

    static FieldName create(String value) {
        assert value
        value = value.trim().toUpperCase()
        
        def existing = cache.get(value)
        if (existing) return existing

        def fn = new FieldName(value)
        cache[value] = fn
        return fn
    }

    final String value

    private FieldName() {
    }

    private FieldName(String value) {
        assert value
        this.value = value.trim().toUpperCase()
    }

    ///////////////////////////////////////////////////////////////////////////
    // CHARSEQUENCE IMPLEMENTATION
    ///////////////////////////////////////////////////////////////////////////

    public char charAt(int index) {
        return value.charAt(index)
    }

    IntStream chars() {
        return value.chars()
    }

    IntStream codePoints() {
        return value.codePoints()
    }

    public int length() {
        return value.length()
    }

    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end)
    }


    public boolean equals(Object obj) {
        if (obj == null) return false
        if (obj instanceof String || obj instanceof GString) return value.equals(obj)
        if (obj instanceof FieldName) return this.value.equals(obj.value)
        return false
    }

    public int hashCode() {
        return value.hashCode()
    }

}
*/

