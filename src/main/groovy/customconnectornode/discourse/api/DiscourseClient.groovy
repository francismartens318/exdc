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

package customconnectornode.discourse.api

import com.exalate.domain.http.MultiPartUploadGroovyHttpRequest
import com.exalate.domain.http.StreamingGroovyHttpResponse


interface DiscourseClient {

    /**
     * Sends a GET request to the specified resource path with the provided query parameters.
     *
     * @param path The API resource path to send the GET request to (e.g., "/topics").
     * @param params A map of query parameters to include in the request (key-value pairs, where the value can be a list of values for the same key).
     * @return A Map object containing the response data received from the API server.
     */
    Map get(String path, Map<String, List<String>> params)

    /**
     * TODO document the method
     * @param path
     * @param params
     * @return
     */

    Map getResponseHeaders(String path, Map<String, List<String>> params)

    /**
     * Sends a POST request to the specified resource path with the provided payload and query parameters.
     *
     * @param path The API resource path to send the POST request to (e.g., "/posts").
     * @param payload The payload to be sent in the request body (typically for creating or submitting new data).
     * @param params A map of query parameters to include in the request (key-value pairs, where the value can be a list of values for the same key).
     * @return A Map object containing the response data received from the API server.
     */
    Map post(String path, Object payload, Map<String, List<String>> params)

    /**
     * Sends a PUT request to the specified resource path with the provided payload and query parameters.
     *
     * @param path The API resource path to send the PUT request to (e.g., "/users/123").
     * @param payload The payload to be sent in the request body (typically for updating existing data).
     * @param params A map of query parameters to include in the request (key-value pairs, where the value can be a list of values for the same key).
     * @return A Map object containing the response data received from the API server.
     */
    Map doPut(String path, Object payload, Map<String, List<String>> params)

    /**
     * Constructs a full URI (Uniform Resource Identifier) for the given resource path and query parameters.
     *
     * @param path The base resource path (e.g., "/categories").
     * @param params A map of query parameters to be appended to the URI as query strings.
     * @return A String containing the complete URI with the path and encoded query parameters.
     */
    String buildUri(String path, Map<String, List<String>> params)

    StreamingGroovyHttpResponse download(String path)

    Map uploadAttachment(List<MultiPartUploadGroovyHttpRequest.IFormPart> parts)

}