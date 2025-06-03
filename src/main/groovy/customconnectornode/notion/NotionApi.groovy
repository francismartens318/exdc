package customconnectornode.notion

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
import customconnectornode.notion.api.NotionClient
import customconnectornode.notion.domain.Page
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

// TODO: Implement other methods of the IIssueTrackerApi interface
// TODO: Add error handling and improve logging
class NotionApi implements IIssueTrackerApi {

    private static final Logger log = LoggerFactory.getLogger(NotionApi.class)
    private final Application application
    NotionClient notionClient // Made package-private for testing

    NotionApi(Application application, String apiKey) {
        this.application = application
        this.notionClient = new NotionClient(apiKey)
    }

    @Override
    IHubIssueReplica readEntity(@NotNull @Nonnull IIssueKey entityKey) throws CategorizedException {
        log.debug("Reading entity with key: {}", entityKey)
        if (!"page".equals(entityKey.getEntityType())) {
            log.warn("readEntity called for unsupported entity type: {}", entityKey.getEntityType())
            // Or throw an exception
            return null
        }

        def pageData = notionClient.getPage(entityKey.URN)

        if (pageData) {
            BasicHubIssue hubIssue = new BasicHubIssue()
            hubIssue.key = new BasicIssueKey(entityKey.URN, "page", "page")
            // Assuming pageData is a Map or a Groovy object from JSON
            def titleProperty = pageData.properties?.Title?.title
            if (titleProperty instanceof List && !titleProperty.isEmpty()) {
                hubIssue.summary = titleProperty[0].plain_text
            } else {
                hubIssue.summary = "Untitled Notion Page" // Default summary
            }
            // TODO: Map other relevant fields from pageData to hubIssue (e.g., description, custom fields)
            return hubIssue
        } else {
            log.error("Failed to read entity with key: {}. Page data was null.", entityKey)
            // Consider throwing a specific exception for not found or access issues
            return null
        }
    }

    // --- Placeholder implementations for other IIssueTrackerApi methods ---

    @Override
    PageResponse<EntityType> searchEntityTypes(@Nullable String query, @NotNull @Nonnull PageRequest pageRequest) throws CategorizedException {
        log.debug("Searching entity types with query: {} and page request: {}", query, pageRequest)
        List<EntityType> entityTypes = [new EntityType("page", true, true)]

        if (query != null && !query.isEmpty()) {
            entityTypes = entityTypes.findAll { it.name.toLowerCase().contains(query.toLowerCase()) }
        }

        int start = pageRequest.start
        int limit = pageRequest.limit
        List<EntityType> paginatedEntityTypes = entityTypes.drop(start).take(limit)
        boolean hasMore = (start + limit) < entityTypes.size()

        return new PageResponse<EntityType>(pageRequest, paginatedEntityTypes, hasMore)
    }

