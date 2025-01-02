package customconnectornode.discourse

import akka.stream.scaladsl.Source

/**
 * Implementation of IIssueTrackerApi for Discourse forum integration.
 * Handles communication with Discourse REST API for managing topics as issues.
 *
 * Key capabilities:
 * - Topic CRUD operations
 * - Entity type management
 * - Search functionality
 * - Authentication via API key
 *
 * Required environment variables:
 * - TRACKER_URL: Base URL of Discourse instance
 * - TRACKER_USER: Discourse username
 * - TRACKER_PASSWORD: Discourse password
 * - TRACKER_API_KEY: Discourse API key for authentication
 */

import akka.util.ByteString
import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.connection.IConnection
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.api.exception.IssueTrackerException
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.api.TopicAccessClient
import customconnectornode.discourse.domain.Post
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.http.DiscourseClientException
import customconnectornode.discourse.http.DiscourseClientImpl
import customconnectornode.discourse.http.TopicAccessClientImpl
import customconnectornode.discourse.transform.TopicReplica
import customconnectornode.domain.*
import customconnectornode.services.api.IIssueTrackerApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.Application

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.inject.Inject

class DiscourseApi implements IIssueTrackerApi {

    private static final Logger log = LoggerFactory.getLogger(DiscourseApi.class)
    private final Application application

    private final TopicAccessClient topicAccessClient
    private final DiscourseClient discourseClient



    @Inject
    DiscourseApi(Application application) {
        this.application = application
        this.discourseClient = new DiscourseClientImpl()
        this.topicAccessClient = new TopicAccessClientImpl(discourseClient)

        log.debug("DiscourseApi initialized successfully")
    }

    // used in test
    TopicAccessClient getTopicAccessClient() {
        return topicAccessClient
    }

    @Override
    PageResponse<EntityType> searchEntityTypes(String query, PageRequest pageRequest) {
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

    @Nullable
    IHubIssueReplica readEntity(IIssueKey entityKey) {
        log.debug("Attempting to read entity topic with key: {}", entityKey)


        if (entityKey.entityType != "topic") {
            // only handle topics for now

            def message = "Asked to retrieve an entity of type ${entityKey.entityType} but only topics are supported"
            log.error(message)
            throw new DiscourseClientException(message)
        }

        // fetch the topic from the Discourse API

        Topic topic = topicAccessClient.getTopic(entityKey.getURN())

        // convert to IHubIssueReplica
        return topic ? TopicReplica.toReplica(topic) : null
    }



    @Nonnull
    @Override
    EntityWriteResult writeEntity(
        IIssueKey entityKey,
        IHubIssueReplica entityBeforeScript,
        IHubIssueReplica entityAfterScript,
        List<INonPersistentTrace> traces,
        List<StreamableFileMetadata> blobMetadataList
    ) {

        Topic changeTopic = TopicReplica.toTopic((BasicHubIssue) entityAfterScript)

        // First create a topic from the entityAfterScript.  This will be used to either create or update the topic
        // in Discourse.
        if  (entityKey == null) {
            // if there is no entity key - first create the entity
            // note that additional information from the hubIssue, such as comments, tags etc will be added during the update step
            log.debug("Entity key is empty, creating new topic using title, description and category ")

            Topic toCreateTopic = TopicReplica.toTopic((BasicHubIssue) entityAfterScript)
            String createdTopicId = topicAccessClient.create(toCreateTopic)?.topic_id
            Topic createdTopic = topicAccessClient.getTopic(createdTopicId)

            // populate the acquired fields id and topic_id
            changeTopic.id = createdTopic.id
            changeTopic.topic_id = createdTopic.topic_id

            // add the created topic to the list of posts

            List<Post> buildPosts = createdTopic.posts + changeTopic.posts.drop(1)
            changeTopic.posts = buildPosts
        }


        log.debug("Writing entity with key: {}", entityKey)
        Topic updatedTopic = topicAccessClient.update(changeTopic)

        return new EntityWriteResult(TopicReplica.toReplica(updatedTopic), traces)
    }

    @Override
    PageResponse<IIssueKey> search(
            String query,
            Date since,
            EntityKeyContext entityKeyContext,
            PageRequest pageRequest
    ) {
        // Return empty page response for now
        return new PageResponse<IIssueKey>(pageRequest, [], true)
    }

    @Override
    Source<ByteString, ?> getFileBodyStream(
        String fileId,
        IIssueKey entityKey,
        IConnection connection
    ) {
        // Return null if attachment not found
        return null
    }

    
    boolean doesEntityExist(@Nonnull IIssueKey entityKey) {
        log.debug("Checking if entity exists: {}", entityKey)
        boolean exists = readEntity(entityKey) != null
        log.debug("Entity {} exists: {}", entityKey, exists)
        return exists
    }

    
    boolean isEntityDeleted(IIssueKey entityKey) {
        return false // Basic implementation assuming entities are not deleted
    }

    
    void deleteEntity( IIssueKey entityKey) {
        throw new IssueTrackerException("Delete not supported")
    }

    
    String uploadFile(
        @Nonnull String filename,
        @Nonnull Source<ByteString, ?> content,
        @Nullable IIssueKey entityKey,
        @Nullable IConnection connection
    ) {
        throw new IssueTrackerException("File upload not supported")
    }



    Source<ByteString, ?> getFileBodyStream(String fileId) {
        throw new UnsupportedOperationException("Method 'getFileBodyStream' is not implemented")
    }


    IConnection getConnection() {
        throw new UnsupportedOperationException("Method 'getConnection' is not implemented")
    }


    void setConnection(IConnection connection) {
        throw new UnsupportedOperationException("Method 'setConnection' is not implemented")
    }
}

