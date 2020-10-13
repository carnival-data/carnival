package carnival.core.vine



import groovy.util.logging.Slf4j

import groovy.sql.*

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.security.SecureRandom

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair

import java.text.SimpleDateFormat

import carnival.core.config.RedcapConfig
import carnival.core.config.DatabaseConfig



/**
 * Convenience class that offers useful utilities for interacting with Redcap.
 *
 */
@Slf4j
abstract class RedcapVine extends Vine {

    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /** */
    static final List<String> POSITIVE_VALS = ["y", "yes", "1"]

    /** */
    static final List<String> NEGATIVE_VALS = ["n", "no", "0"]

    /** */
    static final List<String> IGNORE_VALS = [""]


    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    final String redcapIdField

    /** */    
    RedcapConfig redcapConfig

    /** default filter logic to use with getRedcapRecords() */
    String defaultFilterLogic


    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public RedcapVine(RedcapConfig redcapConfig, String redcapIdField, String defaultFilterLogic = null) {
        super()
        assert redcapIdField
        this.redcapConfig = redcapConfig
        this.redcapIdField = redcapIdField
        if (defaultFilterLogic) this.defaultFilterLogic = defaultFilterLogic
    }

    ///////////////////////////////////////////////////////////////////////////
    // GENERIC QUERY METHODS
    ///////////////////////////////////////////////////////////////////////////
    
    /** 
     * temporary until vertex property date format is standardized
     *
     */
    public static formatDateAsRedcapDateString(String dateValue, String dateFormat = "MM/dd/yyyy") {
        SimpleDateFormat df = new SimpleDateFormat('yyyy-MM-dd')

        def dateString = ""
        try { 
            def date = new Date().parse(dateFormat, dateValue)
            dateString = df.format(date)
        } catch (Exception e) {
            //e.printStackTrace()
            log.warn "Could not parse date $dateValue in date format $dateFormat"
        }

        return dateString
    }

    ///////////////////////////////////////////////////////////////////////////
    // GENERIC QUERY METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    protected Collection<String> getAllRedcapIds() {
        assert redcapIdField

        def res = getRedcapRecords (
            fields:[redcapIdField]
        )
        assert res.success

        def recordIds = res.data.collect { row ->
            println "row: $row"
            
            def rawIdProp = row[redcapIdField]
            if (rawIdProp == null) throw new RuntimeException("row[${redcapIdField}] is null")
            
            def idVal = rawIdProp.value
            if (idVal == null) throw new RuntimeException("row[${redcapIdField}].value is null")

            String.valueOf(idVal) 
        }