    @Override
    EntityWriteResult writeEntity(@Nullable IIssueKey entityKey,
                                  @NotNull @Nonnull IHubIssueReplica entityBeforeScript,
                                  @NotNull @Nonnull IHubIssueReplica entityAfterScript,
                                  @NotNull @Nonnull List<INonPersistentTrace> traces,
                                  @NotNull @Nonnull List<StreamableFileMetadata> blobMetadataList) throws CategorizedException {
        log.debug("Writing entity. Key: {}, EntityAfterScript: {}", entityKey, entityAfterScript)

        if (!"page".equals(entityAfterScript.getKey()?.getEntityType()) && entityKey != null && !"page".equals(entityKey.getEntityType())) {
             log.warn("writeEntity called for unsupported entity type: {}", entityAfterScript.getKey()?.getEntityType() ?: entityKey?.getEntityType())
             // Or throw an exception
            return new EntityWriteResult(entityAfterScript, traces); // Or handle error appropriately
        }

        // Transform IHubIssueReplica to a format suitable for NotionClient (e.g., a Map or a Notion Page object)
        // For now, let's assume we need to construct a Map representing the Notion page properties
        Map<String, Object> notionPageProperties = [:]
        // Example: Set title from summary. Notion API expects a specific structure for titles.
        // This needs to align with how NotionClient's createPage/updatePage methods expect data.
        if (entityAfterScript.getSummary() != null) {
            notionPageProperties.put("Title", [ // This is a simplified representation.
                // Notion's actual title structure is more complex:
                // "properties": { "title": { "title": [{ "text": { "content": "My page title" }}]}}
                // Or for database pages: "Name": { "title": [{ "text": { "content": "My page title" }}]}
                // This transformation logic will need to be robust.
                 [type: "text", text: [content: entityAfterScript.getSummary()]]
            ])
        }
        // TODO: Map other fields from entityAfterScript to notionPageProperties (parent, other properties, content blocks etc.)
        // This is highly dependent on the Notion API structure for creating/updating pages.

        String pageId = entityKey?.URN
        def savedPageData

        if (pageId == null) {
            // Create new page
            log.debug("Creating new Notion page with properties: {}", notionPageProperties)
            // TODO: The parent needs to be specified for creating a page. This could come from entityAfterScript or configuration.
            // For example: notionPageProperties.put("parent", [database_id: "your_database_id_here"]) or page_id
            if (!notionPageProperties.containsKey("parent")) {
                 // Defaulting parent or throwing error if not specified. This is CRITICAL for Notion.
                 log.error("Parent ID/Database ID is required to create a Notion page and was not provided.")
                 // For now, let's assume it's a top-level page in a workspace (less common for integrations)
                 // or this needs to be configured/derived. This part needs careful design.
                 // throw new CategorizedException("Parent for Notion page creation is missing.")
                 // Using a placeholder or skipping creation if parent is missing:
                 log.warn("Parent information is missing. Page creation might fail or be incomplete.")
            }
            savedPageData = notionClient.createPage(notionPageProperties, entityAfterScript.getKey()?.getParentKey()?.getURN()) // Assuming getParentKey().getURN() gives parent_id or database_id
        } else {
            // Update existing page
            log.debug("Updating Notion page {} with properties: {}", pageId, notionPageProperties)
            savedPageData = notionClient.updatePage(pageId, notionPageProperties)
        }

        if (savedPageData == null) {
            log.error("Failed to write entity to Notion. Page data was null after create/update call.")
            // TODO: Handle error, potentially throw exception
            // Returning original entityAfterScript and traces for now
            return new EntityWriteResult(entityAfterScript, traces)
        }

        // Transform the response from NotionClient (savedPageData) back to IHubIssueReplica
        // This is similar to the transformation in readEntity
        BasicHubIssue savedHubIssue = new BasicHubIssue()
        savedHubIssue.key = new BasicIssueKey(savedPageData.id, "page", "page") // Assuming savedPageData has an 'id'

        def titleProperty = savedPageData.properties?.Title?.title // Adjust based on actual Notion response
        if (titleProperty instanceof List && !titleProperty.isEmpty()) {
            savedHubIssue.summary = titleProperty[0].plain_text
        } else {
             // Fallback if title structure is different or missing in response
            savedHubIssue.summary = entityAfterScript.getSummary() ?: "Untitled Notion Page"
        }
        // TODO: Map other relevant fields from savedPageData to savedHubIssue

        log.info("Successfully wrote entity to Notion. Resulting ID: {}", savedHubIssue.getKey().getURN())
        return new EntityWriteResult(savedHubIssue, traces)
    }

