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
        return resultJson ? Topic.fromJson(resultJson) : null
    }



    Topic create(Topic topic) {
        Map createJson = [
                title: topic.title,
                raw: topic.raw,
                category: topic.category
        ]

        return Topic.fromJson(discourseClient.post("/posts", createJson as Object))

    }


    // update the topic first post if the title, raw or cooked fields have changed
    private void updateTopicPost(Topic oldTopic, Topic newTopic) {
        if (newTopic.posts?.size() < 1) {
            log.debug("Topic has no posts - probably a new topic - skipping update")
            return
        }

        Map updatedFields = [:]

        if (oldTopic.title != newTopic.title) updatedFields.title = newTopic.title
        if (oldTopic.raw != newTopic.raw) updatedFields.raw = newTopic.raw

        if (updatedFields.isEmpty()) {
            log.debug("No post fields to update for topic: {}", newTopic)
            return
        }

        log.debug("Updating topic post (topic ${newTopic} with fields ${updatedFields.keySet()}")

        Long postId = newTopic.posts?.first()?.id

        discourseClient.doPut("/posts/${postId}", updatedFields)
    }

    // update the topic meta if the category, tags or title have changed
    private void updateTopicMeta(Topic oldTopic, Topic newTopic) {
        Map updatedFields = [:]
        if (oldTopic.category != newTopic.category) updatedFields.category = newTopic.category
        if (oldTopic.tags != newTopic.tags) updatedFields.tags = newTopic.tags
        if (oldTopic.title != newTopic.title) updatedFields.title = newTopic.title
        if (updatedFields.isEmpty()) {
            log.debug("No topic fields to update for topic: {}", newTopic)
            return
        }

        log.debug("Updating topic meta (topic ${newTopic} with fields ${updatedFields.keySet()}")
        discourseClient.doPut("/t/${newTopic.topic_id}", updatedFields)
    }

    private void mergeTopicPosts(Topic oldTopic, Topic newTopic) {
        // For all comments in newTopic without an id - assuming it is new, add a post to the topic
        newTopic.posts?.each { post ->
            if (!post.id) {
                addPost(newTopic.topic_id, post.raw)
            }
        }

        // TODO - delete posts which have been deleted in newTopic
    }

    @Override
    Topic update(Topic topic) {


        log.debug("Updating topic: {}", topic)
        if (!topic.id) {
            throw new DiscourseClientException("Trying to update a topic without an id - was it created first? (Topic: ${topic})")
        }

        if (topic.posts?.size() < 1) {
            // looks like a new topic, so this is an inappropriate call as the post doesn't exist yet
            throw new DiscourseClientException("Trying to update a topic without a post - was it created first? (Topic: ${topic})")
        }

        // updating a topic is about updating the content of the first post
        Topic oldTopic = getTopic(topic.topic_id)

        // some fields need to be updated in the topic itself, other fields need to be updated in the first post

        mergeTopicPosts(oldTopic, topic)
        updateTopicPost(oldTopic, topic)
        updateTopicMeta(oldTopic, topic)


        return (getTopic(topic.topic_id))

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

        // return the topic ids created after 'since'.
        return searchResult.topics?.findAll() {it.created_at >= utcDate }?.collect { it.id as String }
    }



}