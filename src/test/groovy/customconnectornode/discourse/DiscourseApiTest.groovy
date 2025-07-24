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
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.basic.domain.hubobject.v1.BasicHubLabel
import com.exalate.domain.http.GroovyHttpRequest
import com.exalate.domain.http.GroovyHttpResponse
import com.exalate.domain.http.StreamingGroovyHttpResponse
import com.exalate.replication.services.issuetracker.HttpClient
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.domain.TopicTestUtil
import customconnectornode.discourse.http.DiscourseClientException
import customconnectornode.discourse.transform.TopicReplica
import customconnectornode.discourse.transform.Utils
import customconnectornode.domain.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.Application
import play.api.inject.Injector
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Supplier

class DiscourseApiTest extends Specification {
    @Subject
    DiscourseApi discourseApi


    private static final Logger log = LoggerFactory.getLogger(DiscourseApiTest.class)
    TopicTestUtil topicTestUtil

    Application application
    Injector injector
    HttpClient httpClient

    def setup() {
        TestUtils.setupSpec()

        application = Mock(Application)
        injector = Mock(Injector)
        application.injector() >> injector
        httpClient = Mock(HttpClient)
        injector.instanceOf(HttpClient.class) >> httpClient

        discourseApi = new DiscourseApi(application)
        topicTestUtil = new TopicTestUtil(discourseApi)
    }

    private void mockHttpResponses(List<List<String>> methodBodyPairs) {
        Integer requestStep = 0

        httpClient.http(_ as GroovyHttpRequest) >> { GroovyHttpRequest request ->
            log.debug("Processing requestStep ${requestStep} with ${request}")
            // assert that the request is what is expected (based on the requestStep) and that the url is valid (based on the regex)
            assert requestStep < methodBodyPairs.size(), "Request step out of bounds"
            assert request.method == methodBodyPairs[requestStep][0], "Request method does not match expected value "
            assert request.url ==~ /^https?:\/\/[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}(\/\S*)?$/


            def jsonString = methodBodyPairs[requestStep][1] ?: "{}"
            def headers = methodBodyPairs[requestStep].size() > 2 ? methodBodyPairs[requestStep][2] : [:]
            def response = new GroovyHttpResponse(
                    200,
                    headers as Map<String, List<String>>,
                    { -> jsonString } as Supplier<String>,
                    { -> jsonString } as Supplier<Object>
            )

            requestStep++
            return response
        }
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



        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/7.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/admin_emails.json').text],
                ["GET", getClass().getResource('/json/francis_emails.json').text]
        ]

        mockHttpResponses(methodBodyPairs)


        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:

        result != null
        result.key == "7"
        result.summary == "Test Topic to check the test cases"
        result.description == "<p>This topic is to validate the test case</p>"
        result.category == "General"
        result.category_id == 4


        result.customFields.size() == 1

