package customconnectornode.discourse.http

import customconnectornode.discourse.api.DiscourseClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.hc.client5.http.classic.methods.HttpDelete
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory





/*
** TODO - Make it Async
 */

class DiscourseClientImpl implements DiscourseClient {
    private static final Logger log = LoggerFactory.getLogger(DiscourseClientImpl.class)



    private final CloseableHttpClient httpClient
    private final String baseUrl
    private final String apiKey
    private final String apiUsername
    private final JsonSlurper jsonSlurper = new JsonSlurper()

   DiscourseClientImpl() {
        this.baseUrl = System.getProperty("TRACKER_URL")
        this.apiKey = System.getProperty("TRACKER_API_KEY")
        this.apiUsername = System.getProperty("TRACKER_USER")
        this.httpClient = HttpClients.createDefault()
    }



    private static void logRequest(ClassicHttpRequest request) {
        log.debug("Request: {} {}", request.method, request.uri)
    }

    private void checkResponse(ClassicHttpRequest request, ClassicHttpResponse response) {
        log.debug("Response status: {}", response.code)
        log.debug("Response headers: {}", response.headers)

        // Check if response code indicates an error (4xx or 5xx)

        if (response.code >= 400) {
            def errorJson = jsonSlurper.parse(response.entity.content)
            log.debug("Got a failure response (${response.code}) due to ${errorJson} while requesting ${request.method} ${request.uri}")
            throw new DiscourseClientException("Got a failure response due to ${errorJson} while requesting ${request.method} ${request.uri}")
        }

        if (!response.entity || !response.entity.content) {
            log.debug("Response entity is empty")
            throw new DiscourseClientException("Response entity is empty")
        }

    }


    private String buildUri(String path, Map<String, Object> params) {
            def uri = new StringBuilder(baseUrl)
            if (!path.startsWith('/')) {
                uri.append('/')
            }
            uri.append(path)

            if (params) {
                uri.append('?')
                uri.append(params.collect { key, value ->
                    "${URLEncoder.encode(key.toString(), 'UTF-8')}=${URLEncoder.encode(value.toString(), 'UTF-8')}"
                }.join('&'))
            }

            return uri.toString()
        }

    private void addHeaders(ClassicHttpRequest request) {
        request.addHeader('Api-Key', apiKey)
        request.addHeader('Api-Username', apiUsername)
        request.addHeader('Content-Type', 'application/json')
        request.addHeader('Accept', 'application/json')
    }

    @Override
    Map get(String path, Map<String, Object> params = [:]) {

        String uri = buildUri(path, params)
        ClassicHttpRequest request = new HttpGet(uri)


        addHeaders(request)
        logRequest(request)
        ClassicHttpResponse response = httpClient.execute(request)
        if (response.code == 404) {
            log.debug("Got a 404 response when retrieving ${path}")
            return null
        }

        checkResponse(request, response)
        Object json = jsonSlurper.parse(response.entity.content)
        return json as Map

    }
    @Override
    Map post(String path, Object body, Map<String, Object> params = [:]) {
        def request = new HttpPost(buildUri(path, params))


        addHeaders(request)
        String jsonBody = new JsonBuilder(body).toString()
        StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON)
        request.setEntity(entity)

        logRequest(request)
        ClassicHttpResponse response = httpClient.execute(request)
        checkResponse(request, response)
        Object json = jsonSlurper.parse(response.entity.content)
        return json as Map
    }


    @Override
    Map doPut(String path, Object body, Map<String, Object> params = [:]) {
        def request = new HttpPut(buildUri(path, params))


        addHeaders(request)
        String jsonBody = new JsonBuilder(body).toString()
        StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON)
        request.setEntity(entity)

        logRequest(request)
        ClassicHttpResponse response = httpClient.execute(request)
        checkResponse(request, response)
        Object json = jsonSlurper.parse(response.entity.content)
        return json as Map
    }
}