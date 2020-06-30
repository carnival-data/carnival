package carnival.core.vine



import groovy.util.logging.Slf4j

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.FilterProvider
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.codec.digest.DigestUtils

import carnival.util.StringUtils
import carnival.core.util.CoreUtil



@Slf4j
class JsonVineMethodListCall<T> extends JsonVineMethodCall<T> {

    ///////////////////////////////////////////////////////////////////////////
    // CLASSES
    ///////////////////////////////////////////////////////////////////////////

    static class ListHolder {
        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        List value
    }




    ///////////////////////////////////////////////////////////////////////////
    // STATIC METHODS 
    ///////////////////////////////////////////////////////////////////////////








    ///////////////////////////////////////////////////////////////////////////
    // STATIC FIELDS
    ///////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the result returned by the call */
    @JsonIgnore
    T result

    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    ListHolder resultHolder


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - RESULT
    ///////////////////////////////////////////////////////////////////////////

    public T getResult() {
        return this.resultHolder.value
    }

    public void setResult(T result) {
        this.result = result
        this.resultHolder = new ListHolder(value:result)
        this.resultClass = result.class
    }


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - COMPUTED PROPERTIES
    ///////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////
    // METHODS - JSON SERIALIZATION
    ///////////////////////////////////////////////////////////////////////////




    ///////////////////////////////////////////////////////////////////////////
    // METHODS - FILES
    ///////////////////////////////////////////////////////////////////////////



}