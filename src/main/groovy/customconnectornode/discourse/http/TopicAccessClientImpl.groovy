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

import akka.stream.scaladsl.Source
import akka.util.ByteString

import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.api.domain.twintrace.TraceAction
import com.exalate.api.domain.twintrace.TraceType
import com.exalate.basic.domain.BasicNonPersistentTrace
import com.exalate.basic.domain.hubobject.v1.BasicHubAttachment
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.domain.http.GroovyHttpResponse
import com.exalate.domain.http.MultiPartUploadGroovyHttpRequest
import com.exalate.domain.http.StreamingGroovyHttpResponse
import customconnectornode.discourse.api.DiscourseCategoryAccessClient
import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.api.TopicAccessClient
import customconnectornode.discourse.domain.AttachmentMetaData
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.transform.TopicReplica
import customconnectornode.discourse.transform.Utils
import customconnectornode.domain.StreamableFileMetadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Timestamp
import java.text.SimpleDateFormat

class TopicAccessClientImpl implements TopicAccessClient {
    private static final Logger log = LoggerFactory.getLogger(TopicAccessClientImpl.class)

    private DiscourseClient discourseClient
    private DiscourseCategoryAccessClient categoryAccessClient

    // Constructor to initialize the client with the DiscourseClient dependency.
    TopicAccessClientImpl(DiscourseClient dc, DiscourseCategoryAccessClient categoryAccessClient) {
        this.discourseClient = dc
        this.categoryAccessClient = categoryAccessClient
    }


    void fixCategory(Topic topic) {
        if (!topic?.category_id && !topic?.category) {
            throw new DiscourseClientException("Topic has no category specified")
        }

        topic.category = topic.category ?: categoryAccessClient.fetchCategoryById(topic.category_id)?.name
        topic.category_id = topic.category_id ?: categoryAccessClient.fetchCategoryByName(topic.category)?.id
    }

    /**
     * Fetches a specific topic by its unique identifier.
     *
     * @param topicId The ID of the topic to be fetched.
     * @return A Topic object containing the topic's data, or null if the topic is not found or the response is empty.
     */
    @Override
    Topic getTopic(String topicId) {
        Map resultJson = null
        String topicUrl = "/t/${topicId}.json"
        try {
            resultJson = discourseClient.get(topicUrl, [:])
        } catch (DiscourseClientException e) {
            log.debug("Getting topic ${topicId} failed with error ${e.message}, returning null")
            return null
        }

        if (!resultJson) return null
        Topic topic = Topic.fromJson(resultJson, discourseClient.buildUri(topicUrl, [:]))
        fixCategory(topic)
        return topic
    }

    /**
     * Creates a new topic in Discourse.
     *
     * @param topic A Topic object containing the details of the topic to be created.
     * @return The created Topic object.
     */
    Topic create(Topic topic) {
        fixCategory(topic)
        Map createJson = [
                title: topic.title,
                raw: topic.raw,
                category: topic.category_id

        ]

        Map resultJson = discourseClient.post("/posts.json", createJson as Object, [:])
        return Topic.fromJson(resultJson)
    }

    /**
     * Updates the content of the first post in a topic if any relevant fields have changed.
     *
     * @param oldTopic The existing topic to compare with.
     * @param newTopic The updated topic containing the new data.
     */
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

    /**
     * Updates the metadata of a topic (such as category, tags, or title) if any fields have changed.
     *
     * @param oldTopic The existing topic to compare with.
     * @param newTopic The updated topic containing the new data.
     */
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

    /**
     * Merges posts from the new version of a topic with the existing one, adding new posts as necessary.
     *
     * @param oldTopic The existing topic to compare with.
     * @param newTopic The updated topic with potential new posts.
     * @param traces A list of trace objects representing operations on the topic.
     * @return The updated list of traces after merging posts.
     */
    private List<INonPersistentTrace> mergeTopicPosts(Topic oldTopic, Topic newTopic, List<INonPersistentTrace> traces) {
        int counter
        // For all comments in newTopic without an id - assuming it is new, add a post to the topic, ignoring the first one
        newTopic.posts?.each { post ->
            if (counter++ > 0 && !post.id) {
                String localPostId = addPost(newTopic.topic_id, post.raw)

                // only add a trace if the post has a remote_id
                if (post.remote_id) {
                    traces.add(TopicReplica.toCommentTrace(localPostId, post.remote_id))
                }
            }
        }

        return traces

        // TODO: Implement the deletion of posts that have been removed in the newTopic
    }




    private static StreamableFileMetadata findFileMetadata(List<StreamableFileMetadata> fileMetadataList, String attachmentRemoteId) {
        fileMetadataList.find { fileMetaData ->  fileMetaData.blobMetaData().blobId == attachmentRemoteId }
    }

    private List<INonPersistentTrace> mergeTopicAttachments(Topic newTopic, List<INonPersistentTrace> traces) {
        return traces

    }

