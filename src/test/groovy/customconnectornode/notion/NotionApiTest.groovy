package customconnectornode.notion

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.domain.http.StreamingGroovyHttpResponse
import customconnectornode.notion.api.NotionClient
import customconnectornode.domain.PageRequest
import customconnectornode.domain.PageResponse
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import play.api.Application

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.ArgumentMatchers.isNull // Added for isNull()

class NotionApiTest {

    @Mock
    private Application mockApplication
    @Mock
    private NotionClient mockNotionClient
    @Mock
    private Source<ByteString, ?> mockSource // For getFileBodyStream test
    @Mock
    private StreamingGroovyHttpResponse mockStreamingResponse // For getFileBodyStream test


    private NotionApi notionApi

    @Captor
    private ArgumentCaptor<Map<String, Object>> propertiesCaptor

    @Before
    void setUp() {
        MockitoAnnotations.initMocks(this)
        notionApi = new NotionApi(mockApplication, "test_api_key")
        // Inject the mocked NotionClient into NotionApi
        notionApi.notionClient = mockNotionClient
    }

    @Test
    void testReadEntity_Success() {
        // Arrange
        String pageId = "test-page-id"
        IIssueKey issueKey = new BasicIssueKey(pageId, "page", "page")

        def mockPageData = [
            id: pageId,
            properties: [
                Title: [title: [[plain_text: "Test Page Title"]]]
            ]
        ]

        when(mockNotionClient.getPage(pageId)).thenReturn(mockPageData)

        // Act
        BasicHubIssue result = (BasicHubIssue) notionApi.readEntity(issueKey)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Page ID should match", pageId, result.key.URN)
        assertEquals("Summary should match page title", "Test Page Title", result.summary)
    }

    @Test
    void testReadEntity_NotFound() {
        // Arrange
        String pageId = "non-existent-page-id"
        IIssueKey issueKey = new BasicIssueKey(pageId, "page", "page")

        when(mockNotionClient.getPage(pageId)).thenReturn(null) // Simulate page not found

        // Act
        BasicHubIssue result = (BasicHubIssue) notionApi.readEntity(issueKey)

        // Assert
        assertNull("Result should be null when page not found", result)
    }

    @Test
    void testSearchEntityTypes_NoQuery() {
        PageRequest pageRequest = new PageRequest(0, 10)
        PageResponse<EntityType> response = notionApi.searchEntityTypes(null, pageRequest)

        assertNotNull(response)
        assertEquals("Should return 1 entity type", 1, response.getResults().size())
        assertEquals("Entity type should be 'page'", "page", response.getResults().get(0).getName())
        assertTrue("Entity type should be syncable", response.getResults().get(0).isSyncable())
    }

    @Test
    void testSearchEntityTypes_WithQuery_Match() {
        PageRequest pageRequest = new PageRequest(0, 10)
        PageResponse<EntityType> response = notionApi.searchEntityTypes("page", pageRequest)

        assertNotNull(response)
        assertEquals("Should return 1 entity type", 1, response.getResults().size())
        assertEquals("Entity type should be 'page'", "page", response.getResults().get(0).getName())
    }

    @Test
    void testSearchEntityTypes_WithQuery_NoMatch() {
        PageRequest pageRequest = new PageRequest(0, 10)
        PageResponse<EntityType> response = notionApi.searchEntityTypes("database", pageRequest)

        assertNotNull(response)
        assertEquals("Should return 0 entity types", 0, response.getResults().size())
    }

    @Test
    void testWriteEntity_CreatePage() {
        BasicHubIssue hubIssueToCreate = new BasicHubIssue()
        hubIssueToCreate.summary = "New Notion Page"
        hubIssueToCreate.key = new BasicIssueKey(null, "page", "page", null, new BasicIssueKey("parent-db-id","database","database"))


        def createdPageDataFromClient = [
            id: "new-page-id-from-notion",
            properties: [
                Title: [title: [[plain_text: "New Notion Page"]]]
            ]
        ]

        when(mockNotionClient.createPage(propertiesCaptor.capture(), eq("parent-db-id"))).thenReturn(createdPageDataFromClient)


        def result = notionApi.writeEntity(null, null, hubIssueToCreate, [], [])

        assertNotNull(result)
        assertNotNull(result.entity)
        assertEquals("new-page-id-from-notion", result.entity.key.URN)
        assertEquals("New Notion Page", result.entity.summary)

        Map<String, Object> capturedProps = propertiesCaptor.getValue()
        assertNotNull(capturedProps)
        assertTrue(capturedProps.containsKey("Title"))
    }

