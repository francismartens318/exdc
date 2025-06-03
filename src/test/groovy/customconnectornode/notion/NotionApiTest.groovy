package customconnectornode.notion

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.domain.http.StreamingGroovyHttpResponse
import customconnectornode.notion.api.NotionClient
import customconnectornode.domain.EntityKeyContext
import customconnectornode.domain.PageRequest
import customconnectornode.domain.PageResponse
import play.api.Application
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll // Optional: for parameterized tests if any get added later

// It's good practice to import static methods for Spock if needed, e.g. for interactions
// import static spock.util.matcher.HamcrestMatchers.closeTo // Example

class NotionApiTest extends Specification {

    // Mocks are automatically created by Spock by just declaring them with the Mock() annotation or as fields
    Application mockApplication = Mock()
    NotionClient mockNotionClient = Mock()
    Source<ByteString, ?> mockSource = Mock() // For getFileBodyStream test
    StreamingGroovyHttpResponse mockStreamingResponse = Mock() // For getFileBodyStream test

    @Subject // Indicates the main object under test
    NotionApi notionApi

    void setup() {
        // No need for MockitoAnnotations.initMocks(this)
        notionApi = new NotionApi(mockApplication, "test_api_key")
        notionApi.notionClient = mockNotionClient // Inject mock client
    }

    def "testReadEntity_Success"() {
        given: "a page ID and issue key"
        String pageId = "test-page-id"
        IIssueKey issueKey = new BasicIssueKey(pageId, "page", "page")
        def mockPageData = [
            id: pageId,
            properties: [
                Title: [title: [[plain_text: "Test Page Title"]]]
            ]
        ]

        when: "readEntity is called"
        1 * mockNotionClient.getPage(pageId) >> mockPageData // Spock interaction: 1 call, return mockPageData
        BasicHubIssue result = (BasicHubIssue) notionApi.readEntity(issueKey)

        then: "the result should be correctly mapped"
        result != null
        result.key.URN == pageId
        result.summary == "Test Page Title"
    }

    def "testReadEntity_NotFound"() {
        given: "a non-existent page ID and issue key"
        String pageId = "non-existent-page-id"
        IIssueKey issueKey = new BasicIssueKey(pageId, "page", "page")

        when: "readEntity is called and client returns null"
        1 * mockNotionClient.getPage(pageId) >> null
        BasicHubIssue result = (BasicHubIssue) notionApi.readEntity(issueKey)

        then: "the result should be null"
        result == null
    }

    def "testSearchEntityTypes_NoQuery"() {
        given: "a page request"
        PageRequest pageRequest = new PageRequest(0, 10)

        when: "searchEntityTypes is called with no query"
        PageResponse<EntityType> response = notionApi.searchEntityTypes(null, pageRequest)

        then: "it returns the 'page' entity type"
        response != null
        response.getResults().size() == 1
        response.getResults()[0].getName() == "page"
        response.getResults()[0].isSyncable() // Spock automatically asserts true for boolean conditions
    }

    def "testSearchEntityTypes_WithQuery_Match"() {
        given: "a page request and a matching query"
        PageRequest pageRequest = new PageRequest(0, 10)

        when: "searchEntityTypes is called with 'page' query"
        PageResponse<EntityType> response = notionApi.searchEntityTypes("page", pageRequest)

        then: "it returns the 'page' entity type"
        response != null
        response.getResults().size() == 1
        response.getResults()[0].getName() == "page"
    }

    def "testSearchEntityTypes_WithQuery_NoMatch"() {
        given: "a page request and a non-matching query"
        PageRequest pageRequest = new PageRequest(0, 10)

        when: "searchEntityTypes is called with 'database' query"
        PageResponse<EntityType> response = notionApi.searchEntityTypes("database", pageRequest)

        then: "it returns no entity types"
        response != null
        response.getResults().size() == 0
    }

    def "testWriteEntity_CreatePage"() {
        given: "a hub issue to create and a parent ID"
        BasicHubIssue hubIssueToCreate = new BasicHubIssue(
            key: new BasicIssueKey(null, "page", "page", null, new BasicIssueKey("parent-db-id", "database", "database")),
            summary: "New Notion Page"
        )

        def createdPageDataFromClient = [
            id: "new-page-id-from-notion",
            properties: [
                Title: [title: [[plain_text: "New Notion Page"]]]
            ]
        ]

        when: "writeEntity is called to create a page"
        // Spock's argument captor is implicit with closures for arguments
        def result = notionApi.writeEntity(null, null, hubIssueToCreate, [], [])

        then: "the notionClient creates the page and result is mapped"
        1 * mockNotionClient.createPage({ Map props -> props.Title[0].text.content == "New Notion Page" }, "parent-db-id") >> createdPageDataFromClient
        result != null
        result.entity != null
        result.entity.key.URN == "new-page-id-from-notion"
        result.entity.summary == "New Notion Page"
    }

