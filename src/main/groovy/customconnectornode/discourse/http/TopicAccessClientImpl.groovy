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

import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.api.TopicAccessClient
import customconnectornode.discourse.domain.Topic
import org.apache.hc.client5.http.classic.methods.HttpDelete
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

class TopicAccessClientImpl implements TopicAccessClient {
    private static final Logger log = LoggerFactory.getLogger(TopicAccessClientImpl.class)

    private DiscourseClient discourseClient

    TopicAccessClientImpl(DiscourseClient dc) {
        this.discourseClient = dc
    }



    @Override
    Topic getTopic(String topicId) {
        String topicUrl = "/t/${topicId}.json"


        Map resultJson = discourseClient.get(topicUrl, [:])
        return resultJson ? Topic.fromJson(resultJson, discourseClient.buildUri(topicUrl, [:])) : null
    }



    Topic create(Topic topic) {
        Map createJson = [
                title: topic.title,
                raw: topic.raw,
                category: topic.category
        ]

        return Topic.fromJson(discourseClient.post("/posts", createJson as Object, [:]))

    }


    // update the topic first post if the title, raw or cooked fields have changed
    private void updateTopicPost(Topic oldTopic, Topic newTopic) {
        if (newTopic.posts?.size() < 1 || newTopic.posts?.first()?.id == null) {
            log.debug("Topic has no posts - probably a new topic - skipping update. Also applies when the post is null")
            return
        }


        Map updatedFields = [:]

        if (oldTopic.cooked != newTopic.cooked && newTopic.raw) updatedFields.raw = newTopic.raw

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
        if (oldTopic.category_id != newTopic.category_id) updatedFields.category_id = newTopic.category_id
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
        int counter
        // For all comments in newTopic without an id - assuming it is new, add a post to the topic, ignore the first one
        newTopic.posts?.each { post ->
            if (counter++ > 0 && !post.id) {
                addPost(newTopic.topic_id, post.raw)
            }
        }

        // TODO - delete posts which have been deleted in newTopic
    }

    @Override
    Topic update(Topic topic) {


        log.debug("Updating topic: {}", topic)
        if (!topic.id || topic.posts?.size() < 1) {
            throw new DiscourseClientException("Trying to update a topic without an id - was it created first? (Topic: ${topic})")
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
    List<Topic> search(String query, Timestamp since) {
        // compose the search query

        String encodedQuery = query ? URLEncoder.encode(query, 'UTF-8') : ""
        String separator = encodedQuery ? '&' : ''
        String utcDate


        // if there is a timestamp, add it to the query
        if (since) {
            def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
            utcDate = sdf.format(new Date(since.getTime()))

            // query for all results updated after midnight of the given date, as Discourse doesn't allow for a time based query
            encodedQuery = "${encodedQuery}${separator}after:${utcDate.take(10)}"
        }

        if (!encodedQuery) {
            // an empty query ...
            return []
        }

        log.debug("Fetching topics using the query ${encodedQuery}")

        Map searchResult = discourseClient.get("/search.json?q=${encodedQuery}", [:])


        // return the topic ids created after 'since' (if given) and matching the query.
        return searchResult.topics?.collect { Topic.fromJson(it) }?.findAll { topic ->
            topic.last_posted_at >= utcDate || topic.created_at >= utcDate
        }
    }


}