    /**
     * Updates the specified topic, applying changes to the post content and topic metadata.
     * Also handles post traces.
     *
     * @param topic A Topic object containing the updated topic data.
     * @param traces A list of trace objects representing operations on the topic.
     * @return A map including the updated topic and updated traces.
     * @throws DiscourseClientException If the topic lacks an ID or posts.
     */
    @Override
    Map<String, Object> update(Topic topic, List<INonPersistentTrace> traces) {
        log.debug("Updating topic: {} taking into account the traces {}", topic, traces)

        if (!topic.id || topic.posts?.size() < 1) {
            throw new DiscourseClientException("Trying to update a topic without an id - was it created first? (Topic: ${topic})")
        }

        // Fetch the existing topic to compare changes
        Topic oldTopic = getTopic(topic.topic_id)
        traces = mergeTopicAttachments(topic, traces)

        // Update both the posts and metadata of the topic
        traces = mergeTopicPosts(oldTopic, topic, traces)
        updateTopicPost(oldTopic, topic)
        updateTopicMeta(oldTopic, topic)

        return ([topic: getTopic(topic.topic_id), traces: traces])
    }

    /**
     * Adds a new post to the given topic.
     *
     * @param aTopicId The ID of the topic to which the post will be added.
     * @param rawContent The raw content of the new post.
     * @return The unique ID of the newly added post.
     */
    @Override
    String addPost(String aTopicId, String rawContent) {
        log.debug("Creating post on ${aTopicId} with ${rawContent?.take(20)} ...")

        Map postData = [
                topic_id: aTopicId,
                raw     : rawContent
        ]

        Map topicJson = discourseClient.post("/posts", postData as Object)
        return topicJson.id as String
    }

    /**
     * Executes a search query on Discourse with pagination support.
     * This is a helper method for the public `search` method below.
     *
     * @param queryString The query string to execute.
     * @param page The page number for paginated results.
     * @return A map of search results.
     */
    private Map executeSearchQuery(String queryString, Integer page) {
        String encodedQuery = URLEncoder.encode(queryString, 'UTF-8')
        return discourseClient.get("/search.json?q=${encodedQuery}&page=${page}")
    }

    /**
     * Searches for topics using a specific query and optionally filters based on a timestamp.
     *
     * @param query The search query string for finding topics.
     * @param since Filters results to topics updated or created after this timestamp.
     * @return A list of Topic objects matching the search criteria.
     */
    private static List<Integer> getAllowedCategoryIds() {
        String categoryIds = System.getProperty("TRACKER_CATEGORY_IDS") ?: System.getenv("TRACKER_CATEGORY_IDS")
        if (!categoryIds) {
            return []
        }
        return categoryIds.split(",").collect { it.trim().toInteger() }
    }

    @Override
    List<Topic> search(String query, Timestamp since) {
        List<Integer> allowedCategoryIds = getAllowedCategoryIds()

        // Compose the search query
        if(query.equals("dummy=1")){
            query=null
        }
        String searchQuery = query ?: ""
        String utcDate

        // Add a timestamp filter if provided
        if (since) {
            def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
            utcDate = sdf.format(new Date(since.getTime()))

            // Adjust query to filter results updated after the specified date
            String separator = searchQuery ? ' ' : ''
            searchQuery = "${searchQuery}${separator}after:${utcDate.take(10)}"
        }

        if (!searchQuery) {
            // Return empty list for empty queries
            return []
        }

        String encodedQuery = URLEncoder.encode(searchQuery, 'UTF-8')
        log.debug("Fetching topics using the query ${searchQuery}")

        Map searchResult = discourseClient.get("/search.json?q=${encodedQuery}", [:])

        log.debug("Search resulted in ${searchResult?.topics?.size() ?: 0} entries (not yet filtered)")

        // Collect and filter topics based on the timestamp and allowed categories
        return searchResult.topics?.collect { Topic.fromJson(it as Map) }?.findAll { topic ->
            (topic.last_posted_at >= utcDate || topic.created_at >= utcDate) &&
            (!allowedCategoryIds || allowedCategoryIds.contains(topic.category_id))
        }
    }


    StreamingGroovyHttpResponse downloadAttachment(String fileId) {
        return discourseClient.download("/uploads/default/original/1X/${fileId}")
    }

    /**
     * Uploads an attachment via the DiscourseClient implementation.
     *
     * @param parts A list of form parts for the multipart upload.
     * @param entityId The ID of the entity to which the attachment belongs.
     * @return An instance of Map containing the server response.
     */
    Map uploadAttachment(List<MultiPartUploadGroovyHttpRequest.IFormPart> parts, String entityId) {
        return discourseClient.uploadAttachment("/uploads", parts, [:])
    }
    /**
     * Retrieves metadata about an attachment file, by accessing the file ...
     *
     *
     * @param fileId
     * @return
     */

    AttachmentMetaData getAttachmentMetadata(String fileId) {
        // TODO: this is a workaround until the attachment metadata is available in the API, an alternative is to cache the information

        Map responseHeaders = discourseClient.getResponseHeaders("/uploads/default/original/1X/${fileId}")

        String mimeType = responseHeaders.get("Content-Type")?.first()
        Long fileSize = responseHeaders.get("content-length")?.first()?.toLong()

        return new AttachmentMetaData().builder()
                    .mimeType(mimeType)
                    .fileName(fileId as String)
                    .fileSize(fileSize)
                    .lastModified(Utils.getDateFromString(responseHeaders.get("Last-Modified")?.first() as String))
                    .build()
    }
}