    def "testWriteEntity_UpdatePage"() {
        given: "an existing page ID and a hub issue to update"
        String existingPageId = "existing-page-id"
        IIssueKey existingKey = new BasicIssueKey(existingPageId, "page", "page")
        BasicHubIssue hubIssueToUpdate = new BasicHubIssue(key: existingKey, summary: "Updated Page Title")

        def updatedPageDataFromClient = [
            id: existingPageId,
            properties: [
                Title: [title: [[plain_text: "Updated Page Title"]]]
            ]
        ]

        when: "writeEntity is called to update the page"
        def result = notionApi.writeEntity(existingKey, null, hubIssueToUpdate, [], [])

        then: "the notionClient updates the page and result is mapped"
        1 * mockNotionClient.updatePage(existingPageId, { Map props -> props.Title[0].text.content == "Updated Page Title" }) >> updatedPageDataFromClient
        result != null
        result.entity != null
        result.entity.key.URN == existingPageId
        result.entity.summary == "Updated Page Title"
    }

    def "testSearch_Success"() {
        given: "a page request, query, and entity context"
        PageRequest pageRequest = new PageRequest(0, 10, null)
        String query = "Test Query"
        EntityKeyContext entityKeyContext = new EntityKeyContext("page")

        def notionSearchResult = [
            pages: [
                [id: "page1", properties: [Title: [title: [[plain_text: "Page 1"]]]]],
                [id: "page2", properties: [Title: [title: [[plain_text: "Page 2"]]]]]
            ],
            nextCursor: "next_cursor_string",
            hasMore: true
        ]

        when: "search is called"
        1 * mockNotionClient.searchPages(query, null, null, 10) >> notionSearchResult
        PageResponse<IIssueKey> response = notionApi.search(query, null, entityKeyContext, pageRequest)

        then: "the response is correctly mapped"
        response != null
        response.getResults().size() == 2
        response.getResults()[0].getURN() == "page1"
        response.getResults()[1].getURN() == "page2"
        response.hasMore()
        response.getNextPageStart() == "next_cursor_string"
    }

    def "testSearch_EmptyResult"() {
        given: "a page request, query, and entity context for a non-existent item"
        PageRequest pageRequest = new PageRequest(0, 10, null)
        String query = "NonExistent"
        EntityKeyContext entityKeyContext = new EntityKeyContext("page")

        def notionSearchResult = [ pages: [], nextCursor: null, hasMore: false ]

        when: "search is called"
        1 * mockNotionClient.searchPages(query, null, null, 10) >> notionSearchResult
        PageResponse<IIssueKey> response = notionApi.search(query, null, entityKeyContext, pageRequest)

        then: "the response indicates no results"
        response != null
        response.getResults().size() == 0
        !response.hasMore()
        response.getNextPageStart() == null
    }

    def "testGetFileBodyStream_Success"() {
        given: "a file ID and entity key"
        String fileId = "file-id-or-url"
        IIssueKey entityKey = new BasicIssueKey("page-id", "page", "page")

        when: "getFileBodyStream is called"
        1 * mockNotionClient.downloadFile(fileId, entityKey) >> mockStreamingResponse
        1 * mockStreamingResponse.getSource() >> mockSource
        Source<ByteString, ?> resultSource = notionApi.getFileBodyStream(fileId, entityKey, null)

        then: "the correct source is returned"
        resultSource != null
        resultSource == mockSource
    }

    def "testGetFileBodyStream_ClientReturnsNull_ThrowsCategorizedException"() {
        given: "a file ID and entity key"
        String fileId = "file-id-or-url"
        IIssueKey entityKey = new BasicIssueKey("page-id", "page", "page")

        when: "getFileBodyStream is called and client download fails"
        1 * mockNotionClient.downloadFile(fileId, entityKey) >> null
        notionApi.getFileBodyStream(fileId, entityKey, null)

        then: "a CategorizedException is thrown"
        // Spock's way of testing exceptions
        thrown(com.exalate.api.exception.CategorizedException)
    }
}