    @Override
    PageResponse<IIssueKey> search(@Nullable String query,
                                   @Nullable Date since,
                                   @Nullable EntityKeyContext entityKeyContext,
                                   @Nullable PageRequest pageRequest) throws CategorizedException {
        log.debug("Searching Notion pages. Query: '{}', Since: '{}', Context: '{}', PageRequest: '{}'", query, since, entityKeyContext, pageRequest)

        if (entityKeyContext != null && !"page".equals(entityKeyContext.getEntityType())) {
            log.warn("Search called for unsupported entity type: {}", entityKeyContext.getEntityType())
            // Return empty response for unsupported types
            return new PageResponse<IIssueKey>(pageRequest, [], false)
        }

        // TODO: The NotionClient will need a method like searchPages(query, since, startCursor, pageSize)
        // The 'since' parameter might need conversion to Notion's last_edited_time filter format.
        // Notion API uses cursor-based pagination (start_cursor and page_size).
        // We'll need to manage the cursor between paged requests if we want to support true pagination across multiple calls.
        // For a single call, pageRequest.start can be tricky with cursors if not starting from the beginning.
        // Let's assume pageRequest.start is 0 for the first call for simplicity for now, or map it to a start_cursor if available.

        String startCursor = pageRequest.getStartCursor() // Assuming PageRequest might have this
        Integer pageSize = pageRequest.getLimit()

        // Call NotionClient to search pages. This method needs to be implemented in NotionClient.
        // It should return a structure like [pages: [list of page objects], nextCursor: "cursor_string_or_null", hasMore: boolean]
        def searchResult = notionClient.searchPages(query, since, startCursor, pageSize)

        if (searchResult == null || searchResult.pages == null) {
            log.error("Failed to search pages in Notion or received null result.")
            return new PageResponse<IIssueKey>(pageRequest, [], false)
        }

        List<IIssueKey> issueKeys = []
        searchResult.pages.each { pageData ->
            // Assuming pageData is a map or object with at least an 'id'
            if (pageData?.id) {
                issueKeys.add(new BasicIssueKey(pageData.id, "page", "page"))
            } else {
                log.warn("Found a page in search results without an ID: {}", pageData)
            }
        }

        log.info("Search returned {} pages from Notion.", issueKeys.size())

        // The 'hasMore' from searchResult.hasMore would be ideal here.
        // If PageRequest needs total count, Notion search doesn't provide it directly.
        return new PageResponse<IIssueKey>(pageRequest, issueKeys, searchResult.hasMore, searchResult.nextCursor)
    }

    @Override
    Source<ByteString, ?> getFileBodyStream(@NotNull @Nonnull String fileId,
                                             @NotNull @Nonnull IIssueKey entityKey,
                                             @Nullable IConnection connection) throws CategorizedException {
        log.debug("Attempting to get file body stream for fileId: {} (associated with entityKey: {})", fileId, entityKey)

        if (!"page".equals(entityKey.getEntityType())) {
            log.warn("getFileBodyStream called for an entity type ({}) that may not support file attachments directly in this context: {}", entityKey.getEntityType(), entityKey.getURN())
            // Depending on how Notion handles file IDs (e.g., are they globally unique or per-page/block?),
            // this check might be adjusted. For now, we proceed but log a warning.
        }

        // Notion file URLs can be public (if shared) or temporary signed URLs obtained via the API for private files.
        // The 'fileId' here might be a Notion file URL or a block ID that contains a file.
        // The NotionClient.downloadFile method will need to handle this.
        // It might need to retrieve block details to get the actual file URL if fileId is a block_id.

        // TODO: Implement NotionClient.downloadFile(String fileIdentifier)
        // This method should return a StreamingGroovyHttpResponse or similar object with a .source property
        StreamingGroovyHttpResponse fileStreamResponse = notionClient.downloadFile(fileId, entityKey)

        if (fileStreamResponse == null) {
            log.error("Failed to get file stream from NotionClient for fileId: {}. Response was null.", fileId)
            // Or throw a specific CategorizedException
            throw new CategorizedException("Failed to download file from Notion: " + fileId) {}
        }

        log.info("Successfully obtained file stream for fileId: {}", fileId)
        return fileStreamResponse.source
    }
}
