package carnival.util



import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static com.xlson.groovycsv.CsvParser.parseCsv
import com.xlson.groovycsv.CsvIterator
import com.xlson.groovycsv.PropertyMapper

import groovy.sql.GroovyRowResult
import groovy.transform.EqualsAndHashCode



/**
 * NOT USED!  INCLUDED FOR INFORMATIONAL/EXAMPLE PURPOSES ONLY.
 *
 */
class FieldNameExtensions {
    
    static FieldName getFieldName(String str) {
        FieldName.create(str)
        new FieldName(str)
    }


    /*
    
    attempted to create equality, compareTo, and hashCode concordance between
    FieldName and String, but could not get it to work.

    static int compareTo(String str, FieldName fn) {
        println "\n\n\ncompareTo FieldName $str $fn \n\n\n"
        str.compareTo(fn.value)
    }


    static boolean equals(String str, FieldName fn) {
        println "\n\n\nequals $str $fn \n\n\n"
        str.equals(fn.value)
    }


    static boolean equals(String str, Object fn) {
        println "\n\n\nequals obj $str $fn ${fn.class.name}\n\n\n"

        def methods = str.metaClass.methods
        methods.each { if ("$it".indexOf('equals') > -1) println ">> ${it.name} ${it.declaringClass.name}" }
        def origEqualsMethod = methods.find { it.name == "equals" && it.declaringClass.name == "java.lang.String" }
        
        //if (fn instanceof carnival.core.util.FieldName) return str.equals(fn.value)
        //else return false

        if (fn instanceof carnival.core.util.FieldName) {
            println "fn instanceof carnival.core.util.FieldName"
            return origEqualsMethod.invoke(str, [fn.value].toArray())
        } else {
            println "NOT fn instanceof carnival.core.util.FieldName"
            return origEqualsMethod.invoke(str, [fn].toArray())
        }
    }
    
    */

}