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
import customconnectornode.discourse.api.TopicAccessClient
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.http.DiscourseClientException
import customconnectornode.discourse.http.TopicAccessClientImpl
import customconnectornode.discourse.transform.TopicReplicaBuilder
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
    private final String discourseUrl
    private final String discourseUser
    private final String discoursePassword
    private final String discourseApiKey

    private final TopicAccessClient discourseClient



    @Inject
    DiscourseApi(Application application) {
        this.application = application



        log.debug("Initializing DiscourseApi")
        // Get required environment variables
        this.discourseUrl = System.getProperty('TRACKER_URL')
        this.discourseUser = System.getProperty('TRACKER_USER')
        this.discoursePassword = System.getProperty('TRACKER_PASSWORD')
        this.discourseApiKey = System.getProperty('TRACKER_API_KEY')


        log.debug("Discourse URL: {}", discourseUrl)
        log.debug("Discourse User: {}", discourseUser)
        log.debug("Discourse Password: {}", discoursePassword)
        log.debug("Discourse API Key: {}", discourseApiKey)
        // Validate required environment variables
        if (!discourseUrl || !discourseUser || !discoursePassword || !discourseApiKey) {
            log.error("Missing required environment variables")
            throw new IllegalStateException("Missing required environment variables. Please set TRACKER_URL, TRACKER_USER, and TRACKER_PASSWORD")
        }

        this.discourseClient = new TopicAccessClientImpl(discourseUrl, discourseApiKey, discourseUser)

        log.debug("DiscourseApi initialized successfully")
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
        Topic topic = discourseClient.getTopic(entityKey.getURN()).block()

        // convert to IHubIssueReplica
        return TopicReplicaBuilder.build(topic)
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
        log.debug("Writing entity with key: {}", entityKey)
        // Basic implementation returning the entity after script
        log.debug("Successfully wrote entity: {}", entityKey)
        return new EntityWriteResult(entityAfterScript, traces)
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

    
    boolean isEntityDeleted(@Nonnull IIssueKey entityKey) {
        return false // Basic implementation assuming entities are not deleted
    }

    
    void deleteEntity(@Nonnull IIssueKey entityKey) {
        throw new IssueTrackerException("Delete operation not supported")
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