        // check category custom field
        result.customFields["category"]?.name == "DiscourseCategory"
        result.customFields["category"].id == 1
        result.customFields["category"].uid == "4"

    }

    def "readEntity returns null for a non-existent topic"() {
        given:
        BasicIssueKey entityKey = new BasicIssueKey("", "10000000", "topic")
        def jsonString = ""
        def mockResponse = new GroovyHttpResponse(
                404,
                [:],
                { -> jsonString } as Supplier<String>,
                { -> jsonString } as Supplier<Object>
        )

        httpClient.http(_ as GroovyHttpRequest) >> mockResponse

        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:
        !result
    }

    def "doesEntityExist returns false when asked for a non-existent topic"() {
        given:

        BasicIssueKey entityKey = new BasicIssueKey("", "10000000", "topic")

        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/7.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
        ]

        mockHttpResponses(methodBodyPairs)

        when:
        Boolean result = discourseApi.doesEntityExist(entityKey)

        then:
        result == false
    }

    def "doesEntityExist throws exception when asked for something else than a topic"() {
        given:

        BasicIssueKey entityKey = new BasicIssueKey("", "10000000", "blurb")


        when:
        Boolean result = discourseApi.doesEntityExist(entityKey)

        then:
        thrown(DiscourseClientException)
    }

    def "writeEntity returns EntityWriteResult with provided entity"() {
        given:


        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/json/306.json').text],        // create the test topic
                ["GET", getClass().getResource('/json/306.json').text],         // fetch the test topic
                ["GET", getClass().getResource('/json/categories.json').text],  // fetch the categories
                ["GET", getClass().getResource('/json/306.json').text],         // fetch the resulting topic (after the write)
                ["PUT", getClass().getResource('/json/306.json').text],         // Put an update
                ["GET", getClass().getResource('/json/306-after-update.json').text],         // Get the updated topic
        ]

        mockHttpResponses(methodBodyPairs)

        when:
        Topic testTopic = topicTestUtil.createAndFetchTopic("Update entity")
        IIssueKey testTopicKey = new BasicIssueKey(testTopic.id as String, testTopic.topic_id, "topic")


        IHubIssueReplica entityBeforeScript = TopicReplica.toReplica(testTopic)
        testTopic.title = "The new title for the test case"
        testTopic.raw = "The new raw for the test case  " + System.currentTimeMillis()
        IHubIssueReplica entityAfterScript = TopicReplica.toReplica(testTopic)

        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(testTopicKey, entityBeforeScript, entityAfterScript, traces, blobMetadataList)

        then:
        result?.entity != null
        testTopic.title == entityAfterScript.summary
        result.traces == traces
    }

    def "update a IssueHubObject with 2 comments and persist it" () {
        given:


        // the methodBodyPairs contains the method and the json response for each step in handling an update

        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/json/638.json').text],
                ["GET", getClass().getResource('/json/312-initial.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/312-initial.json').text],
                ["POST", getClass().getResource('/json/639.json').text],
                ["PUT", getClass().getResource('/json/312-after-put.json').text],
                ["GET", getClass().getResource('/json/312-final.json').text]

        ]

        mockHttpResponses(methodBodyPairs)

        when:

        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.summary = "Create topic with comments from a hubIssue " + System.currentTimeMillis()
        hubIssue.description = "This is a test case to creating a topic with comments from a hubIssue"
        TopicReplica.addCategory(hubIssue, 4, "General")


        hubIssue.comments.add(TopicTestUtil.someComment("This is a test comment " + System.currentTimeMillis()))


        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(null, null, hubIssue, traces, blobMetadataList)

        then:
        result.entity?.comments?.size() == 1
    }

    def "create a IssueHubObject of type topic with comments and persist it"() {
        given:


        // the methodBodyPairs contains the method and the json response for each step in handling an update

        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/json/638.json').text],
                ["GET", getClass().getResource('/json/312-initial.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/312-initial.json').text],
                ["POST", getClass().getResource('/json/639.json').text],
                ["PUT", getClass().getResource('/json/312-after-put.json').text],
                ["GET", getClass().getResource('/json/312-final.json').text]

        ]

        mockHttpResponses(methodBodyPairs)

        when:
        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.summary = "Create topic with comments from a hubIssue " + System.currentTimeMillis()
        hubIssue.description = "This is a test case to creating a topic with comments from a hubIssue"
        TopicReplica.addCategory(hubIssue, 4, "General")


        hubIssue.comments.add(TopicTestUtil.someComment("This is a test comment " + System.currentTimeMillis()))


        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(null, null, hubIssue, traces, blobMetadataList)

        then:
        result.entity?.comments?.size() == 1
    }

    def "create a IssueHubObject of type topic with 5 comments and persist it"() {
        given:
        // the methodBodyPairs contains the method and the json response for each step in handling an update

        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/json/640.json').text], // create topic
                ["GET", getClass().getResource('/json/313-initial.json').text], // return fully populated topic as confirmation of the create
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/313-initial.json').text], // return fully populated topic as preparation for the update
                ["POST", getClass().getResource('/json/641.json').text], // create comment 1
                ["POST", getClass().getResource('/json/642.json').text], // create comment 2
                ["POST", getClass().getResource('/json/643.json').text], // create comment 3
                ["POST", getClass().getResource('/json/644.json').text], // create comment 4
                ["POST", getClass().getResource('/json/645.json').text], // create comment 5
                ["PUT", getClass().getResource('/json/313-after-put.json').text], // update topic with comments
                ["GET", getClass().getResource('/json/313-final.json').text] // retrieve the final topic with all comments

        ]

        mockHttpResponses(methodBodyPairs)

        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.summary = "Create topic with comments from a hubIssue " + System.currentTimeMillis()
        hubIssue.description = "This is a test case to creating a topic with comments from a hubIssue"
        TopicReplica.addCategory(hubIssue, 4, "General")

        5.times { Integer counter ->
            hubIssue.comments.add(TopicTestUtil.someComment("This is a test comment number ${counter} " + System.currentTimeMillis()))
        }


        when:
        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(null, null, hubIssue, traces, blobMetadataList)

        then:
        result != null
        result.entity != null
        result.entity?.comments?.size() == 5

    }

    def "update the tags of an existing IssueHubObject"() {
        given:

        // the methodBodyPairs contains the method and the json response for each step in handling an update

        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/json/646.json').text], // create topic
                ["GET", getClass().getResource('/json/314-initial.json').text], // return fully populated topic as confirmation of the create
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/314-initial.json').text], // return fully populated topic as preparation for the update
                ["PUT", getClass().getResource('/json/314-after-put.json').text], // update the tag
                ["GET", getClass().getResource('/json/314-final.json').text], // create comment 2
                ["POST", getClass().getResource('/json/643.json').text], // create comment 3
                ["POST", getClass().getResource('/json/644.json').text], // create comment 4
                ["POST", getClass().getResource('/json/645.json').text], // create comment 5
                ["PUT", getClass().getResource('/json/313-after-put.json').text], // update topic with comments
                ["GET", getClass().getResource('/json/313-final.json').text] // retrieve the final topic with all comments

        ]

        mockHttpResponses(methodBodyPairs)


        when:
        Topic testTopic = topicTestUtil.createAndFetchTopic("TagTester")


        BasicHubIssue testTopicReplicaBefore = TopicReplica.toReplica(testTopic)
        BasicHubIssue testTopicReplicaAfter = TopicReplica.toReplica(testTopic)
        testTopicReplicaAfter.labels = [new BasicHubLabel(label: "Tag tester")] as Set

        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(testTopicReplicaBefore.entityKey, testTopicReplicaBefore, testTopicReplicaAfter, traces, blobMetadataList)

        then:
        result != null
        result.entity != null
        result.entity?.labels?.size() == 1
        result.entity?.labels?.first()?.label == "tag-tester"
    }

    def "search since now returns empty page response as there are no topics created after now"() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/search-01.json').text], // return a list of topics and posts that are matching any query
        ]

        mockHttpResponses(methodBodyPairs)

        def query = "kwik kwak kwek, a duck is not a chicken"
        def since = Utils.getDateFromString("2025-01-04T18:33:44.000Z") // the result set in search-01.json is from 2025-01-04T18:33:44.000Z
        def entityKeyContext = new EntityKeyContext("topic", [:])
        def pageRequest = new PageRequest(0, 10)


        when:
        def result = discourseApi.search(query, since, entityKeyContext, pageRequest)

        then:
        result != null
        result.results.isEmpty()
    }

    def "search query is safely escaped"() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/search-01.json').text], // return a list of topics and posts that are matching any query
        ]

        mockHttpResponses(methodBodyPairs)

        def query = "kwik kwak kwek, a duck is not a chicken"
        def since = Utils.getDateFromString("2025-01-04T18:33:44.000Z") // the result set in search-01.json is from 2025-01-04T18:33:44.000Z
        def entityKeyContext = new EntityKeyContext("topic", [:])
        def pageRequest = new PageRequest(0, 10)


        when:
        def result = discourseApi.search(query, since, entityKeyContext, pageRequest)

        then:
        result != null
        result.results.isEmpty()
    }

    def "A trigger is returning the expected topics "() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/search-02.json').text], // return a list of topics and posts that are matching any query
        ]


        mockHttpResponses(methodBodyPairs)

        def query = "hubissue" // search-02.json contains the topics on community.exalate.st that contains the word hubissue
        def entityKeyContext = new EntityKeyContext("topic", [:])

        def pageRequest = new PageRequest(0, 10)


        when:
        def result = discourseApi.search(query, null, entityKeyContext, pageRequest)

        then:
        result != null
        result.results.size() == 22
    }

    def "A trigger is returning the expected topics with a tag"() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/search-03.json').text], // return a list of topics and posts that are matching any query
        ]

        mockHttpResponses(methodBodyPairs)

        def query = "tags:tag-tester" // search-03.json contains the topics on community.exalate.st that contains the tag 'tag-tester'
        def entityKeyContext = new EntityKeyContext("topic", [:])

        def pageRequest = new PageRequest(0, 10)


        when:
        def result = discourseApi.search(query, null, entityKeyContext, pageRequest)

        then:
        result != null
        result.results.size() == 1
    }

    def "The replica of a topic with one post with one attachment lists the single attachment in the attachments list"() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/317.json').text], // return a list of topics and posts that are matching any query
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/admin_emails.json').text],
                ["GET", getClass().getResource('/json/system_emails.json').text],
                ["GET", getClass().getResource('/json/1117b3d1cd715f2a4c56408a4cec986285a55d8f-meta.json').text], // return a list of topics and posts that are matching any query

        ]
        mockHttpResponses(methodBodyPairs)

        BasicIssueKey entityKey = new BasicIssueKey("317", "317", "topic")

        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:
        result.attachments.size() == 1
    }

    def "The replica of a topic with one post with two attachments lists the two attachments in the attachments list"() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/319.json').text], // return a list of topics and posts that are matching any query["GET", getClass().getResource('/json/1117b3d1cd715f2a4c56408a4cec986285a55d8f-meta.json').text], // return a list of topics and posts that are matching any query
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/1117b3d1cd715f2a4c56408a4cec986285a55d8f-meta.json').text], // return a list of topics and posts that are matching any query
                ["GET", getClass().getResource('/json/admin_emails.json').text]
        ]
        mockHttpResponses(methodBodyPairs)

        BasicIssueKey entityKey = new BasicIssueKey("319", "319", "topic")

        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:
        result.attachments.size() == 1
    }

    def "The replica of a topic with two attachments in the description and 3 posts with one identical attachment results in 2 attachments"() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/320.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/admin_emails.json').text],
                ["GET", getClass().getResource('/json/fca98506638111b38fc57bb49ed7ac6384a66567-meta.json').text], // return a list of topics and posts that are matching any query
                ["GET", getClass().getResource('/json/1117b3d1cd715f2a4c56408a4cec986285a55d8f-meta.json').text], // return a list of topics and posts that are matching any query

        ]
        mockHttpResponses(methodBodyPairs)

        BasicIssueKey entityKey = new BasicIssueKey("320", "320", "topic")

        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:
        result.attachments.size() == 2
        result.comments.size() == 3
    }

    def "The download of a certain attachment contains what we expect"() {
        given:
        def imageBytes = getClass().getResource('/json/1117b3d1cd715f2a4c56408a4cec986285a55d8f.jpeg').bytes
        def byteString = ByteString.fromArray(imageBytes)
        def source = Source.single(byteString)

        def mockResponseDownload = new StreamingGroovyHttpResponse(
                200,
                [:],
                source
        )

        httpClient.download(_ as GroovyHttpRequest) >> mockResponseDownload

        BasicIssueKey entityKey = new BasicIssueKey("319", "319", "topic")

        when:
        Source<ByteString, ?> result = discourseApi.getFileBodyStream("1117b3d1cd715f2a4c56408a4cec986285a55d8f.jpeg", entityKey, null)

        then:
        result != null
    }

    def "The download of an attachment that doesn't exists results in an exception"() {
        given:


        def mockResponse = new StreamingGroovyHttpResponse(
                404,
                [:],
                null
        )

        httpClient.download(_ as GroovyHttpRequest) >> mockResponse

        BasicIssueKey entityKey = new BasicIssueKey("319", "319", "topic")

        when:
        Source<ByteString, ?> result = discourseApi.getFileBodyStream("blurb.jpeg", entityKey, null)

        then:
        thrown(DiscourseClientException)
    }

    def "A post with a single attachment is reporting the correct filesize and mimetype"() {
        given:


        def fileMetaHeaders = [
                "content-length":["237202"],
                "Content-Type":["image/jpeg"],
        ]

        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/json/319.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", null, fileMetaHeaders],  // return the headers which are expected at this stage of the test.
                ["GET", getClass().getResource('/json/admin_emails.json').text]
        ]

        mockHttpResponses(methodBodyPairs)



        BasicIssueKey entityKey = new BasicIssueKey("319", "319", "topic")

        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:
        result != null
        result.attachments.size() == 1
        result.attachments[0].filename == "1117b3d1cd715f2a4c56408a4cec986285a55d8f.jpeg"
        result.attachments[0].filesize == 237202
        result.attachments[0].mimetype == "image/jpeg"
    }

    def "The download of a certain attachment contains what we expect"() {
        given:
        def imageBytes = getClass().getResource('/json/1117b3d1cd715f2a4c56408a4cec986285a55d8f.jpeg').bytes
        def byteString = ByteString.fromArray(imageBytes)
        def source = Source.single(byteString)

        def mockResponseDownload = new StreamingGroovyHttpResponse(
                200,
                [:],
                source
        )

        httpClient.download(_ as GroovyHttpRequest) >> mockResponseDownload

        BasicIssueKey entityKey = new BasicIssueKey("319", "319", "topic")

        when:
        Source<ByteString, ?> result = discourseApi.getFileBodyStream("1117b3d1cd715f2a4c56408a4cec986285a55d8f.jpeg", entityKey, null)

        then:
        result != null
    }

    def "The download of an attachment that doesn't exists results in an exception"() {
        given:


        def mockResponse = new StreamingGroovyHttpResponse(
                404,
                [:],
                null
        )

        httpClient.download(_ as GroovyHttpRequest) >> mockResponse

        BasicIssueKey entityKey = new BasicIssueKey("319", "319", "topic")

        when:
        Source<ByteString, ?> result = discourseApi.getFileBodyStream("blurb.jpeg", entityKey, null)

        then:
        thrown(DiscourseClientException)
    }


    def "readEntity returns hub issue where the resolution is accepted"() {
        given:
        BasicIssueKey entityKey = new BasicIssueKey("333", "333", "topic")



        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/solved/topic_333.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/admin_emails.json').text]

        ]

        mockHttpResponses(methodBodyPairs)


        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:

        result != null
        result.key == "333"
        result.accepted_answer != null
        result.accepted_answer.username == "admin"
        result.accepted_answer.displayName == "Francis Martens"
        result.accepted_answer.date == "2025-03-25T23:10:52.984Z"
        result.accepted_answer.excerpt == "this might be a good solution"
        result.accepted_answer.url == "https://community.exalate.st/t/exdc-0213-take-1-long-summary/333/4"
    }


    def "readEntity returns hub empty accepted_answer where there is no solution"() {
        given:
        BasicIssueKey entityKey = new BasicIssueKey("7", "7", "topic")



        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/solved/topic_7.json').text],
                ["GET", getClass().getResource('/json/categories.json').text],
                ["GET", getClass().getResource('/json/admin_emails.json').text],
                ["GET", getClass().getResource('/json/francis_emails.json').text]
        ]

        mockHttpResponses(methodBodyPairs)


        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:

        result != null
        result.key == "7"
        result.accepted_answer == null
    }

}