        return recordIds
    }

    /**
     *
     *
     * @return list of maps with the keys [redcapIdField, "redcap_repeat_instrument", "redcap_repeat_instance"]
     */
    public Collection<Map> getAllRepeatInstanceIds(String form) {
        assert redcapIdField

        def res = getRedcapRecords (
            forms:[form],
            fields:[redcapIdField, "redcap_repeat_instrument", "redcap_repeat_instance"]
        )
        assert res.success

        return res.data.findAll{ it.redcap_repeat_instrument.equals(form)}
    }


    ///////////////////////////////////////////////////////////////////////////
	// REDCAP CONNECTION METHODS
	///////////////////////////////////////////////////////////////////////////
    
    /**
     * Clear all the data in the project
     */
    public deleteAllRecords() {
        def redcapIds = getAllRedcapIds()
        
        log.trace "deleteAllRecords redcapIds: $redcapIds"

        def res = deleteRedcapRecords(redcapIds:redcapIds)
        assert res.success
    }


    /**
     * Get all records for a redcap project
     *
     * @param args.fields A list of fields to be pulled per record.  config.id_field will always be pulled.
     * @param args.forms A list of forms to be pulled per record.
     * @param args.redcapId Optionally restrict results to a single redcap id.
     * @param args.redcapIds Optionally restrict results to a a list of redcap id.
     * @param args.filterLogic String Optional filter logic to be passed into the REDCap api call.  e.g.:'[squash_status_complete] = 2 and [hup_mrn] != 100000000'.  Overwrites the value of defaultFilterLogic.
     * @param args.ignoreFilterLogic Bool Optional - If true, ignore defaultFilterLogic and args.filterLogic
     *
     * @return [success:bool, message:string, data:arrays and maps]
     *
     */
    protected getRedcapRecords(Map args) {
        log.trace "getRedcapRecords args.size: ${args.size()}"
        //log.trace "RedcapVine.getRedcapRecords args: $args"
        
        // validate our args
        assert redcapConfig


        // it would seem that REDCap prioritizes the forms param over the
        // fields param.  so, they cannot be used in conjunction.
        //if (args.forms && args.fields) throw new IllegalArgumentException("'fields' and 'forms' params will not produce expected results if used together")

        // in order to restrict that volume of data returned, enforce that either the forms of the fields arguments are set
        //if (!(args.forms || args.fields)) throw new IllegalArgumentException("please use the 'fields' or 'forms' params")

        // construct the REDCap request params
        def params = [
            token:redcapConfig.apiToken, 
            content:'record', 
            format:'json',
            type:'flat',
            //rawOrLabel:'both',  //experimental
            rawOrLabel: 'raw',
            rawOrLabelHeaders: 'raw',
            exportCheckboxLabel: false,
            exportSurveyFields: false,
            exportDataAccessGroups: false,
            returnFormat:'json',
        ]

        // optionally restrict to certain forms
        if (args.forms) {
            def forms = args.forms.collect()
            forms = forms.unique()
            params.forms = forms.join(",")
        }

        // optionally restrict to certain fields
        if (args.fields) {
            def fields = args.fields.collect()

            // always pull the primary key
            if (!(redcapConfig.idField in fields)) fields += redcapConfig.idField

            // uniquify
            fields = fields.unique()

            // assign to params
            def fieldsStr = fields.join(",")
            params.fields = fieldsStr
        }

        // optionally add filter logic
        if (args.ignoreFilterLogic) {
            log.warn "WARNING: getRedcapRecords() ignoring defaultFilterLogic ($defaultFilterLogic) with filterLogic argument ($args.filterLogic)"
        }
        else if ((defaultFilterLogic) && (args.filterLogic)) {
            log.warn "WARNING: getRedcapRecords() overwriting defaultFilterLogic ($defaultFilterLogic) with filterLogic argument ($args.filterLogic)"
            params.filterLogic = args.filterLogic
        }
        else if (args.filterLogic) {
            params.filterLogic = args.filterLogic
        }
        else if (defaultFilterLogic) {
            params.filterLogic = defaultFilterLogic
        }


        if (args.redcapId) params.records = [args.redcapId]

        if (args.redcapIds) {
            if (args.redcapIds.size() == 1) {
                params.records = args.redcapIds  
            } else if (args.redcapIds.size() < 1) {
                return [
                    success: false, 
                    message: "no redcapIds provided"
                ]
            } else if (args.redcapIds.size() > 1) {
                params.records = args.redcapIds.join(",")
            }
        }

        /*
        this only seems to work if all the events included are longitudinal events
        if you just get all events and pass them in, nothing seems to be returned
        not sure i understand...

        if (args.events) params.events = args.events
        params.events = params.events.collect { it.startsWith("follow_up") ? it : null}
        log.debug "args.events: ${args.events}"
        //params.events = ['follow_up_1_arm_1', 'follow_up_2_arm_1']
        */

        // debug
        log.trace "RedcapVine.getRedcapRecords params: $params"
        return handlePost(params)
    }


    //////////////////////////////////////////////
    // Repeating Records
    //////////////////////////////////////////////
    
    /**
     *
     * Can I import data into repeating forms?
     *
     * You can import data into reporting forms by using the redcap_repeat_instrument and redcap_repeat_instance fields to direct it to the appropriate form and instance. However, several people have found when they import into repeating forms the import is incomplete. It is highly recommended you manually check that the data imported correctly and completely after importing data into repeating forms.
     *
     * The import columns for repeating forms are:
     *   record_id,redcap_event_name,redcap_repeat_instrument,redcap_repeat_instance,redcap_data_access_group,[*data]
    */
    public getNextRepeatingFormId(String form, def recordId) {
        log.trace "getNextRepeatingFormId($form, $recordId)"

        def res = getRedcapRecords(
            forms:[form], 
            redcapId:recordId, 
            fields:[

                "redcap_repeat_instrument",
                "redcap_repeat_instance"]
        )

        assert res.success


        def repeatIds = res.data.findResults{it.redcap_repeat_instrument.equals(form) ? it.redcap_repeat_instance : null}
        
        if (!repeatIds) return 1
        return repeatIds.max() + 1
    }

    
    /** */
    public Map getNextRepeatingFormIdForMultipleRecords(String form, Collection recordIds) {
        def res = getRedcapRecords(
            forms:[form], 
            redcapIds:recordIds, 
            fields:[
                "redcap_repeat_instrument",
                "redcap_repeat_instance"]
        )

        assert res.success

        def nextIdsMap = [:].withDefault { 1 }
        def groupedRes = res.data.groupBy { it[redcapIdField] }

        groupedRes.each { recordId, data -> 
            def repeatIds = data.findResults{it.redcap_repeat_instrument.equals(form) ? it.redcap_repeat_instance : null}
            if (!repeatIds) nextIdsMap[recordId] = 1
            else nextIdsMap[recordId] = repeatIds.max() + 1
        }

        return nextIdsMap
    }


    /**
     * @param args.dataMaps  - list of Maps of REDCap records to be posted
     * @param args.form      - name of the repeating form to be modified
     * @param args.enforceUniqueRecords (optional) - Boolean - if true, make sure the uploaded records are unique
     *
     * @return [success:Bool, message:String]
     */
    public addNewRedcapRepeatingInstrumentRecords(Map args) {

        // TODO - make sure that args.form is actually a repeating insturment
        assert args.form && args.form instanceof String
        assert args.dataMaps instanceof Collection<Map>

        def redcapIds = []
        args.dataMaps.each {
            assert it.containsKey(redcapIdField)
            assert !it.containsKey("redcap_repeat_instance") : "addNewRedcapRepeatingInstrumentRecords() argument dataMaps contained the key 'redcap_repeat_instance'.  This value will be generated and should not be included."
            // TODO - make sure all the keys are values in the given form

            redcapIds << it.get(redcapIdField)
        }

        if (args.enforceUniqueRecords) {
            assert args.dataMaps.unique(false) == args.dataMaps
        }

        //def nextFormIdMap = [:]
        def nextFormIdMap = getNextRepeatingFormIdForMultipleRecords(args.form, redcapIds)
        // log.debug "---"
        // log.debug "redcapIds: $redcapIds"
        // log.debug "nextFormIdMap: $nextFormIdMap"

        Collection<Map> newDataMaps = []
        args.dataMaps.each { row ->
            def currRecId = String.valueOf(row.get(redcapIdField))
            assert currRecId

            // log.debug "\tcurrRecId: $currRecId"
            // log.debug "\tnextId: ${nextFormIdMap[currRecId]}"

            //if (!(nextFormIdMap.containsKey(currRecId))) nextFormIdMap[currRecId] = getNextRepeatingFormId(args.form, currRecId)
            //if (!(nextFormIdMap.containsKey(currRecId))) nextFormIdMap[currRecId] = 1

            newDataMaps << row + [redcap_repeat_instance:nextFormIdMap[currRecId], redcap_repeat_instrument:args.form]
            nextFormIdMap[currRecId] = nextFormIdMap[currRecId] + 1
        }

        def res = updateRedcapRecord(dataMaps:newDataMaps)
        //verifyNewRepeatingRecords(newDataMaps, args.form, args.enforceUniqueRecords)  // slow...
        return res
    }


    /**
     * verify that sentData exists in redcap form 'form', and that each matching record is unique
     */
    private verifyNewRepeatingRecords(Collection<Map> sentData, String form, Boolean unique = false){
        def formData = getAllRepeatInstanceIds(form)
        assert formData.size() > 0

        def errMsg = []

        sentData.each { sentRow ->
            def matchingRows = formData.findAll { formRow -> 
                def match = true
                sentRow.each { k, v ->
                    match = match && (String.valueOf(formRow[k]) == String.valueOf(v))
                }
                return match
            }

            if ((unique && (matchingRows.size() != 1)) || (!unique && (matchingRows.size() == 0)))
                errMsg << "Unable to verify new record: $sentRow \nMatching rows in redcap: $matchingRows"
        }

        assert !errMsg
    }


    ///////////////////////////////////////////////
    //
    ///////////////////////////////////////////////

    /**
     * Update entries in a RedCap record.
     *
     * @param args.postData The data to be posted in CSV format. If postData is not provided, then the headers
     *        and data must be provided as lists or a map.
     *
     * @param args.headerItems List of REDCap header items to be posted.
     * @param args.dataItems List of REDCap data items to be posted.
     *
     * @param args.dataMap Map of REDCap record to be posted.
     *
     * @param args.dataMaps list of Maps of REDCap records to be posted.
     *
     *
     * @return [success:Bool, message:String]
     *
     */
    public updateRedcapRecord(Map args) {
        // validate our args
        if (!(
            args.containsKey('postData') ^ 
            (args.containsKey('headerItems') && args.containsKey('dataItems')) ^ 
            args.containsKey('dataMap') ^ 
            args.containsKey('dataMaps')
        ))
            throw new IllegalArgumentException("Exactly one of the following must be provided: args.postData, args.dataMap, (args.headerItems and args.dataItems).  Keys provided: ${args.keySet()}")


        assert redcapConfig

        if (validate(redcapConfig)) return validate(redcapConfig)
        def redcapUrl = redcapConfig.url
        def apiToken = redcapConfig.apiToken

        // gather up the post data as a csv
        String postData

        if (args.postData) {
            postData = args.postData
        } 
        else if (args.dataMap) {
            // map sort order not guaranteed
            def headerItems = []
            def dataItems = []
            args.dataMap.each { k, v -> 
                headerItems << k
                dataItems << v
            }

            postData = headerItems*.toString().join(",") + "\n" + dataItems*.toString().join(",")
        }
        else if (args.dataMaps) {
            // map sort order not guaranteed
            def headerItems = []
            def dataItemRows = []
            def dataItems = []

            // get header
            args.dataMaps.first().each { k, v -> 
                headerItems << k
            }

            // get remaining rows
            args.dataMaps.each { dataMap ->
                headerItems.each {field ->
                    if (dataMap[field]) dataItems << dataMap[field]
                    else dataItems << ""
                }
                dataItemRows << dataItems
                dataItems = []
            }
            /*
            println "header: $headerItems"
            println "dataItemRows: $dataItemRows"
            println "dataItemRow size: $dataItemRows.size()"
            */
            // format as csv data
            postData = headerItems*.toString().join(",")
            dataItemRows.each { dataRow ->
                postData += "\n" + dataRow*.toString().join(",")
            }
        }
        else {
            postData = args.headerItems*.toString().join(",")
            postData += "\n"
            postData += args.dataItems*.toString().join(",")
        }

        //log.debug "postData:\n---\n$postData\n---"

        def params = [
                token:apiToken,
                content:'record',
                format:'csv',
                //type:'flat',
                overwriteBehavior:'normal',
                //returnContent:'ids',
                returnFormat:'json',
                data:postData
        ]

        def res = handlePost(params)

        if (!res.success) return [
                success:false,
                message:"Request failed: ${res.message}"
        ]

        //log.debug "redcap responseData: " + responseData
        //if (data?.ids?.size() != 1)
        //    return [success:false, message:"Request succeded, but update failed: " + data]

        return [success:res.success, message:res.message]
    }


    /**
     * Delete records for a redcap project
     *
     * @param args.redcapIds restrict results to a a list of redcap id.
     *
     * @return [success:bool, message:string, data:arrays and maps]
     *
     */
    protected deleteRedcapRecords(Map args) {
        log.trace "deleteRedcapRecords args.size: ${args.size()}"
        //log.trace "RedcapVine.deleteRedcapRecords args: $args"
        
        assert redcapConfig

        // validate our args
        if (!args.containsKey('redcapIds')) throw new IllegalArgumentException("args.redcapIds cannot be null")
        assert args.redcapIds instanceof List
        
        // construct the REDCap request params
        def params = [
            token:redcapConfig.apiToken, 
            action:'delete',
            content:'record', 
            format:'json'
        ]
 
        if (args.redcapIds.size() == 0 ) {
            //throw new IllegalArgumentException("args.redcapIds cannot be empty")
            return [success:true, message:"No records in project."]
        } else {
            //params.records = args.redcapIds.join(",")
            //params.add(new BasicNameValuePair("records", args.redcapIds as ArrayList))
            //params.records = args.redcapIds.collect{String.valueOf(it)} as ArrayList
            //params.records = args.redcapIds
            params.put("records[]", args.redcapIds) // records needs to be an array
        }

        // debug
        log.trace "RedcapVine.deleteRedcapRecords params: $params"
        return handlePost(params)
    }


    /**
     * Send a POST message to RedCap
     *
     *
     * @parm params - params to be sent to redcap  
     * @contentType - the content type of the message [ContentType.JSON, ContentType.URLENC]
     *
     * @return [sucess:Boolean, data:returnedJsonData, message:String, exception:Exception]
     */
    private Map handlePost(Map params, ContentType sendContentType = ContentType.URLENC, ContentType returnContentType = ContentType.JSON) {
        def paramsStr = String.valueOf(params).size() > 500 ? String.valueOf(params).substring(0, 500) + "..." : String.valueOf(params)

        log.trace "RedcapVine.handlePost params: $paramsStr"
        //log.debug "RedcapVine.handlePost"        

        def http = createHTTPBuilder(redcapConfig)

        def data = null
        def message = null
        def success = false

        // perform a POST request, expecting JSON response data
        try {
            http.request( POST, returnContentType) {
                send sendContentType, params
                //body = params

                // bad request
                response.'400' = { resp, json ->
                    success = false
                    message = "${resp.statusLine}, error: $json"
                }

                // response handler for unautorized access
                response.'401' = { resp, json ->
                    success = false
                    message = "${resp.statusLine}, error: $json"
                }

                // forbidden
                response.'403' = { resp, json ->
                    success = false
                    message = "${resp.statusLine}, error: $json"
                }

                // not found
                response.'404' = { resp, json ->
                    success = false
                    message = "${resp.statusLine}, error: $json"
                }

                // improperly formatted input
                response.'406' = { resp, json ->
                    success = false
                    message = "${resp.statusLine}, error: $json"
                }

                // response handler for a success response code:
                response.success = { resp, json ->
                    log.trace "getRedcapData: successful http.request"
                    log.trace "Got response: ${resp.statusLine}"
                    log.trace "Content-Type: ${resp.headers.'Content-Type'}"
                    
                    //log.debug "getRedcapRecords json: ${json}"
                    //log.debug "response: " + squash.util.JSONTranslatable.convertObjectToJSON(json, log).toString(2)

                    success = true
                    data = json
                }

                // handler for any failure (>400) status code:
                //response.failure = { resp, json -> // in the event of a failure an xml message is usually passed which causes the json parser to throw an exception upon entering this closure
                response.failure = { resp, json ->
                    success = false
                    message = "Received failure code when posting REDCapData: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase} : $json"

                    log.trace "getRedcapData: failure http.request"
                    log.trace "Got response: ${resp.statusLine}"
                    log.trace "Content-Type: ${resp.headers.'Content-Type'}"
                    log.trace "$resp.statusLine"
                    log.trace "$resp"
                    log.trace "json: $json"
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace()
            return [success:false, message:"Unexpected error when posting REDCap data.", exception:exception]
        }

        return [success:success, data:data, message:message]
    }


    /** */
    private Map validate(RedcapConfig config) {
        if (!config) throw new IllegalArgumentException("null config")

        if (!config.url) return [
            success:false, 
            message:"No REDCap URL configured."
        ]

        if (!config.apiToken) return [
            success:false, 
            message:"No REDCap API token configured."
        ]

        return [:]
    }



    ///////////////////////////////////////////////////////////////////////////
    // BASIC HTTP SERVICES
    ///////////////////////////////////////////////////////////////////////////

    /** */
    private createHTTPBuilder(RedcapConfig config) {
        def url = config.url

        if (config.ignoreAllSSLValidation) return generateIgnoreSSLHTTPBuilder(url)
        else if (config.trustAllSSLCertificates) return generateSSLTrustingHTTPBuilder(url)
        else return new HTTPBuilder(url)
    }


    /**
     * Create an HTTPBuilder object with a custom HTTPClient that accepts all SSL certificates
     * NOT SECURE!!!! DO NOT USE IN PRODUCTION!!!
     *
     * @param url
     *
     * @return HTTPBuilder - HTTPBuildder object with a custom HTTPClient that accepts all SSL certificates
     *
     */
    private HTTPBuilder generateSSLTrustingHTTPBuilder(String url) {
        def http = new HTTPBuilder(url) {
            @Override
            org.apache.http.impl.client.AbstractHttpClient createClient(org.apache.http.params.HttpParams params) {
                DefaultHttpClient httpclient = new DefaultHttpClient();

                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, getTrustingManager(), new java.security.SecureRandom());

                SSLSocketFactory socketFactory = new SSLSocketFactory(sc);
                Scheme sch = new Scheme("https", 443, socketFactory);
                httpclient.getConnectionManager().getSchemeRegistry().register(sch);
                return httpclient;
            }
        }

        return http
    }


    /**
     * Create an HTTPBuilder object with a custom HTTPClient that accepts all SSL certificates
     * and ignores host name validation.
     * NOT SECURE!!!! DO NOT USE IN PRODUCTION!!!
     *
     * @param url
     *
     * @return HTTPBuilder - HTTPBuildder object with a custom HTTPClient that accepts all SSL certificates
     *
     */
    private HTTPBuilder generateIgnoreSSLHTTPBuilder(String url) {
        def httpBuilder = new HTTPBuilder(url)
        def nullTrustManager = [
            checkClientTrusted: { chain, authType ->  },
            checkServerTrusted: { chain, authType ->  },
            getAcceptedIssuers: { null }
        ]
        def nullHostnameVerifier = [
            verify: { hostname, session -> true }
        ]
        SSLContext sc = SSLContext.getInstance("SSL")
        sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], new SecureRandom())
        SSLSocketFactory sf=new SSLSocketFactory(sc)
        sf.hostnameVerifier=SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
        httpBuilder.getClient().getConnectionManager().schemeRegistry.register(new Scheme("https",sf,443))

        return httpBuilder
    }


    /**
     * Returns a trusting trust manager for use in development.
     *
     */
    private TrustManager[] getTrustingManager() {
        TrustManager[] trustAllCerts = new TrustManager[1]

        trustAllCerts[0] = new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // Do nothing
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // Do nothing
            }
        }

        return trustAllCerts
    }

}