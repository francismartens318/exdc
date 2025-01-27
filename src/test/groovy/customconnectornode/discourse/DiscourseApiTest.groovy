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


import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.hubobject.EntityType
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.basic.domain.hubobject.v1.BasicHubLabel
import com.exalate.domain.http.GroovyHttpRequest
import com.exalate.domain.http.GroovyHttpResponse
import com.exalate.replication.services.issuetracker.GroovyHttpClient
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
    private static final Logger log = LoggerFactory.getLogger(DiscourseApiTest.class)


    @Subject
    DiscourseApi discourseApi
    TopicTestUtil topicTestUtil

    Application application
    Injector injector
    GroovyHttpClient groovyHttpClient

    def setup() {
        TestUtils.setupSpec()

        application = Mock(Application)
        injector = Mock(Injector)
        application.injector() >> injector
        groovyHttpClient = Mock(GroovyHttpClient)
        injector.instanceOf(GroovyHttpClient.class) >> groovyHttpClient

        discourseApi = new DiscourseApi(application)
        topicTestUtil = new TopicTestUtil(discourseApi)
    }

    private void mockHttpResponses(List<List<String>> methodBodyPairs) {
        Integer requestStep = 0

        groovyHttpClient.http(_ as GroovyHttpRequest) >> { GroovyHttpRequest request ->
            // assert that the request is what is expected (based on the requestStep) and that the url is valid (based on the regex)
            assert request.method == methodBodyPairs[requestStep][0]
            assert request.url ==~ /^https?:\/\/[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}(\/\S*)?$/

            def jsonString = methodBodyPairs[requestStep][1]
            def response = new GroovyHttpResponse(
                    200,
                    [:],
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


        def jsonString = getClass().getResource('/7.json').text
        def mockResponse = new GroovyHttpResponse(
                200,
                [:],
                { -> jsonString } as Supplier<String>,
                { -> jsonString } as Supplier<Object>
        )

        groovyHttpClient.http(_ as GroovyHttpRequest) >> mockResponse

        when:
            IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:

            result != null
            result.key == "7"
            result.summary == "Test Topic to check the test cases"
            result.description == "<p>This topic is to validate the test case</p>"
//            result.created.toString() == "Mon Dec 23 12:28:09 CET 2024"


            result.customFields.size() == 1

            // check category custom field
            result.customFields["category"]?.name == "Category"
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

        groovyHttpClient.http(_ as GroovyHttpRequest) >> mockResponse

        when:
        IHubIssueReplica result = discourseApi.readEntity(entityKey)

        then:
        !result
    }

    def "doesEntityExist returns false when asked for a non-existent topic"() {
        given:

        BasicIssueKey entityKey = new BasicIssueKey("", "10000000", "topic")
        def jsonString = getClass().getResource('/7.json').text
        def mockResponse = new GroovyHttpResponse(
                200,
                [:],
                { -> jsonString } as Supplier<String>,
                { -> jsonString } as Supplier<Object>
        )

        groovyHttpClient.http(_ as GroovyHttpRequest) >> mockResponse

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

        def jsonString = getClass().getResource('/306.json').text
        def mockResponse = new GroovyHttpResponse(
                200,
                [:],
                { -> jsonString } as Supplier<String>,
                { -> jsonString } as Supplier<Object>
        )

        groovyHttpClient.http(_ as GroovyHttpRequest) >> mockResponse

        when:
        Topic testTopic = topicTestUtil.createAndFetchTopic("Update entity")
        IIssueKey testTopicKey = new BasicIssueKey(testTopic.id as String, testTopic.topic_id, "topic")


        IHubIssueReplica entityBeforeScript = TopicReplica.toReplica(testTopic)
        testTopic.title = "The new title for the test case  " + System.currentTimeMillis()
        testTopic.raw = "The new raw for the test case  " + System.currentTimeMillis()
        IHubIssueReplica entityAfterScript = TopicReplica.toReplica(testTopic)

        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(testTopicKey, entityBeforeScript, entityAfterScript, traces, blobMetadataList)

        then:
        result != null
        result.entity == entityAfterScript
        result.traces == traces
    }

    def "create a IssueHubObject of type topic with comments and persist it" () {
        given:


        // the methodBodyPairs contains the method and the json response for each step in handling an update

        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/638.json').text],
                ["GET", getClass().getResource('/312-initial.json').text],
                ["GET", getClass().getResource('/312-initial.json').text],
                ["POST", getClass().getResource('/639.json').text],
                ["PUT", getClass().getResource('/312-after-put.json').text],
                ["GET", getClass().getResource('/312-final.json').text]

        ]

        mockHttpResponses(methodBodyPairs)

        when:
        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.summary = "Create topic with comments from a hubIssue " + System.currentTimeMillis()
        hubIssue.description = "This is a test case to creating a topic with comments from a hubIssue"
        TopicReplica.addCategory(hubIssue, "4")


        hubIssue.comments.add(TopicTestUtil.someComment("This is a test comment " + System.currentTimeMillis()))


        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(null, null, hubIssue, traces, blobMetadataList)

        then:
        result.entity?.comments?.size() == 1
    }

    def  "create a IssueHubObject of type topic with 5 comments and persist it" () {
        given:
        // the methodBodyPairs contains the method and the json response for each step in handling an update

        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/640.json').text], // create topic
                ["GET", getClass().getResource('/313-initial.json').text], // return fully populated topic as confirmation of the create
                ["GET", getClass().getResource('/313-initial.json').text], // return fully populated topic as preparation for the update
                ["POST", getClass().getResource('/641.json').text], // create comment 1
                ["POST", getClass().getResource('/642.json').text], // create comment 2
                ["POST", getClass().getResource('/643.json').text], // create comment 3
                ["POST", getClass().getResource('/644.json').text], // create comment 4
                ["POST", getClass().getResource('/645.json').text], // create comment 5
                ["PUT", getClass().getResource('/313-after-put.json').text], // update topic with comments
                ["GET", getClass().getResource('/313-final.json').text] // retrieve the final topic with all comments

        ]

        mockHttpResponses(methodBodyPairs)

        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.summary = "Create topic with comments from a hubIssue " + System.currentTimeMillis()
        hubIssue.description = "This is a test case to creating a topic with comments from a hubIssue"
        TopicReplica.addCategory(hubIssue, "4")

        5.times {Integer counter ->
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

    def  "update the tags of an existing IssueHubObject" () {
        given:

        // the methodBodyPairs contains the method and the json response for each step in handling an update

        List<List<String>> methodBodyPairs = [
                ["POST", getClass().getResource('/646.json').text], // create topic
                ["GET", getClass().getResource('/314-initial.json').text], // return fully populated topic as confirmation of the create
                ["GET", getClass().getResource('/314-initial.json').text], // return fully populated topic as preparation for the update
                ["PUT", getClass().getResource('/314-after-put.json').text], // update the tag
                ["GET", getClass().getResource('/314-final.json').text], // create comment 2
                ["POST", getClass().getResource('/643.json').text], // create comment 3
                ["POST", getClass().getResource('/644.json').text], // create comment 4
                ["POST", getClass().getResource('/645.json').text], // create comment 5
                ["PUT", getClass().getResource('/313-after-put.json').text], // update topic with comments
                ["GET", getClass().getResource('/313-final.json').text] // retrieve the final topic with all comments

        ]

        mockHttpResponses(methodBodyPairs)



        when:
        Topic testTopic = topicTestUtil.createAndFetchTopic("TagTester")


        BasicHubIssue testTopicReplicaBefore = TopicReplica.toReplica(testTopic)
        BasicHubIssue testTopicReplicaAfter = TopicReplica.toReplica(testTopic)
        testTopicReplicaAfter.labels = [new BasicHubLabel(label: "Tag tester")] as Set

        List<INonPersistentTrace> traces = []
        List<StreamableFileMetadata> blobMetadataList = []
        EntityWriteResult result = discourseApi.writeEntity(testTopicReplicaBefore.entityKey,testTopicReplicaBefore , testTopicReplicaAfter, traces, blobMetadataList)

        then:
        result != null
        result.entity != null
        result.entity?.labels?.size() == 1
        result.entity?.labels?.first()?.label == "tag-tester"
    }

    def "search since now returns empty page response as there are no topics created after now"() {
        given:
        List<List<String>> methodBodyPairs = [
                ["GET", getClass().getResource('/search-01.json').text], // return a list of topics and posts that are matching any query
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
                ["GET", getClass().getResource('/search-01.json').text], // return a list of topics and posts that are matching any query
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
                ["GET", getClass().getResource('/search-02.json').text], // return a list of topics and posts that are matching any query
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
                ["GET", getClass().getResource('/search-03.json').text], // return a list of topics and posts that are matching any query
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


    // TODO: Support attachments
    def "uploadFile throws IssueTrackerException"() {
        assert true
    }
}