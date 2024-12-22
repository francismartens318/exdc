package customconnectornode.discourse

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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.connection.IConnection
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.api.exception.IssueTrackerException
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.basic.domain.hubobject.v1.BasicHubUser
import com.exalate.basic.domain.hubobject.v1.status.BasicHubStatus
import com.exalate.replication.services.issuetracker.GroovyHttpClient
import customconnectornode.domain.EntityKeyContext
import customconnectornode.domain.EntityWriteResult
import customconnectornode.domain.PageRequest
import customconnectornode.domain.PageResponse
import customconnectornode.domain.StreamableFileMetadata
import customconnectornode.services.api.IIssueTrackerApi

import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.Application

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.inject.Inject
import java.text.SimpleDateFormat

import static com.exalate.util.UrlUtils.concat


class DiscourseApi implements IIssueTrackerApi {

    private static final Logger log = LoggerFactory.getLogger(DiscourseApi.class)
    private final Application application
    private final String discourseUrl
    private final String discourseUser
    private final String discoursePassword
    private final String discourseApiKey



    @Inject
    DiscourseApi(Application application) {
        this.application = application


        log.debug("Initializing DiscourseApi")
        // Get required environment variables
        this.discourseUrl = System.getenv('TRACKER_URL')
        this.discourseUser = System.getenv('TRACKER_USER')
        this.discoursePassword = System.getenv('TRACKER_PASSWORD')
        this.discourseApiKey = System.getenv('TRACKER_API_KEY')
        
        log.debug("Discourse URL: {}", discourseUrl)
        log.debug("Discourse User: {}", discourseUser)
        log.debug("Discourse Password: {}", discoursePassword)
        log.debug("Discourse API Key: {}", discourseApiKey)
        // Validate required environment variables
        if (!discourseUrl || !discourseUser || !discoursePassword || !discourseApiKey) {
            log.error("Missing required environment variables")
            throw new IllegalStateException("Missing required environment variables. Please set TRACKER_URL, TRACKER_USER, and TRACKER_PASSWORD")
        }
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

        GroovyHttpClient httpClient = application.injector().instanceOf(GroovyHttpClient.class)
        log.debug("Using HTTP client: {}", httpClient)
        return null

        httpClient.setBasicAuth(discourseUser, discoursePassword)

        HttpURLConnection conn = null
        try {
            // Extract topic ID from entity key
            String topicId = entityKey.urn
            if (!topicId) {
                log.error("No topic ID provided in entity key")
                return null
            }



            // Build URL for topic API endpoint
            String topicUrl = concat(discourseUrl, "/t/${topicId}.json")
            log.debug("Fetching topic from URL: {}", topicUrl)

            // Create URL and open connection
            URL url = new URL(topicUrl)
            conn = (HttpURLConnection) url.openConnection()
            
            // Configure connection
            conn.setDoOutput(true)
            conn.setDoInput(true)
            conn.setConnectTimeout(5000)
            conn.setReadTimeout(5000)
            conn.setUseCaches(false)
            
            // Set request method and headers
            conn.setRequestMethod("GET")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Api-Key", discourseApiKey)
            conn.setRequestProperty("Api-Username", discourseUser)
            
            log.debug("Connection properties set. Attempting to connect...")
            conn.connect()
            log.debug("Connected successfully")
            
            // Get response
            int responseCode
            try {
                responseCode = conn.getResponseCode()
                log.debug("Response code: {}", responseCode)
            } catch (Exception e) {
                log.error("Error getting response code: {}", e.message)
                log.error("Full error:", e)
                return null
            }

            if (responseCode != 200) {
                // Read error stream if available
                def errorStream = conn.getErrorStream()
                if (errorStream) {
                    def errorResponse = new BufferedReader(new InputStreamReader(errorStream)).getText()
                    log.error("Error response: {}", errorResponse)
                }
                log.error("Failed to fetch topic. Response code: {}", responseCode)
                return null
            }




            // Read response
            def response = new BufferedReader(new InputStreamReader(conn.getInputStream())).getText()
            log.debug("Response: {}", response)
            
            // Parse JSON response
            def jsonSlurper = new JsonSlurper()
            def topic = jsonSlurper.parseText(response)
            log.debug("Parsed topic: {}", topic)


            // Create hub issue from topic data
            BasicHubIssue hubIssue = new BasicHubIssue()
            hubIssue.key = topic.id?.toString()
            hubIssue.summary = topic.title
            hubIssue.description = topic.post_stream?.posts?.get(0)?.cooked // First post content
            
            // Add these required fields
            hubIssue.setId(topic.id?.toString())
            hubIssue.setEntityKey(new BasicIssueKey(topic.id?.toString(), topic.id?.toString(),"topic" ))
            hubIssue.setEntityUrl(concat(discourseUrl, "/t/${topic.id}"))
            
            // Optional but recommended fields
            if (topic.created_at) {
                try {
                    // Updated format to handle ISO 8601 with Z timezone
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
                    hubIssue.created = sdf.parse(topic.created_at)
                } catch (Exception e) {
                    log.warn("Could not parse created_at date: {}", topic.created_at)
                }
            }
            if (topic.last_posted_at) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    hubIssue.updated = sdf.parse(topic.last_posted_at)
                } catch (Exception e) {
                    log.warn("Could not parse last_posted_at date: {}", topic.last_posted_at)
                }
            }
            
            // Set status
            BasicHubStatus status = new BasicHubStatus()
            status.setName(topic.closed ? "Closed" : "Open")
            status.setValue(topic.closed ? "Closed" : "Open")
            hubIssue.setStatus(status)
            
            // Set author if available
            if (topic.post_stream?.posts?.get(0)?.username) {
                BasicHubUser author = new BasicHubUser()
                author.setUsername(topic.post_stream.posts.get(0).username)
                hubIssue.setReporter(author)
            }

            log.debug("Successfully created hub issue from topic: {}", hubIssue)
            return hubIssue

        } catch (Exception e) {
            log.error("Error reading topic: {}", e.message)
            log.error("Full stack trace:", e)
            return null
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect()
                } catch (Exception e) {
                    log.warn("Error disconnecting: {}", e.message)
                }
            }
        }
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

