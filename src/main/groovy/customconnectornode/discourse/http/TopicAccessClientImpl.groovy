package customconnectornode.discourse.http

import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.api.TopicAccessClient
import customconnectornode.discourse.domain.Topic
import groovy.json.JsonOutput
import org.apache.hc.client5.http.classic.methods.HttpDelete
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.core5.http.io.entity.StringEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

import java.text.SimpleDateFormat

class TopicAccessClientImpl implements TopicAccessClient {
    private static final Logger log = LoggerFactory.getLogger(TopicAccessClientImpl.class)

    private DiscourseClient discourseClient

    TopicAccessClientImpl(DiscourseClient dc) {
        this.discourseClient = dc
    }



    @Override
    Topic getTopic(String topicId) {
        Map resultJson = discourseClient.get("/t/${topicId}.json")
        return Topic.fromJson(resultJson)
    }

    @Override
    String createTopic(String title, String raw, String category) {
        log.debug("Creating Topic with title ${title} and category ${category}")
        Map createJson = [
                title: title,
                raw: raw,
                category: category
        ]

        Map topicJson = discourseClient.post("/posts", createJson as Object)
        return topicJson.topic_id
    }

    @Override
    Topic updateTopic(String topicId, Topic topic) {
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
    Void deleteTopic(String topicId) {
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
    void addPost(String aTopicId, String rawContent) {
        log.debug("Creating post on ${aTopicId} with ${rawContent?.take(20)} ...")

        Map postData = [
                topic_id: aTopicId,
                raw     : rawContent
        ]

        Map topicJson = discourseClient.post("/posts", postData as Object)
    }


    private Map executeSearchQuery(String queryString, Integer page) {
        String encodedQuery = URLEncoder.encode(queryString, 'UTF-8')
        return discourseClient.get("/search.json?q=${encodedQuery}&page=${page}")
    }

    @Override
    List<String> searchTriggers(String queryString) {
        log.debug("Searching triggers with query: {}", queryString)

        List<String> allTopicIds = []
        Integer currentPage = 0
        boolean hasMorePages = true

        while (hasMorePages) {
            Map searchResult = executeSearchQuery(queryString, currentPage)
            List<String> topicIds = searchResult.topics?.collect { it.id as String }
            allTopicIds.addAll(topicIds)

            Integer totalResults = searchResult.total_results as Integer ?: 0
            Integer perPage = searchResult.per_page as Integer ?: 20

            hasMorePages = (currentPage + 1) * perPage < totalResults
            currentPage++
        }

        return allTopicIds
    }

    @Override
    List<String> latestUpdated(Long since) {
        def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
        def utcDate = sdf.format(new Date(since))
        log.debug("Fetching topics updated since: {}", utcDate.take(10))

        Map searchResult = discourseClient.get("/search.json?q=after:${utcDate.take(10)}")
        return searchResult.topics?.collect { it ->
            if (it.created_at >= utcDate) {
                return it.id as String
            }
        }
    }



}