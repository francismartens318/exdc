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

import com.exalate.domain.http.GroovyHttpRequest
import com.exalate.domain.http.GroovyHttpResponse
import com.exalate.replication.services.issuetracker.HttpClient
import customconnectornode.discourse.TestUtils
import customconnectornode.discourse.domain.DiscourseCategory
import customconnectornode.discourse.http.DiscourseCategoryAccessClientImpl
import customconnectornode.discourse.http.DiscourseClientImpl
import spock.lang.Specification

import java.util.function.Supplier

class DiscourseCategoryAccessClientTest extends Specification {

    DiscourseCategoryAccessClient categoryAccessClient
    HttpClient httpClient

    def setup() {
        TestUtils.setupSpec()
        httpClient = Mock(HttpClient)

        categoryAccessClient = new DiscourseCategoryAccessClientImpl(new DiscourseClientImpl(httpClient))
    }


    private void mockHttpResponses(List<List<String>> methodBodyPairs) {
        Integer requestStep = 0

        httpClient.http(_ as GroovyHttpRequest) >> { GroovyHttpRequest request ->
            // assert that the request is what is expected (based on the requestStep) and that the url is valid (based on the regex)
            assert request.method == methodBodyPairs[requestStep][0]
            assert request.url ==~ /^https?:\/\/[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}(\/\S*)?$/

            def jsonString = methodBodyPairs[requestStep][1]
            def response = new GroovyHttpResponse(
                    200,
                    [:],
                    { -> jsonString } as Supplier<String>,
                    { -> jsonString } as Supplier<Object>
            )

            requestStep++
            return response
        }
    }

    def "should get the categories"() {
        given:

        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/categories.json').text],
        ]

        mockHttpResponses(methodBodyPairs)

        when:
        List<DiscourseCategory> result = categoryAccessClient.fetchAllCategories()
        Integer foundCategoryId = categoryAccessClient.fetchCategoryByName("General")?.id
        String foundCategoryName = categoryAccessClient.fetchCategoryById(4)?.name


        then:
        result.size() == 3
        foundCategoryId == 4
        foundCategoryName == "General"
    }

    def "should only fetch the categories once (and cache it)"() {
        given:

        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/categories.json').text],
                ["SHOULDNOTHAPPEN", getClass().getResource('/json/categories.json').text],    // the second request should not happen
        ]

        mockHttpResponses(methodBodyPairs)

        when:
        List<DiscourseCategory> result = categoryAccessClient.fetchAllCategories()
        result = categoryAccessClient.fetchAllCategories()


        then:
        result.size() == 3
    }
}
