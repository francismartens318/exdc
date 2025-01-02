package customconnectornode.discourse


import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.api.domain.hubobject.v1_2.IHubUser
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.api.exception.IssueTrackerException
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubComment
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.basic.domain.hubobject.v1.BasicHubUser
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.domain.TopicTest
import customconnectornode.discourse.transform.TopicReplica
import customconnectornode.domain.EntityWriteResult
import customconnectornode.domain.PageRequest
import customconnectornode.domain.PageResponse
import customconnectornode.domain.StreamableFileMetadata
import play.api.Application
import play.api.inject.Injector
import spock.lang.Specification
import spock.lang.Subject



/*
 * Following tests should be made

  it should "return a PageResponse with one entity type in"
  it should "read a request and return a HubIssueReplica"
  it should "return null if request with given id was not found"
  it should "read a request with an attachment"
  it should "create a request and return a EntityWriteResult"
  it should "create a request with comments"
  it should "update a request and return a EntityWriteResult"
  it should "update a comment"
  it should "delete a comment"
  it should "search requests by date"
  it should "get the body of an attachment"
 */

class DiscourseApiTest extends Specification {

    @Subject
    DiscourseApi discourseApi
    TopicTest topicTest

    Application application
    Injector injector

    def setup() {
        TestUtils.setupSpec()

        application = Mock(Application)
        injector = Mock(Injector)

        discourseApi = new DiscourseApi(application)
        topicTest = new TopicTest(discourseApi)

    }

    def "searchEntityTypes returns correct entity types"() {
        given:
            String query = "test"
            PageRequest pageRequest = new PageRequest(0, 10)

        when:
            PageResponse<EntityType> result = discourseApi.searchEntityTypes(query, pageRequest)

        then:
            result.results.size() == 1
            result.results[0].name == "topic"
    }

    def "readEntity returns hub issue for valid topic with a number of comments"() {
        given:
            BasicIssueKey entityKey = new BasicIssueKey("7", "7", "topic")


        when:
            IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:

            result != null
            result.key == "7"
            result.summary == "Test Topic to check the test cases"
            result.description == "<p>This topic is to validate the test case</p>"
            result.created.toString() == "Mon Dec 23 12:28:09 CET 2024"


            result.customFields.size() == 1

            // check category custom field
            result.customFields["category"]?.name == "Category"
            result.customFields["category"].id == 1
            result.customFields["category"].uid == "4"


    }

    def "doesEntityExist returns false when asked for a non-existent topic"() {
        given:
            BasicIssueKey entityKey = new BasicIssueKey("", "10000000", "topic")

        when:
            Boolean result = discourseApi.doesEntityExist(entityKey)

        then:
            result == false
    }

    def "writeEntity returns EntityWriteResult with provided entity"() {
        given:
            Topic testTopic = topicTest.createAndFetchTopic("Update entity")
            IIssueKey testTopicKey = new BasicIssueKey(testTopic.id as String, testTopic.topic_id, "topic")


            IHubIssueReplica entityBeforeScript = TopicReplica.toReplica(testTopic)
            testTopic.title = "The new title for the test case  " + System.currentTimeMillis()
            testTopic.raw = "The new raw for the test case  " + System.currentTimeMillis()
            IHubIssueReplica entityAfterScript = TopicReplica.toReplica(testTopic)

            List<INonPersistentTrace> traces = []
            List<StreamableFileMetadata> blobMetadataList = []

        when:
            EntityWriteResult result = discourseApi.writeEntity(testTopicKey, entityBeforeScript, entityAfterScript, traces, blobMetadataList)

        then:
            result != null
            result.entity == entityAfterScript
            result.traces == traces
    }

    BasicHubComment someComment(String commentBody) {
        BasicHubComment hubComment = new BasicHubComment()
        hubComment.author = new BasicHubUser()
        hubComment.author.displayName = "Kwak Dot Duck"
        hubComment.author.email = "kwak318@duck.com"
        hubComment.author.key = "4" // 4 is the user id of the test user kwak318
        hubComment.author.username = "kwak318"
        hubComment.body = commentBody
        hubComment.created = new Date()
        hubComment.internal = false
        return hubComment
    }

    def "create a IssueHubObject of type topic with comments and persist it" () {
        given:
        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.summary = "Create topic with comments from a hubIssue" + System.currentTimeMillis()
        hubIssue.description = "This is a test case to creating a topic with comments from a hubIssue"
        TopicReplica.addCategory(hubIssue, "4")


        hubIssue.comments.add(someComment("This is a test comment " + System.currentTimeMillis()))

        when:
        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(null, null, hubIssue, traces, blobMetadataList)

        then:
        result != null
        result.entity != null
        result.entity?.summary == hubIssue.summary
        result.entity?.comments?.size() == 1
    }

    def  "create a IssueHubObject of type topic with 5 comments and persist it" () {
        given:
        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.summary = "Create topic with comments from a hubIssue" + System.currentTimeMillis()
        hubIssue.description = "This is a test case to creating a topic with comments from a hubIssue"
        TopicReplica.addCategory(hubIssue, "4")

        5.times {Integer counter ->
            hubIssue.comments.add(someComment("This is a test comment number ${counter} " + System.currentTimeMillis()))
        }


        when:
        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(null, null, hubIssue, traces, blobMetadataList)

        then:
        result != null
        result.entity != null
        result.entity?.summary == hubIssue.summary
        result.entity?.comments?.size() == 5

    }

    def "search returns empty page response"() {
        given:
        def query = "test"
        def since = new Date()
        def entityKeyContext = Mock(EntityKeyContext)
        def pageRequest = new PageRequest(0, 10)

        when:
        def result = discourseApi.search(query, since, entityKeyContext, pageRequest)

        then:
        result != null
        result.items.isEmpty()
        result.hasMore
    }

    def "deleteEntity throws an exception"() {
        given:

        when:
        discourseApi.deleteEntity(null)

        then:
        thrown(IssueTrackerException)
    }

    def "uploadFile throws IssueTrackerException"() {
        when:
        discourseApi.uploadFile("test.txt", Mock(Source), null, null)

        then:
        thrown(IssueTrackerException)
    }
}