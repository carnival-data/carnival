package carnival.core.vine



import groovy.util.logging.Slf4j
import groovy.transform.ToString

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.ContentTypes.*
import groovyx.net.http.*
import static groovyx.net.http.util.SslUtils.ignoreSslIssues


/** 
 * A configuration of an HTTP server that offers web services.
 *
 * To use: instantiateHttpBuilder() to configure an HttpBuilder according to the server config.
 * This object can then be used to:
 *
 * - Generate a configured HttpBuilder object that can make http requests directly (see <https://http-builder-ng.github.io/http-builder-ng/asciidoc/html5/#_make_requests>)
 * - Create an HttpEndpoint object which has convience methods to make basic http calls and parse results
 * or exceptions into a map.  (See HttpEndpoint.handlePost())
 *
 *
 * Example of generating a httpBuilder to POST json data:

        def httpServerConfig = new HttpServerConfig(
            baseUrl: "https://jsonplaceholder.typicode.com"
        )

        def http = httpServerConfig.instantiateHttpBuilder()

        def res = http.post {
            request.uri.path = "/posts"
            request.contentType = ContentTypes.JSON[0]
            request.body = [message:"wordswordswords"]
        }

        assert res == ["message":"wordswordswords", id:101]
 *
 *
 */
@ToString(includeSuperProperties=true, includeNames=true)
class HttpServerConfig {
    String baseUrl
    Boolean ignoreAllSSLValidation = false
    Boolean trustAllSSLCertificates = false

    HttpEndpointConfig createEndpointConfig(Map args) {
        HttpEndpointConfig.create(this, args)
    } 

    /** 
    *    Return an instantiated httpBuilder to make requests on.
    */
    def instantiateHttpBuilder() {
        HttpBuilder.configure {
            request.uri = baseUrl
            if (ignoreAllSSLValidation || trustAllSSLCertificates) ignoreSslIssues execution
        } 
    }
}


/** 
 * An HttpEndpointConfig extends HttpServerConfig to provide a full path and 
 * specified content type to an HTTP web service.
 *
 */
@ToString(includeSuperProperties=true, includeNames=true)
class HttpEndpointConfig extends HttpServerConfig {

    static public create(HttpServerConfig httpServerConfig, Map args) {
        def hrc = new HttpEndpointConfig(args)
        hrc.baseUrl = httpServerConfig.baseUrl
        hrc.ignoreAllSSLValidation = httpServerConfig.ignoreAllSSLValidation
        hrc.trustAllSSLCertificates = httpServerConfig.trustAllSSLCertificates
        return hrc
    }

    String urlPath
    ContentTypes sendContentType = ContentTypes.JSON
    //ContentTypes returnContentType = ContentTypes.JSON

    String url() {
        return baseUrl + urlPath
    }

    /** 
    Return an instantiated httpBuilder to make requests on.
    */
    def instantiateHttpBuilder() {
        HttpBuilder.configure {
            request.uri = baseUrl
            request.uri.path = urlPath
            if (sendContentType) request.contentType = sendContentType[0]

            if (ignoreAllSSLValidation || trustAllSSLCertificates) ignoreSslIssues execution
        } 
    }
}




/**
 * An HttpEndpoint is a wrapper for a specific URL web service endpoint as
 * specified by an HttpEndpointConfig. 
 *
 */
@Slf4j
class HttpEndpoint {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static public HttpEndpoint create(HttpServerConfig httpServerConfig, String urlPath, ContentType contentType = ContentType.JSON) {
        def conf = httpServerConfig.createEndpointConfig(urlPath:urlPath, sendContentType:ContentTypes.JSON)
        def he = new HttpEndpoint(conf)
        return he
    }


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    HttpEndpointConfig httpEndpointConfig


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public HttpEndpoint(HttpEndpointConfig httpEndpointConfig) {
        assert httpEndpointConfig
        this.httpEndpointConfig = httpEndpointConfig
    }



    ///////////////////////////////////////////////////////////////////////////
    // BASIC HTTP SERVICES
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Post and return a map of the result.  The httpEndpointConfig defines the content type.
     *
     * @param params - post params 
     *
     * @return [sucess:Boolean,
     *          data:returnedJsonData,
     *          message:String,
     *          exception:Exception,
     *          responseCode:responseCode]
     *
     */
    public Map handlePost(Map params) {

        def http = this.httpEndpointConfig.instantiateHttpBuilder()
        http.post {
            request.body = params

            response.success { FromServer resp, body ->
                log.trace "HttpEndpoint.handlePost: successful http.request"
                log.trace "Got response: ${resp.statusCode}"
                log.trace "Content-Type: ${resp.contentType}"

                return [success:true, data:body, message:"success", responseCode:resp.statusCode]
            }
            response.failure { FromServer resp ->
                success = false
                responseCode = resp.status
                message = "HttpEndpoint.handlePost received failure code: ${resp.statusLine?.statusCode} : ${resp.statusLine}"

                log.error "HttpEndpoint.handlePost: failure http.request"
                log.error "Got response: ${resp.statusCode}"
                log.error "Got message: ${resp.message}"
                log.trace "Content-Type: ${resp.contentType}"
                log.error "resp: $resp"

                return [success:false, data:resp.message, message:"failure", responseCode:resp.statusCode]
            }
            response.exception  { Throwable exception ->
                def msg
                if (exception instanceof ConnectException) msg = "ConnectException when handling HttpEndpoint.handlePost httpEndpointConfig:$httpEndpointConfig params:$params, connection refused."
                else if (exception instanceof UnknownHostException) msg = "UnknownHostException when handling HttpEndpoint.handlePost httpEndpointConfig:$httpEndpointConfig params:$params, connection refused."
                else msg = "Exception when handling HttpEndpoint.handlePost httpEndpointConfig:$httpEndpointConfig params:$params"
                
                log.error(msg, exception.message)
                return [success:false, message:msg, exception:exception, responseCode:null]
            }
        }
    }
}