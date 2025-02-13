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
import com.exalate.replication.services.issuetracker.HttpClient
import customconnectornode.discourse.api.DiscourseClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * `DiscourseClientImpl` is an implementation of the `DiscourseClient` interface responsible
 * for interacting with a Discourse API. This class handles HTTP requests (GET, POST, PUT)
 * to the Discourse server and processes responses.
 *
 * Responsibilities include:
 * - Constructing API requests with appropriate headers and parameters
 * - Sending requests through a `HttpClient`
 * - Parsing responses and handling errors
 * - Centralizing the configuration for Discourse API connection (e.g., base URL, API key, etc.)
 */
class DiscourseClientImpl implements DiscourseClient {

    private static final Logger log = LoggerFactory.getLogger(DiscourseClientImpl.class);
    // Logger for recording debug information

    private final HttpClient httpClient; // HTTP client for making requests
    private final String baseUrl;  // Base URL of the Discourse server
    private final JsonSlurper jsonSlurper = new JsonSlurper(); // JSON parser for processing responses

    /**
     * Fetches a configuration parameter either from JVM system properties or environment variables, allowing for flexibility in configuration.
     *
     * @param key The name of the parameter to fetch.
     * @return The value of the parameter or null if not defined.
     */
    private static String getParameter(String key) {
        return System.getProperty(key) ?: System.getenv(key);
    }

    /**
     * Constructor for initializing the Discourse client. Reads essential configuration values
     * (base URL, API key, username) from system properties or environment variables.
     *
     * @param ghc The `HttpClient` instance used to perform HTTP requests, and which is delivered by the application.
     */
    DiscourseClientImpl(HttpClient hc) {
        this.baseUrl = getParameter("TRACKER_URL");
        this.httpClient = hc;

        log.debug("DiscourseClientImpl created with baseUrl: ${baseUrl}");
    }

    /**
     * Logs debug details for an outgoing HTTP request.
     *
     * @param request The HTTP request being sent.
     */
    private static void logRequest(GroovyHttpRequest request) {
        log.debug("Request: {} {}", request.method, request.url);
    }

    /**
     * Validates an HTTP response. Logs response details such as status and body.
     * Throws an exception if the response indicates an error (4xx or 5xx) or if
     * the response body is empty.
     *
     * @param request The HTTP request that triggered the response.
     * @param response The HTTP response to validate.
     * @throws DiscourseClientException if an error occurs in the response.
     */
    private static void checkResponse(GroovyHttpRequest request, GroovyHttpResponse response) {
        log.debug("Response status: {}", response.code);
        log.debug("Response body: {} ...", response.bodyString.take(50));

        // Check for HTTP error codes
        if (response.code >= 400) {
            log.debug("Got a failure response (${response.code}) while requesting ${request.method} ${request.url}");
            throw new DiscourseClientException("Error ${response.code} for ${request.method} ${request.url}");
        }

        // Validate response body
        if (!response.bodyString) {
            log.debug("Response bodyString is empty");
            throw new DiscourseClientException("Response bodyString is empty");
        }
    }

    /**
     * Constructs a complete URI based on the given path and query parameters.  The URL is constructed in a safe way.
     *
     * @param path The API path to append to the base URL (e.g., `/topics`).
     * @param params Optional query parameters to include in the URI.
     * @return The fully constructed URI as a string.
     */
    String buildUri(String path, Map<String, List<String>> params = [:]) {
        def uri = new StringBuilder(baseUrl);
        if (!path.startsWith('/')) {
            uri.append('/');
        }
        uri.append(path);

        if (params) {
            uri.append('?');
            uri.append(params.collect { key, value ->
                "${URLEncoder.encode(key.toString(), 'UTF-8')}=${URLEncoder.encode(value.toString(), 'UTF-8')}"
            }.join('&'));
        }

        return uri.toString();
    }

    /**
     * Constructs request headers for Discourse API requests.
     * Adds authentication headers and standard header fields for JSON communication.
     *
     * @return A map of headers where keys are header names and values are lists of header values.
     */
    private Map<String, List<String>> getHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put('Content-Type', ['application/json']);
        headers.put('Accept', ['application/json']);
        return headers;
    }

    /**
     * Sends a GET request to the specified Discourse API endpoint and retrieves a parsed JSON
     * response.
     *
     * @param path The API endpoint path (e.g., `/users`).
     * @param params Optional query parameters.
     * @return A map containing the parsed JSON response.
     */
    @Override
    Map get(String path, Map<String, List<String>> params = [:]) {
        GroovyHttpRequest request = new GroovyHttpRequest("GET", buildUri(path, params), null, params, getHeaders());
        GroovyHttpResponse response = httpClient.http(request);
        checkResponse(request, response);
        return response?.bodyString ? jsonSlurper.parseText(response.bodyString) as Map : null;
    }

    /**
     * Sends a POST request with a JSON body to the specified Discourse API endpoint.
     *
     * @param path The API endpoint path.
     * @param body The body of the POST request (serialized to JSON).
     * @param params Optional query parameters.
     * @return A map containing the parsed JSON response.
     */
    @Override
    Map post(String path, Object body, Map<String, List<String>> params = [:]) {
        String jsonBody = new JsonBuilder(body).toString();
        GroovyHttpRequest request = new GroovyHttpRequest("POST", buildUri(path, params), jsonBody, params, getHeaders());
        logRequest(request);
        GroovyHttpResponse response = httpClient.http(request);
        checkResponse(request, response);
        return jsonSlurper.parseText(response.bodyString) as Map;
    }

    /**
     * Sends a PUT request with a JSON body to the specified Discourse API endpoint.
     *
     * @param path The API endpoint path.
     * @param body The body of the PUT request (serialized to JSON).
     * @param params Optional query parameters.
     * @return A map containing the parsed JSON response.
     */
    @Override
    Map doPut(String path, Object body, Map<String, List<String>> params = [:]) {
        String jsonBody = new JsonBuilder(body).toString();
        GroovyHttpRequest request = new GroovyHttpRequest("PUT", buildUri(path, params), jsonBody, params, getHeaders());
        logRequest(request);
        GroovyHttpResponse response = httpClient.http(request);
        checkResponse(request, response);
        return jsonSlurper.parseText(response.bodyString) as Map;
    }
}