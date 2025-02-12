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

package customconnectornode.discourse

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.connection.IConnection
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.api.exception.CategorizedException
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.replication.services.issuetracker.HttpClient
import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.api.TopicAccessClient
import customconnectornode.discourse.domain.Post
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.http.DiscourseClientException
import customconnectornode.discourse.http.DiscourseClientImpl
import customconnectornode.discourse.http.TopicAccessClientImpl
import customconnectornode.discourse.transform.TopicReplica
import customconnectornode.domain.EntityKeyContext
import customconnectornode.domain.EntityWriteResult
import customconnectornode.domain.PageRequest
import customconnectornode.domain.PageResponse
import customconnectornode.domain.StreamableFileMetadata
import customconnectornode.services.api.IIssueTrackerApi
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.Application

import javax.annotation.Nonnull

class DiscourseApi implements IIssueTrackerApi {
    private static final Logger log = LoggerFactory.getLogger(DiscourseApi.class)

    private final Application application
    private final HttpClient httpClient
    private final DiscourseClient discourseClient
    private final TopicAccessClient topicAccessClient

    TopicAccessClient getTopicAccessClient() {
        return topicAccessClient
    }

    DiscourseApi(Application application) {
        log.debug("Constructing customconnectornode.discourse.DiscourseApi")
        this.application = application
        this.httpClient = application.injector().instanceOf(HttpClient.class)
        this.discourseClient = new DiscourseClientImpl(httpClient)
        this.topicAccessClient = new TopicAccessClientImpl(discourseClient)
    }

    @Override
    PageResponse<EntityType> searchEntityTypes(@Nullable @javax.annotation.Nullable String query, @NotNull @Nonnull PageRequest pageRequest) throws CategorizedException {
        log.debug("Searching entity types with query: {} and page request: {}", query, pageRequest)
        // Return basic topic type for now
        PageResponse<EntityType> response = new PageResponse<EntityType>(
                pageRequest,
                [new EntityType("topic", true, true)],
                true
        )
        log.debug("Search entity types response : {}", response)

        return response
    }

    @Override
    IHubIssueReplica readEntity(@NotNull @Nonnull IIssueKey entityKey) throws CategorizedException {
        log.debug("Attempting to read entity topic with key: {}", entityKey)


        if (entityKey.entityType != "topic") {
            // only handle topics for now

            def message = "Asked to retrieve an entity of type ${entityKey.entityType} but only topics are supported"
            log.error(message)
            throw new DiscourseClientException(message)
        }

        // fetch the topic from the Discourse API

        Topic topic = topicAccessClient.getTopic(entityKey.getURN())

        return topic ?  TopicReplica.toReplica(topic) : null
    }

    @Override
    EntityWriteResult writeEntity(@Nullable @javax.annotation.Nullable IIssueKey entityKey, @NotNull @Nonnull IHubIssueReplica entityBeforeScript, @NotNull @Nonnull IHubIssueReplica entityAfterScript, @NotNull @Nonnull List<INonPersistentTrace> traces, @NotNull @Nonnull List<StreamableFileMetadata> blobMetadataList) throws CategorizedException {
        Topic changeTopic = TopicReplica.toTopic((BasicHubIssue) entityAfterScript)

        // First create a topic from the entityAfterScript.  This will be used to either create or update the topic
        // in Discourse.
        if  (entityKey == null) {
            // if there is no entity key - first create the entity
            // note that additional information from the hubIssue, such as comments, tags etc will be added during the update step
            log.debug("Entity key is empty, creating new topic using title, description and category ")

            // the topicAccessClient.create method needs a topic structure to create the topic
            Topic toCreateTopic = TopicReplica.toTopic((BasicHubIssue) entityAfterScript)

            // after creating the topic, we need to get the topic so that we have a fully populated topic object (including the underlying posts)
            String createdTopicId = topicAccessClient.create(toCreateTopic)?.topic_id
            Topic createdTopic = topicAccessClient.getTopic(createdTopicId)

            // populate the acquired fields id and topic_id
            changeTopic.id = createdTopic.id
            changeTopic.topic_id = createdTopic.topic_id

            List<Post> buildPosts = []
            // add the created topic to the list of posts, pass on an empty list of posts if the member is null
            buildPosts.addAll(createdTopic.posts ?: [])
            // add the changeTopic to the list of posts, but drop the first element (which is the topic itself)
            buildPosts.addAll(changeTopic.posts.drop(1) ?: [])
            changeTopic.posts = buildPosts
        }


        log.debug("Writing entity with key: {}", entityKey)
        Map<String,Object> updatedResult = topicAccessClient.update(changeTopic, traces)

        return new EntityWriteResult(TopicReplica.toReplica(updatedResult.topic as Topic), traces)
    }


    @Override
    PageResponse<IIssueKey> search(@Nullable String query, @Nullable Date since, @Nullable EntityKeyContext entityKeyContext, @Nullable PageRequest pageRequest) throws CategorizedException {
        log.debug("Search called Query ${query} - ${since} - ${entityKeyContext} - ${pageRequest}")
        if (entityKeyContext && entityKeyContext.entityTypeName != "topic") {
            throw new DiscourseClientException("Asked to search for an entity of type ${entityKeyContext.entityTypeName} but only topics are supported")
        }

        List<Topic> changedTopics = topicAccessClient.search(query, since?.toTimestamp())

        log.debug("Search resulted in ${changedTopics.size()} entries")

        List<IIssueKey> issueKeyList = []
        changedTopics.each { Topic topic ->
            issueKeyList.add(TopicReplica.toEntityKey(topic))
        }


        // TODO: do proper pagination
        PageResponse<IIssueKey> response = new PageResponse<IIssueKey> (pageRequest, issueKeyList,true)



        return response
    }

    @Override
    Source<ByteString, ?> getFileBodyStream(@NotNull @Nonnull String fileId, @NotNull @Nonnull IIssueKey entityKey, @Nullable @javax.annotation.Nullable IConnection connection) throws CategorizedException {
        return null
    }

    Boolean doesEntityExist(BasicIssueKey basicIssueKey) {
        log.debug("Attempting to check if entity topic with key:${basicIssueKey} exists")


        if (basicIssueKey.entityType != "topic") {
            // only handle topics for now
            def message = "Asked to retrieve an entity of type ${basicIssueKey.entityType} but only topics are supported"
            log.error(message)
            throw new DiscourseClientException(message)
        }

        // fetch the topic from the Discourse API

        Topic topic = topicAccessClient.getTopic(basicIssueKey.getURN())

        return topic != null && topic.topic_id == basicIssueKey.URN
    }
}
