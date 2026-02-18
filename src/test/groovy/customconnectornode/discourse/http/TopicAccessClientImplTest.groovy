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

import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.domain.http.GroovyHttpResponse
import com.exalate.domain.http.StreamingGroovyHttpResponse
import customconnectornode.discourse.api.DiscourseCategoryAccessClient
import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.domain.Topic
import customconnectornode.domain.StreamableFileMetadata
import spock.lang.Specification
import spock.lang.Subject

import java.sql.Timestamp
import java.util.function.Supplier

class TopicAccessClientImplTest extends Specification {

    @Subject
    TopicAccessClientImpl topicAccessClient

    DiscourseClient discourseClient
    DiscourseCategoryAccessClient categoryAccessClient

    def setup() {
        discourseClient = Mock(DiscourseClient)
        categoryAccessClient = Mock(DiscourseCategoryAccessClient)
        topicAccessClient = new TopicAccessClientImpl(discourseClient, categoryAccessClient)
    }

    def "fixCategory should throw exception when topic has no category or category_id"() {
        given:
        Topic topic = new Topic()

        when:
        topicAccessClient.fixCategory(topic)

        then:
        thrown(DiscourseClientException)
    }

    def "fixCategory should set category when only category_id is provided"() {
        given:
        Topic topic = new Topic().builder().category_id(4).build()
        categoryAccessClient.fetchCategoryById(4) >> [name: "General"]

        when:
        topicAccessClient.fixCategory(topic)

        then:
        topic.category == "General"
        topic.category_id == 4
    }

    def "fixCategory should set category_id when only category is provided"() {
        given:
        Topic topic = new Topic().builder().category("General").build()
        categoryAccessClient.fetchCategoryByName("General") >> [id: 4]

        when:
        topicAccessClient.fixCategory(topic)

        then:
        topic.category == "General"
        topic.category_id == 4
    }


    def "getTopic should return null when resultJson is null"() {
        given:
        discourseClient.get("/t/123.json", [:]) >> null

        when:
        def result = topicAccessClient.getTopic("123")

        then:
        result == null
    }

    def "getTopic should return topic when resultJson is valid"() {
        given:
        def resultJson = [
            id: 123,
            topic_id: "123",
            title: "Test Topic",
            category_id: 4
        ]
        discourseClient.get("/t/123.json", [:]) >> resultJson
        discourseClient.buildUri("/t/123.json", [:]) >> "https://example.com/t/123.json"
        categoryAccessClient.fetchCategoryById(4) >> [name: "General"]

        when:
        def result = topicAccessClient.getTopic("123")

        then:
        result != null
        result.id == 123
        result.topic_id == "123"
        result.title == "Test Topic"
        result.category_id == 4
        result.category == "General"
    }

    def "create should create a new topic"() {
        given:
        Topic topic = new Topic().builder()
            .title("Test Topic")
            .raw("Test content")
            .category_id(4)
            .category("General")
            .build()

        def resultJson = [
            id: 123,
            topic_id: "123",
            title: "Test Topic",
            category_id: 4
        ]
        discourseClient.post("/posts.json", [title: "Test Topic", raw: "Test content", category: 4], [:]) >> resultJson

        when:
        def result = topicAccessClient.create(topic)

        then:
        result != null
    }

    def "updateTopicPost should not update when topic has no posts"() {
        given:
        Topic oldTopic = new Topic().builder().build()
        Topic newTopic = new Topic().builder().build()

        when:
        topicAccessClient.updateTopicPost(oldTopic, newTopic)

        then:
        0 * discourseClient.doPut(_, _)
    }


    def "updateTopicMeta should not update when no fields have changed"() {
        given:
        Topic oldTopic = new Topic().builder()
            .title("Test Topic")
            .category_id(4)
            .build()
        oldTopic.tags = ["tag1", "tag2"]

        Topic newTopic = new Topic().builder()
            .title("Test Topic")
            .category_id(4)
            .build()
        newTopic.tags = ["tag1", "tag2"]

        when:
        topicAccessClient.updateTopicMeta(oldTopic, newTopic)

        then:
        0 * discourseClient.doPut(_, _)
    }

    def "search should return empty list when query is empty and since is null"() {
        when:
        def result = topicAccessClient.search("", null)

        then:
        result == []
    }

    def "search should return empty list when query is dummy=1 and since is null"() {
        when:
        def result = topicAccessClient.search("dummy=1", null)

        then:
        result == []
    }

    def "search should filter topics by allowed category ids"() {
        given:
        System.setProperty("TRACKER_CATEGORY_IDS", "7,22")
        def since = Timestamp.valueOf("2026-02-01 00:00:00")
        def searchResult = [
            topics: [
                [id: 1, title: "Allowed topic", category_id: 7, created_at: "2026-02-05T10:00:00.000Z", last_posted_at: "2026-02-05T10:00:00.000Z"],
                [id: 2, title: "Blocked topic", category_id: 27, created_at: "2026-02-05T10:00:00.000Z", last_posted_at: "2026-02-05T10:00:00.000Z"],
                [id: 3, title: "Another allowed topic", category_id: 22, created_at: "2026-02-05T10:00:00.000Z", last_posted_at: "2026-02-05T10:00:00.000Z"]
            ]
        ]
        discourseClient.get(_, _) >> searchResult

        when:
        def result = topicAccessClient.search("dummy=1", since)

        then:
        result.size() == 2
        result.collect { it.id } == [1, 3]

        cleanup:
        System.clearProperty("TRACKER_CATEGORY_IDS")
    }

    def "search should return all topics when TRACKER_CATEGORY_IDS is not set"() {
        given:
        System.clearProperty("TRACKER_CATEGORY_IDS")
        def since = Timestamp.valueOf("2026-02-01 00:00:00")
        def searchResult = [
            topics: [
                [id: 1, title: "Topic 1", category_id: 7, created_at: "2026-02-05T10:00:00.000Z", last_posted_at: "2026-02-05T10:00:00.000Z"],
                [id: 2, title: "Topic 2", category_id: 27, created_at: "2026-02-05T10:00:00.000Z", last_posted_at: "2026-02-05T10:00:00.000Z"]
            ]
        ]
        discourseClient.get(_, _) >> searchResult

        when:
        def result = topicAccessClient.search("dummy=1", since)

        then:
        result.size() == 2
    }

    def "search should use space separator for after clause in query"() {
        given:
        System.clearProperty("TRACKER_CATEGORY_IDS")
        def since = Timestamp.valueOf("2026-02-01 00:00:00")
        def capturedUrl = null
        discourseClient.get(_, _) >> { args ->
            capturedUrl = args[0]
            return [topics: []]
        }

        when:
        topicAccessClient.search("test query", since)

        then:
        capturedUrl != null
        // after: should be inside the q parameter (space-separated), not a separate URL param
        !(capturedUrl as String).contains("&after")
        URLDecoder.decode(capturedUrl as String, 'UTF-8').contains("test query after:")
    }

    def "downloadAttachment should delegate to discourseClient"() {
        given:
        def response = Mock(StreamingGroovyHttpResponse)
        discourseClient.download("/uploads/default/original/1X/abc123") >> response

        when:
        def result = topicAccessClient.downloadAttachment("abc123")

        then:
        result == response
    }

}