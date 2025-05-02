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

package customconnectornode.discourse.transform

import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.hubobject.v1_2.HubCustomFieldType
import com.exalate.api.domain.hubobject.v1_2.IHubCustomField
import com.exalate.api.domain.hubobject.v1_2.IHubLabel
import com.exalate.api.domain.hubobject.v1_2.IHubUser
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubAttachment
import com.exalate.basic.domain.hubobject.v1.BasicHubComment
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.basic.domain.hubobject.v1.BasicHubLabel
import com.exalate.basic.domain.hubobject.v1.BasicHubUser
import com.fasterxml.jackson.databind.ObjectMapper
import customconnectornode.discourse.domain.Post
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.domain.TopicTestUtil
import customconnectornode.discourse.http.TopicAccessClientException
import spock.lang.Specification
import spock.lang.Subject

class TopicReplicaTest extends Specification {

    @Subject
    TopicReplica topicReplica

    def "toReplica should convert a Topic to a BasicHubIssue"() {
        given:
        Topic topic = new Topic().builder()
                .id(123L)
                .topic_id("123")
                .title("Test Topic")
                .cooked("<p>This is a test topic</p>")
                .created_at("2024-01-01T12:00:00.000Z")
                .updated_at("2024-01-02T12:00:00.000Z")
                .category("General")
                .category_id(4)
                .tags(["test", "spock"])
                .build()
        Post post1 = new Post().builder()
                .id(456L)
                .cooked("<p>This is a test topic</p>")
                .created_at("2024-01-01T12:00:00.000Z")
                .updated_at("2024-01-02T12:00:00.000Z")
                .display_username("Test User")
                .username("testuser")
                .user_id(789L)
                .topic_id("123")
                .post_number(1)
                .build()
        Post post2 = new Post().builder()
                .id(457L)
                .cooked("<p>This is the first test comment</p>")
                .created_at("2024-01-01T13:00:00.000Z")
                .updated_at("2024-01-01T13:00:00.000Z")
                .display_username("Test User")
                .username("testuser")
                .user_id(789L)
                .topic_id("123")
                .post_number(2)
                .build()

        topic.posts = [ post1, post2 ]

        when:
        BasicHubIssue result = TopicReplica.toReplica(topic)

        then:
        result != null
        result.id == "123"
        result.key == "123"
        result.summary == "Test Topic"
        result.description == "<p>This is a test topic</p>"
        result.category == "General"
        result.category_id == 4
        result.labels.size() == 2
        result.labels.collect { it.label }.containsAll(["test", "spock"])
        result.comments.size() == 1 // First post is the topic itself, not a comment
        result.customFields.size() == 1
        result.customFields["category"] != null
        result.customFields["category"].name == "DiscourseCategory"
        result.customFields["category"].uid == "4"
    }

    def "toReplica should handle comments correctly"() {
        given:
        Topic topic = new Topic().builder()
                .id(123L)
                .topic_id("123")
                .title("Test Topic")
                .cooked("<p>This is a test topic</p>")
                .created_at("2024-01-01T12:00:00.000Z")
                .updated_at("2024-01-02T12:00:00.000Z")
                .category("General")
                .category_id(4)
                .build()

        // First post (topic itself)
        Post post1 = new Post().builder()
                .id(456L)
                .cooked("<p>This is the topic post</p>")
                .created_at("2024-01-01T12:00:00.000Z")
                .updated_at("2024-01-01T12:00:00.000Z")
                .display_username("Test User")
                .username("testuser")
                .user_id(789L)
                .topic_id("123")
                .post_number(1)
                .build()

        // Second post (first comment)
        Post post2 = new Post().builder()
                .id(457L)
                .cooked("<p>This is a comment</p>")
                .created_at("2024-01-01T13:00:00.000Z")
                .updated_at("2024-01-01T13:00:00.000Z")
                .display_username("Test User")
                .username("testuser")
                .user_id(789L)
                .topic_id("123")
                .post_number(2)
                .build()

        // Third post (second comment)
        Post post3 = new Post().builder()
                .id(458L)
                .cooked("<p>This is another comment</p>")
                .created_at("2024-01-01T14:00:00.000Z")
                .updated_at("2024-01-01T14:00:00.000Z")
                .display_username("Another User")
                .username("anotheruser")
                .user_id(790L)
                .topic_id("123")
                .post_number(3)
                .build()

        topic.posts = [post1, post2, post3]

        when:
        BasicHubIssue result = TopicReplica.toReplica(topic)

        then:
        result != null
        result.comments.size() == 2
        result.comments[0].id == 457L
        result.comments[0].body == "<p>This is a comment</p>"
        result.comments[0].author.displayName == "Test User"
        result.comments[0].author.username == "testuser"
        result.comments[1].id == 458L
        result.comments[1].body == "<p>This is another comment</p>"
        result.comments[1].author.displayName == "Another User"
        result.comments[1].author.username == "anotheruser"
    }

