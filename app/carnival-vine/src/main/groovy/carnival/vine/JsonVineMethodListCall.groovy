package carnival.vine



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

import carnival.util.StringUtils



/**
 * JsonVineMethodListCall extends JsonVineMethodCall to support vine methods
 * that return lists of objects.
 *
 */
@Slf4j
class JsonVineMethodListCall<T> extends JsonVineMethodCall<T> {

    ///////////////////////////////////////////////////////////////////////////
    // CLASSES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * A simple wrapper for a list that facilitates JSON serialization.
     *
     */
    static class ListHolder {
        @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
        List value
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** the result returned by the call */
    @JsonIgnore
    T result

    /** required for JSON serialization to work properly */
    @JsonTypeInfo(use=Id.CLASS, include=As.WRAPPER_ARRAY)
    ListHolder resultHolder


    ///////////////////////////////////////////////////////////////////////////
    // METHODS - RESULT
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * Return the result of the call.
     * @return The result of the call
     */
    public T getResult() {
        return this.resultHolder.value
    }

    /**
     * Set the result of the call.
     * @param result The result of the call
     */
    public void setResult(T result) {
        this.result = result
        this.resultHolder = new ListHolder(value:result)
        this.resultClass = result.class
    }

}