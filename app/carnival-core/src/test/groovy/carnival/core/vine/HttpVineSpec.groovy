package carnival.core.vine


import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.Shared
import spock.lang.IgnoreIf

import static groovyx.net.http.HttpBuilder.configure
import static groovyx.net.http.ContentTypes.*
import groovyx.net.http.*
import static groovyx.net.http.util.SslUtils.ignoreSslIssues


/**
 * gradle :carnival-core:test --tests "carnival.core.vine.HttpVineSpec"
 *
 */
class HttpVineSpec extends Specification {


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    // for now, test against a public testing server.  In the future,
    // use a mocking library like Ersatz <http://stehno.com/ersatz/>
    @Shared testBaseUrl = "https://jsonplaceholder.typicode.com/"
    @Shared testUrlPostPath = "/posts"
    @Shared testUrlGetPath = "/todos/1"

    @Shared testPostParms = [message:"wordswordswords"]
    @Shared testPostExpectedResult = ["message":"wordswordswords", id:101]


    ///////////////////////////////////////////////////////////////////////////
    // SET UP
    ///////////////////////////////////////////////////////////////////////////
    
    // optional fixture methods
    /*
    def setup() {}          // run before every feature method
    def cleanup() {}        // run after every feature method
    def setupSpec() {}     // run before the first feature method
    def cleanupSpec() {}   // run after the last feature method
    */


    ///////////////////////////////////////////////////////////////////////////
    // TESTS
    ///////////////////////////////////////////////////////////////////////////



    def "generate httpBuilder from HttpServerConfig"() {
        when:
        def httpServerConfig = new HttpServerConfig(
            baseUrl: testBaseUrl,
            ignoreAllSSLValidation: false,
            trustAllSSLCertificates: false
            )

        then:
        httpServerConfig.baseUrl == testBaseUrl

        when:
        def http = httpServerConfig.instantiateHttpBuilder()

        then:
        http
        http instanceof HttpBuilder
        
        when:
        def httpObjConfig = http.getObjectConfig()
        println "httpObjConfig.getContextMap(): ${httpObjConfig.getContextMap()}"

        then:
        httpObjConfig.getContextMap() == [:]
    }

    def "generate httpBuilder from HttpServerConfig ignoreAllSSLValidation"() {
        when:
        def httpServerConfig = new HttpServerConfig(
            baseUrl: testBaseUrl,
            ignoreAllSSLValidation: true,
            trustAllSSLCertificates: false
            )

        then:
        httpServerConfig.baseUrl == testBaseUrl

        when:
        def http = httpServerConfig.instantiateHttpBuilder()

        then:
        http
    }

    @IgnoreIf({ !Boolean.valueOf(properties['test.http']) })
    def "post from HttpServerConfig configured httpBuilder"() {
        given:
        def httpServerConfig = new HttpServerConfig(
            baseUrl: testBaseUrl,
            ignoreAllSSLValidation: false,
            trustAllSSLCertificates: false
            )

        when:
        def http = httpServerConfig.instantiateHttpBuilder()

        def res = http.post {
            request.uri.path = testUrlPostPath
            request.contentType = ContentTypes.JSON[0]
            request.body = testPostParms
        }

        then:
            res == testPostExpectedResult
    }

    @IgnoreIf({ !Boolean.valueOf(properties['test.http']) })
    def "post from HttpEndpoint configured httpBuilder"() {
        given:
        def httpServerConfig = new HttpServerConfig(
            baseUrl: testBaseUrl,
            ignoreAllSSLValidation: false,
            trustAllSSLCertificates: false
            )

        def httpEndpoint = HttpEndpoint.create(
                httpServerConfig, 
                testUrlPostPath
            )

        when:
        def http = httpEndpoint.httpEndpointConfig.instantiateHttpBuilder()

        def res = http.post {
            request.body = testPostParms
        }

        then:
            res == testPostExpectedResult
    }

    @IgnoreIf({ !Boolean.valueOf(properties['test.http']) })
    def "create HttpEndpoint" () {
        given:
        def httpServerConfig = new HttpServerConfig(
            baseUrl: testBaseUrl,
            ignoreAllSSLValidation: false,
            trustAllSSLCertificates: false
            )

        when:
        def httpEndpoint = HttpEndpoint.create(
                httpServerConfig, 
                testUrlPostPath
            )

        then:
        httpEndpoint.httpEndpointConfig.url() == "${testBaseUrl}${testUrlPostPath}"

        when:
        def http = httpEndpoint.httpEndpointConfig.instantiateHttpBuilder()

        then:
        http
    }

    @IgnoreIf({ !Boolean.valueOf(properties['test.http']) })
    def "httpEndpoint handlePost good" () {
        given:
        def httpServerConfig = new HttpServerConfig(
            baseUrl: testBaseUrl,
            ignoreAllSSLValidation: false,
            trustAllSSLCertificates: false
            )

        def httpEndpoint = HttpEndpoint.create(
                httpServerConfig, 
                testUrlPostPath
            )

        when:
        def res = httpEndpoint.handlePost(testPostParms)

        then:
        res.success
        res.data instanceof Map
        res.data == testPostExpectedResult
    }


    @IgnoreIf({ !Boolean.valueOf(properties['test.http']) })
    def "httpEndpoint handlePost UnknownHostException" () {
        given:
        def httpServerConfig = new HttpServerConfig(
            baseUrl: 'http://some.unknown.host/',
            ignoreAllSSLValidation: false,
            trustAllSSLCertificates: false
            )

        def httpEndpoint = HttpEndpoint.create(
                httpServerConfig, 
                "test/unknown/host/path"
            )

        when:
        def res = httpEndpoint.handlePost(message:"wordswordswords")

        then:
        !res.success
        res.message.contains("Exception when handling HttpEndpoint.handlePost")
    }

}