    def "toReplica should handle attachments correctly"() {
        given:
        Topic topic = new Topic().builder()
                .id(123L)
                .topic_id("123")
                .title("Test Topic")
                .cooked("<p>This is a test topic</p>")
                .created_at("2024-01-01T12:00:00.000Z")
                .updated_at("2024-01-02T12:00:00.000Z")
                .category("General")
                .category_id(4)
                .build()

        Post post = new Post().builder()
                .id(456L)
                .cooked("<p>This is a comment with attachment</p>")
                .created_at("2024-01-01T13:00:00.000Z")
                .updated_at("2024-01-01T13:00:00.000Z")
                .display_username("Test User")
                .username("testuser")
                .user_id(789L)
                .topic_id("123")
                .post_number(2)
                .build()

        post.attachmentIDs = ["attachment1.jpg", "attachment2.pdf"]
        topic.posts = [post]

        when:
        BasicHubIssue result = TopicReplica.toReplica(topic)

        then:
        result != null
        result.attachments.size() == 2
        result.attachments[0].filename == "attachment1.jpg"
        result.attachments[0].author.displayName == "Test User"
        result.attachments[1].filename == "attachment2.pdf"
        result.attachments[1].author.displayName == "Test User"
    }

    def "toTopic should convert a BasicHubIssue to a Topic"() {
        given:
        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.id = "123"
        hubIssue.key = "123"
        hubIssue.summary = "Test Topic with a sufficiently long title"
        hubIssue.description = "This is a test topic with a sufficiently long description"
        hubIssue.created = Utils.getDateFromString("2024-01-01T12:00:00.000Z")
        hubIssue.updated = Utils.getDateFromString("2024-01-02T12:00:00.000Z")
        hubIssue.category = "General"
        hubIssue.category_id = 4

        BasicHubUser reporter = new BasicHubUser()
        reporter.displayName = "Test User"
        reporter.username = "testuser"
        hubIssue.reporter = reporter

        hubIssue.labels = [new BasicHubLabel(label: "test"), new BasicHubLabel(label: "spock")] as Set

        BasicHubComment comment = new BasicHubComment()
        comment.id = "456"
        comment.body = "This is a test comment"
        comment.created = Utils.getDateFromString("2024-01-01T13:00:00.000Z")
        comment.updated = Utils.getDateFromString("2024-01-01T13:00:00.000Z")

        BasicHubUser commentAuthor = new BasicHubUser()
        commentAuthor.displayName = "Comment User"
        commentAuthor.username = "commentuser"
        comment.author = commentAuthor

        hubIssue.comments = [comment]

        when:
        Topic result = TopicReplica.toTopic(hubIssue)

        then:
        result != null
        result.id == 123L
        result.topic_id == "123"
        result.title == "Test Topic with a sufficiently long title"
        result.raw == "This is a test topic with a sufficiently long description"
        result.category == "General"
        result.category_id == 4
        result.tags.size() == 2
        result.tags.containsAll(["test", "spock"])
        result.posts.size() == 2
        result.posts[0].raw == "This is a test topic with a sufficiently long description"
        result.posts[0].display_username == "Test User"
        result.posts[0].username == "testuser"
        result.posts[1].raw == "This is a test comment"
        result.posts[1].display_username == "Comment User"
        result.posts[1].username == "commentuser"
    }