    @Test
    void testWriteEntity_UpdatePage() {
        String existingPageId = "existing-page-id"
        IIssueKey existingKey = new BasicIssueKey(existingPageId, "page", "page")
        BasicHubIssue hubIssueToUpdate = new BasicHubIssue()
        hubIssueToUpdate.key = existingKey
        hubIssueToUpdate.summary = "Updated Page Title"

        def updatedPageDataFromClient = [
            id: existingPageId,
            properties: [
                Title: [title: [[plain_text: "Updated Page Title"]]]
            ]
        ]
        when(mockNotionClient.updatePage(eq(existingPageId), propertiesCaptor.capture())).thenReturn(updatedPageDataFromClient)

        def result = notionApi.writeEntity(existingKey, null, hubIssueToUpdate, [], [])

        assertNotNull(result)
        assertNotNull(result.entity)
        assertEquals(existingPageId, result.entity.key.URN)
        assertEquals("Updated Page Title", result.entity.summary)

        Map<String, Object> capturedProps = propertiesCaptor.getValue()
        assertNotNull(capturedProps)
        assertTrue(capturedProps.containsKey("Title"))
    }

    @Test
    void testSearch_Success() {
        PageRequest pageRequest = new PageRequest(0, 10, null)
        String query = "Test Query"

        def notionSearchResult = [
            pages: [
                [id: "page1", properties: [Title: [title: [[plain_text: "Page 1"]]]]],
                [id: "page2", properties: [Title: [title: [[plain_text: "Page 2"]]]]]
            ],
            nextCursor: "next_cursor_string",
            hasMore: true
        ]
        // Ensure isNull() is imported for the third argument of searchPages if it's truly null
        when(mockNotionClient.searchPages(eq(query), any(), isNull(String.class), eq(10))).thenReturn(notionSearchResult)


        PageResponse<IIssueKey> response = notionApi.search(query, null, new customconnectornode.domain.EntityKeyContext("page"), pageRequest)

        assertNotNull(response)
        assertEquals("Should return 2 issue keys", 2, response.getResults().size())
        assertEquals("page1", response.getResults().get(0).getURN())
        assertEquals("page2", response.getResults().get(1).getURN())
        assertTrue("Should have more pages", response.hasMore())
        assertEquals("next_cursor_string", response.getNextPageStart())
    }

    @Test
    void testSearch_EmptyResult() {
        PageRequest pageRequest = new PageRequest(0, 10, null)
        String query = "NonExistent"

        def notionSearchResult = [ pages: [], nextCursor: null, hasMore: false ]
        when(mockNotionClient.searchPages(eq(query), any(), isNull(String.class), eq(10))).thenReturn(notionSearchResult)

        PageResponse<IIssueKey> response = notionApi.search(query, null, new customconnectornode.domain.EntityKeyContext("page"), pageRequest)

        assertNotNull(response)
        assertEquals("Should return 0 issue keys", 0, response.getResults().size())
        assert !response.hasMore() // Changed from assertFalse for Groovy style
        assertNull("Next cursor should be null", response.getNextPageStart())
    }

    @Test
    void testGetFileBodyStream_Success() {
        String fileId = "file-id-or-url"
        IIssueKey entityKey = new BasicIssueKey("page-id", "page", "page")

        when(mockNotionClient.downloadFile(fileId, entityKey)).thenReturn(mockStreamingResponse)
        when(mockStreamingResponse.getSource()).thenReturn(mockSource)

        Source<ByteString, ?> resultSource = notionApi.getFileBodyStream(fileId, entityKey, null)

        assertNotNull("Source should not be null", resultSource)
        assertEquals("Should return the mocked source", mockSource, resultSource)
        verify(mockNotionClient).downloadFile(fileId, entityKey)
    }

    @Test(expected = com.exalate.api.exception.CategorizedException.class)
    void testGetFileBodyStream_ClientReturnsNull() {
        String fileId = "file-id-or-url"
        IIssueKey entityKey = new BasicIssueKey("page-id", "page", "page")

        when(mockNotionClient.downloadFile(fileId, entityKey)).thenReturn(null)

        notionApi.getFileBodyStream(fileId, entityKey, null)
    }
}
