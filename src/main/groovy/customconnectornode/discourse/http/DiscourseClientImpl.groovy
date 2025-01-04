/*
 * Copyright (c) 2024 Exalate (https://exalate.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package customconnectornode.discourse.http

import com.exalate.domain.http.GroovyHttpRequest
import com.exalate.domain.http.GroovyHttpResponse
import com.exalate.replication.services.issuetracker.GroovyHttpClient
import customconnectornode.discourse.api.DiscourseClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.hc.client5.http.classic.methods.HttpPut
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



    private final GroovyHttpClient groovyHttpClient
    private final String baseUrl
    private final String apiKey
    private final String apiUsername
    private final JsonSlurper jsonSlurper = new JsonSlurper()


    private static String getParameter(String key) {
        return System.getProperty(key) ?: System.getenv(key)
    }

   DiscourseClientImpl(GroovyHttpClient ghc) {
       this.baseUrl = getParameter("TRACKER_URL")
       this.apiKey = getParameter("TRACKER_API_KEY")
       this.apiUsername = getParameter("TRACKER_USER")

       this.groovyHttpClient = ghc

       log.debug("DiscourseClientImpl created with baseUrl: ${baseUrl}, apiKey: ${apiKey}.take(3), apiUsername: ${apiUsername}")
    }



    private static void logRequest(GroovyHttpRequest request) {
        log.debug("Request: {} {}", request.method, request.url)
    }

    private static void checkResponse(GroovyHttpRequest request, GroovyHttpResponse response) {
        log.debug("Response status: {}", response.code)
        log.debug("Response headers: {}", response.headers)

        // Check if response code indicates an error (4xx or 5xx)

        if (response.code >= 400) {
            log.debug("Got a failure response (${response.code}) due to ... while requesting ${request.method} ${request.uri}")
            throw new DiscourseClientException("Got a failure response due to .... while requesting ${request.method} ${request.uri}")
        }

        if (!response.bodyString ) {
            log.debug("Response bodyString is empty")
            throw new DiscourseClientException("Response bodyString is empty")
        }

    }


    String buildUri(String path, Map<String, List<String>> params = [:]) {
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

    private Map<String, List<String>> getHeaders() {
        Map<String, List<String>> headers = new HashMap<>()


        headers.put('Api-Key',  [ apiKey ] )
        headers.put('Api-Username', [ apiUsername ])
        headers.put('Content-Type', [ 'application/json' ])
        headers.put('Accept', [ 'application/json' ])
        return headers
    }

    @Override
    Map get(String path, Map<String, List<String>> params = [:]) {

        GroovyHttpRequest request = new GroovyHttpRequest("GET", buildUri(path, params), null, params, getHeaders())
        GroovyHttpResponse response = groovyHttpClient.http(request)
        checkResponse(request, response)

        return response?.bodyString ? jsonSlurper.parseText(response.bodyString) as Map : null
    }

    @Override
    Map post(String path, Object body, Map<String, List<String>> params = [:]) {
        String jsonBody = new JsonBuilder(body).toString()
        GroovyHttpRequest request = new GroovyHttpRequest("POST", buildUri(path, params), jsonBody, params, getHeaders())
        logRequest(request)

        GroovyHttpResponse response = groovyHttpClient.http(request)
        checkResponse(request, response)
        return jsonSlurper.parseText(response.bodyString) as Map
    }


    @Override
    Map doPut(String path, Object body, Map<String, List<String>> params = [:]) {
        String jsonBody = new JsonBuilder(body).toString()
        GroovyHttpRequest request = new GroovyHttpRequest("PUT", buildUri(path, params), jsonBody, params, getHeaders())
        logRequest(request)
        GroovyHttpResponse response = groovyHttpClient.http(request)
        checkResponse(request, response)
        return jsonSlurper.parseText(response.bodyString) as Map
    }
}