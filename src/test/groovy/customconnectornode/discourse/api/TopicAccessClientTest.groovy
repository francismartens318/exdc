package customconnectornode.discourse.api


import customconnectornode.discourse.TestUtils
import customconnectornode.discourse.domain.Post
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.http.DiscourseClientImpl
import customconnectornode.discourse.http.TopicAccessClientImpl
import spock.lang.Specification
import spock.lang.Subject

class TopicAccessClientTest extends Specification {

    @Subject
    TopicAccessClient topicAccessClient

    def setup() {
        TestUtils.setupSpec()

        topicAccessClient = new TopicAccessClientImpl(new DiscourseClientImpl())
    }

def "should get an existing topic"() {
    given:
        String topic_id = "7"

    when:
        Topic aTopic = topicAccessClient.getTopic(topic_id)

    then:
        aTopic != null
        aTopic.title == "Test Topic to check the test cases"
        aTopic.posts_count == 6
        aTopic.category_id == 4
        aTopic.topic_id == "7"
    }

def "should create a new topic"() {
    given:

        String title = "My Test Topic to check the test cases " + System.currentTimeMillis()
        String raw = "This is the description of my test topic " + System.currentTimeMillis()
        String category = "4"


    when:
        String createdTopicId = topicAccessClient.createTopic(title,raw, category)
        Topic fetchedTopic = topicAccessClient.getTopic(createdTopicId)

    then:
        fetchedTopic != null
        fetchedTopic.title == title
        fetchedTopic.category_id == 4
        fetchedTopic.posts_count == 1
    }

def "should add a post to an existing topic"() {
    given:
        // Create a new topic first
        String title = "Topic for Post Test " + System.currentTimeMillis()
        String raw = "This is the description of my test topic " + System.currentTimeMillis()
        String category = "4"

        // Prepare post content
        def postContent = "This is a new post added to the topic " + System.currentTimeMillis()

    when:
        String createdTopicId = topicAccessClient.createTopic(title, raw, category)
        topicAccessClient.addPost(createdTopicId, postContent)
        Topic fetchedTopic = topicAccessClient.getTopic(createdTopicId)

    then:
        fetchedTopic != null
        fetchedTopic.title == title
        fetchedTopic.posts[0].username == System.getProperty("TRACKER_USER")
        fetchedTopic.posts_count == 2
    }

def "should add 5 posts to a single topic"() {
    given:
        // Create initial topic
        String title = "Topic for Multiple Posts Test " + System.currentTimeMillis()
        String raw = "Initial topic content for multiple posts"
        String category = "4"

        String createdTopicId = topicAccessClient.createTopic(title, raw, category)


        // Create 50 posts
        def posts = []
        5.times { index ->
            def postContent = "This is post number ${index + 1} added at " + System.currentTimeMillis()
            topicAccessClient.addPost(createdTopicId, postContent)
        }

    when:
        // Get the topic to verify all posts
        Topic finalTopic = topicAccessClient.getTopic(createdTopicId)


    then:
        finalTopic != null
        finalTopic.posts_count == 6  // Initial post + 5 additional posts

        Post topicPost = finalTopic.firstPost()
        topicPost.topic_id == createdTopicId
    }

def "should return topics created by xl8bot"() {
    given:
        String trackerUser = System.getProperty("TRACKER_USER")
        // Create initial topic
        String title ="${trackerUser} Test Topic " + System.currentTimeMillis()
        String raw = "Content created by ${trackerUser}"
        String category = "4"

        String createdTopicId = topicAccessClient.createTopic(title, raw, category)


    when:
        String queryForXl8Bot = "@${trackerUser}"
        List<String> topicIds = topicAccessClient.searchTriggers(queryForXl8Bot)

    then:
        topicIds != null
        !topicIds.isEmpty()
        topicIds.contains(createdTopicId)
        Topic topic = topicAccessClient.getTopic(topicIds[0])
        topic.posts[0]?.username == trackerUser
    }

def "should return topics created 10 seconds ago"() {
    given:
        long currentMillis = System.currentTimeMillis()
        String trackerUser = System.getProperty("TRACKER_USER")
        // Create initial topic
        String title ="${trackerUser} Test Topic " + System.currentTimeMillis()
        String raw = "Content created by ${trackerUser} to validate the latestUpdated method"
        String category = "4"

        String createdTopicId = topicAccessClient.createTopic(title, raw, category)
        sleep(1000)  // Wait for 9 seconds

    when:

        List<String> topicIds = topicAccessClient.latestUpdated(currentMillis)

    then:
        topicIds != null
            !topicIds.isEmpty()

        // Assuming tht no other topics were created in the last 10 seconds
        topicIds[0] == createdTopicId

    }
}