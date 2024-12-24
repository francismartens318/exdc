package customconnectornode.discourse.http

import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.domain.Topic
import org.apache.hc.core5.http.ClassicHttpResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.client5.http.classic.methods.*
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class DiscourseClientImpl implements DiscourseClient {
    private static final Logger log = LoggerFactory.getLogger(DiscourseClientImpl.class)

    private final CloseableHttpClient httpClient
    private final String baseUrl
    private final String apiKey
    private final String apiUsername
    private final JsonSlurper jsonSlurper = new JsonSlurper()

    DiscourseClientImpl(String baseUrl, String apiKey, String apiUsername) {
        this.baseUrl = baseUrl
        this.apiKey = apiKey
        this.apiUsername = apiUsername
        this.httpClient = HttpClients.createDefault()
    }

    private void addHeaders(ClassicHttpRequest request) {
        request.addHeader('Api-Key', apiKey)
        request.addHeader('Api-Username', apiUsername)
        request.addHeader('Content-Type', 'application/json')
    }

    private static void logRequest(ClassicHttpRequest request) {
        log.debug("Request: {} {}", request.method, request.uri)
    }

    private  void checkResponse(ClassicHttpResponse response) {
        log.debug("Response status: {}", response.code)
        log.debug("Response headers: {}", response.headers)

        // Check if response code indicates an error (4xx or 5xx)

        if (response.code >= 400) {
            def errorJson = jsonSlurper.parse(response.entity.content)
            log.debug("Got a failure response due to ${errorJson}")
            throw new DiscourseClientException("Got a failure response due to ${errorJson}")
        }

    }

    @Override
    Mono<Topic> getTopic(String topicId) {
        def request = new HttpGet("${baseUrl}/t/${topicId}.json")
        addHeaders(request)
        logRequest(request)

        def response = httpClient.execute(request) { response ->
            checkResponse(response)

            def json = jsonSlurper.parse(response.entity.content)
            return Topic.fromJson(json as Map)
        }

        return Mono.just(response)
    }

    @Override
    Mono<Topic> createTopic(Topic topic) {
        log.debug("Creating topic: {}", topic)
        String checkTopic = topic.isValidForCreation()
        if (checkTopic) {
            log.error("Topic ${topic.title} is missing information ${checkTopic}")
            return Mono.error(new Exception("Topic ${topic.toString()} is missing information ${checkTopic}"))
        }

        def request = new HttpPost("${baseUrl}/posts")
        addHeaders(request)
        request.entity = new StringEntity(JsonOutput.toJson(topic))
        logRequest(request)

        def response = httpClient.execute(request) { response ->
            checkResponse(response)

            def json = jsonSlurper.parse(response.entity.content)
            return Topic.fromJson(json as Map)
        }

        return Mono.just(response)
    }

    @Override
    Mono<Topic> updateTopic(String topicId, Topic topic) {
        log.debug("Updating topic: {}", topic)
        def request = new HttpPut("${baseUrl}/t/${topicId}")
        addHeaders(request)
        request.entity = new StringEntity(JsonOutput.toJson(topic))
        logRequest(request)

        def response = httpClient.execute(request) { response ->
            checkResponse(response)

            def json = jsonSlurper.parse(response.entity.content)
            return Topic.fromJson(json as Map)
        }

        return Mono.just(response)
    }

    @Override
    Mono<Void> deleteTopic(String topicId) {
        log.debug("Deleting topic: {}", topicId)
        def request = new HttpDelete("${baseUrl}/t/${topicId}")
        addHeaders(request)
        logRequest(request)

        httpClient.execute(request) { response ->
            checkResponse(response)
        }

        return Mono.empty()
    }

    @Override
    Mono<Topic> createPost(String aTopicId, String rawContent) {
        log.debug("Creating post on ${aTopicId} with ${rawContent?.take(20)} ...")
        def request = new HttpPost("${baseUrl}/posts")

        addHeaders(request)
        def postData = [
            topic_id: aTopicId,
            raw: rawContent
        ]

        request.entity = new StringEntity(JsonOutput.toJson(postData))
        logRequest(request)

        def response = httpClient.execute(request) { response ->
            checkResponse(response)

            def json = jsonSlurper.parse(response.entity.content)
            return Topic.fromJson(json as Map)
        }

        return Mono.just(response)
    }
}