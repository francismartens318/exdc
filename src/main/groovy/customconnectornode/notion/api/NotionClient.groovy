package customconnectornode.notion.api

import com.exalate.api.domain.IIssueKey
import com.exalate.domain.http.StreamingGroovyHttpResponse
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.PATCH
import static groovyx.net.http.Method.POST

class NotionClient {

    private static final Logger log = LoggerFactory.getLogger(NotionClient.class)
    private final String apiKey
    private final String baseUrl = "https://api.notion.com/v1/"
    private final String notionVersion = "2022-06-28"

    NotionClient(String apiKey) {
        this.apiKey = apiKey
    }

    private def executeRequest(method, path, Map body = null, Map queryParams = [:]) {
        def http = new HTTPBuilder(baseUrl)
        // http.ignoreSSLIssues() // Use with caution for local development if needed

        http.request(method, JSON) { req ->
            uri.path = path
            uri.query = queryParams
            headers.'Authorization' = "Bearer \${apiKey}"
            headers.'Notion-Version' = notionVersion
            headers.'Content-Type' = 'application/json'

            if (body) {
                req.body = body
            }

            log.debug("Notion API Request: {} {} Query: {} Body: {}", method, baseUrl + path, queryParams, body ? JsonOutput.toJson(body) : "Empty")

            response.success = { resp, json ->
                log.debug("Notion API Response Status: {}, Body: {}", resp.getStatusLine(), json)
                return json
            }

            response.failure = { resp, reader ->
                def errorBody = reader ? reader.text : "No error body"
                log.error("Notion API Request Failed: {} {}. Status: {}. Response: {}", method, baseUrl + path, resp.statusLine, errorBody)
                // More specific error handling could be added here
                // e.g. throw new NotionApiException(resp.status, errorBody)
                return [error: true, status: resp.status, message: errorBody, bodyContent: errorBody]
            }
        }
    }

    def getPage(String pageId) {
        log.debug("Fetching page with ID: {}", pageId)
        return executeRequest(GET, "pages/\${pageId}")
    }

    def createPage(Map properties, String parentId) {
        log.debug("Creating page with parentId: {} and properties: {}", parentId, properties)
        if (!parentId) {
            log.error("Parent ID is required to create a Notion page.")
            return [error: true, message: "Parent ID is required for page creation."]
        }

        Map parentStructure = [:]
        // Heuristic to determine if parentId is a database_id or page_id based on common UUID format
        // This is a simplification; a more robust way might be needed if IDs are not distinguishable
        // Or the type of parent should be explicitly passed.
        // For now, assuming parentId is a database_id as it's a common use case for integrations.
        // A better approach would be to require the parent type or have separate methods.
        parentStructure.put("database_id", parentId) // Defaulting to database_id for now
        // Example: if (isPageId(parentId)) { parentStructure.put("page_id", parentId) }

        Map body = [
            parent: parentStructure,
            properties: properties
            // icon and cover can be added here if needed
        ]
        return executeRequest(POST, "pages", body)
    }

    def updatePage(String pageId, Map properties) {
        log.debug("Updating page ID: {} with properties: {}", pageId, properties)
        Map body = [properties: properties]
        // icon and cover can also be updated
        return executeRequest(PATCH, "pages/\${pageId}", body)
    }

    def searchPages(String query, Date since, String startCursor, Integer pageSize) {
        log.debug("Searching pages. Query: '{}', Since: '{}', StartCursor: '{}', PageSize: {}", query, since, startCursor, pageSize)
        Map body = [:]
        if (query) {
            body.query = query
        }
        if (since) {
            // Notion API filter for last_edited_time
            // Format: "2024-01-01T00:00:00Z"
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
            body.filter = [
                property: "last_edited_time",
                timestamp: [
                    "on_or_after": isoFormat.format(since)
                ]
            ]
        }
        if (startCursor) {
            body.start_cursor = startCursor
        }
        if (pageSize != null && pageSize > 0) {
            body.page_size = pageSize
        }

        // Notion search API returns a list of pages/databases.
        // We need to parse this and potentially extract page IDs, titles for BasicHubIssue creation.
        def searchResult = executeRequest(POST, "search", body)
        if (searchResult?.results) {
            return [
                pages: searchResult.results.findAll { it.object == 'page' }, // Filter for pages only
                nextCursor: searchResult.next_cursor,
                hasMore: searchResult.has_more
            ]
        } else {
            log.warn("Search result from Notion did not contain 'results' field or was null: {}", searchResult)
            return [pages: [], nextCursor: null, hasMore: false]
        }
    }

    // downloadFile needs to handle different types of fileIdOrUrl (direct URL, Notion file URL, block ID with file)
    // This is a simplified version that assumes fileIdOrUrl is a direct, publicly accessible URL for now.
    // A more robust implementation would inspect the type of file or retrieve block data if it's a block_id.
    StreamingGroovyHttpResponse downloadFile(String fileUrl, IIssueKey entityKeyContext) {
        log.debug("Attempting to download file from URL: {} (context: {})", fileUrl, entityKeyContext?.URN)

        try {
            def http = new HTTPBuilder(fileUrl)
            // http.ignoreSSLIssues() // If needed for internal/dev URLs

            // For Notion-hosted files (type: "file"), the URL is pre-signed and doesn't need auth headers.
            // For external files, it's just a public URL.
            // If it were a call to a Notion API endpoint that *returns* a file stream, auth would be needed.

            StreamingGroovyHttpResponse response = http.get(contentType: groovyx.net.http.ContentType.ANY) { resp, stream ->
                if (resp.statusLine.statusCode == 200) {
                    log.info("Successfully started streaming file from URL: {}", fileUrl);
                    // The StreamingGroovyHttpResponse class should handle the stream correctly
                    // to be consumable by Akka streams.
                    // This assumes StreamingGroovyHttpResponse is compatible with what Exalate expects for Source<ByteString, ?>
                    return new StreamingGroovyHttpResponse(resp, stream) // This constructor might need adjustment
                } else {
                    log.error("Failed to download file from URL: {}. Status: {}", fileUrl, resp.statusLine);
                    return null // Or throw exception
                }
            }
            return response
        } catch (Exception e) {
            log.error("Exception while trying to download file from URL {}: {}", fileUrl, e.getMessage(), e)
            return null // Or throw exception
        }
    }
}
