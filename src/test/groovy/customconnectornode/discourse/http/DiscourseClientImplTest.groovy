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
import com.exalate.domain.http.MultiPartUploadGroovyHttpRequest
import com.exalate.domain.http.StreamingGroovyHttpResponse
import com.exalate.replication.services.issuetracker.HttpClient
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Supplier

class DiscourseClientImplTest extends Specification {

    @Subject
    DiscourseClientImpl discourseClient

    HttpClient httpClient

    def setup() {
        // Set system property for testing
        System.setProperty("TRACKER_URL", "https://example.com")
        
        httpClient = Mock(HttpClient)
        discourseClient = new DiscourseClientImpl(httpClient)
    }

    def cleanup() {
        System.clearProperty("TRACKER_URL")
    }

    def "buildUri should construct a valid URI with path"() {
        when:
        def result = discourseClient.buildUri("/test")

        then:
        result == "https://example.com/test"
    }

    def "buildUri should handle paths without leading slash"() {
        when:
        def result = discourseClient.buildUri("test")

        then:
        result == "https://example.com/test"
    }

    def "buildUri should include query parameters"() {
        when:
        def result = discourseClient.buildUri("/test", [param1: ["value1"], param2: ["value2"]])

        then:
        result == "https://example.com/test?param1=%5Bvalue1%5D&param2=%5Bvalue2%5D"
    }



    def "get should throw exception when response code is 400 or higher"() {
        given:
        def responseBody = '{"error": "Not found"}'
        def response = new GroovyHttpResponse(
                404,
                [:],
                { -> responseBody } as Supplier<String>,
                { -> responseBody } as Supplier<Object>
        )
        httpClient.http(_ as GroovyHttpRequest) >> response

        when:
        discourseClient.get("/test")

        then:
        thrown(DiscourseClientException)
    }

    def "get should throw exception when response body is empty"() {
        given:
        def response = new GroovyHttpResponse(
                200,
                [:],
                { -> "" } as Supplier<String>,
                { -> "" } as Supplier<Object>
        )
        httpClient.http(_ as GroovyHttpRequest) >> response

        when:
        discourseClient.get("/test")

        then:
        thrown(DiscourseClientException)
    }

    def "getResponseHeaders should return headers from response"() {
        given:
        def headers = ["Content-Type": ["application/json"]]
        def response = new GroovyHttpResponse(
                200,
                headers,
                { -> "{}" } as Supplier<String>,
                { -> "{}" } as Supplier<Object>
        )
        httpClient.http(_ as GroovyHttpRequest) >> response

        when:
        def result = discourseClient.getResponseHeaders("/test")

        then:
        result == headers
    }


    def "download should throw exception when response code is 400 or higher"() {
        given:
        def response = Mock(StreamingGroovyHttpResponse)
        response.code >> 404
        response.source >> "source"
        httpClient.download(_ as GroovyHttpRequest) >> response

        when:
        discourseClient.download("/test")

        then:
        thrown(DiscourseClientException)
    }

    def "download should throw exception when response source is null"() {
        given:
        def response = Mock(StreamingGroovyHttpResponse)
        response.code >> 200
        response.source >> null
        httpClient.download(_ as GroovyHttpRequest) >> response

        when:
        discourseClient.download("/test")

        then:
        thrown(DiscourseClientException)
    }


}