    def "toTopic should validate BasicHubIssue"() {
        given:
        BasicHubIssue hubIssue = new BasicHubIssue()
        hubIssue.id = "123"
        hubIssue.key = "123"
        hubIssue.summary = invalidSummary
        hubIssue.description = invalidDescription
        hubIssue.category_id = invalidCategoryId

        when:
        TopicReplica.toTopic(hubIssue)

        then:
        def exception = thrown(TopicAccessClientException)
        exception.message == expectedMessage

        where:
        invalidSummary      | invalidDescription                      | invalidCategoryId | expectedMessage
        null                | "This is a valid description"           | 4                 | "<b>Summary is null</b><br>"
        "Short"             | "This is a valid description"           | 4                 | "<b>Summary is too short, needs to be at least 15 characters</b><br>"
        "Valid summary ... "| null                                    | 4                 | "<b>Description is null</b><br>"
        "Valid summary ... "| "Short"                                 | 4                 | "<b>Description is too short, needs to be at least 20 characters</b><br>"
        "Valid summary ... "| "This is a valid description"           | null              | "<b>Category_id is null</b><br>"
    }

    def "toEntityKey should create a BasicIssueKey from a Topic"() {
        given:
        Topic topic = new Topic().builder()
                .id(123L)
                .topic_id("123")
                .build()

        when:
        IIssueKey result = TopicReplica.toEntityKey(topic)

        then:
        result != null
        result.id == 123L
        result.key == "123"
        result.entityType == "topic"
    }

    def "buildCustomField should create a BasicHubCustomField with correct values"() {
        when:
        IHubCustomField result = TopicReplica.buildCustomField(1L, "TestField", "test-uid", "Test Description", HubCustomFieldType.STRING, "Test Value")

        then:
        result != null
        result.id == 1L
        result.name == "TestField"
        result.uid == "test-uid"
        result.type == HubCustomFieldType.STRING
        result.value == "Test Value"
    }

    def "getHubUser should create a BasicHubUser with correct values"() {
        when:
        IHubUser result = TopicReplica.getHubUser("Display Name", "username", 123L)

        then:
        result != null
        result.displayName == "Display Name"
        result.username == "username"
        result.key == "123"
    }

    def "addCategory should add category information to a BasicHubIssue"() {
        given:
        BasicHubIssue hubIssue = new BasicHubIssue()

        when:
        TopicReplica.addCategory(hubIssue, 4, "General")

        then:
        hubIssue.category_id == 4
        hubIssue.category == "General"
        hubIssue.customFields.size() == 1
        hubIssue.customFields["category"] != null
        hubIssue.customFields["category"].name == "DiscourseCategory"
        hubIssue.customFields["category"].uid == "4"
        hubIssue.customFields["category"].value == "General"
    }

    def "addCategory should throw exception when category or category_id is null"() {
        given:
        BasicHubIssue hubIssue = new BasicHubIssue()

        when:
        TopicReplica.addCategory(hubIssue, categoryId, categoryName)

        then:
        def exception = thrown(Exception)
        exception.message.contains("Nor category_id nor category provided")

        where:
        categoryId | categoryName
        null       | "General"
        4          | null
        null       | null
    }

    def "fromJson should create a Topic from JSON data"() {
        given:
        def jsonString = getClass().getResource('/json/7.json').text
        def jsonData = new ObjectMapper().readValue(jsonString, Map.class)

        when:
        Topic result = Topic.fromJson(jsonData)

        then:
        result != null
        result.id == 7
        result.title == "Test Topic to check the test cases"
        result.posts.size() > 0
        result.tags.containsAll(["testcase", "spock"])
    }
}
