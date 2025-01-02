package customconnectornode.discourse.api


import customconnectornode.discourse.TestUtils
import customconnectornode.discourse.domain.Post
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.domain.TopicTest
import customconnectornode.discourse.http.DiscourseClientImpl
import customconnectornode.discourse.http.TopicAccessClientImpl
import spock.lang.Specification
import spock.lang.Subject

class TopicAccessClientTest extends Specification {

    @Subject
    TopicAccessClient topicAccessClient
    TopicTest topicTest

    def setup() {
        TestUtils.setupSpec()

        topicAccessClient = new TopicAccessClientImpl(new DiscourseClientImpl())
        topicTest = new TopicTest(topicAccessClient)

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




        when:
        Topic aTopic = topicTest.createAndFetchTopic("topic creation")

        then:
        aTopic != null
        aTopic.category_id == 4
        aTopic.posts_count == 1
    }

    def "should add a post to an existing topic"() {
        given:
        // Create a new topic first
        Topic aTopic = topicTest.createAndFetchTopic("Add post to topic")

        // Prepare post content
        def postContent = "This is a new post added to the topic " + System.currentTimeMillis()

        when:
        topicAccessClient.addPost(aTopic.topic_id, postContent)
        Topic fetchedTopic = topicAccessClient.getTopic(aTopic.topic_id)

        then:
        fetchedTopic != null
        fetchedTopic.title == aTopic.title
        fetchedTopic.posts[0].username == System.getProperty("TRACKER_USER")
        fetchedTopic.posts_count == 2
    }

    def "should add 5 posts to a single topic"() {
        given:
        // Create a new topic first
        Topic aTopic = topicTest.createAndFetchTopic("Add 5 posts to topic")


        // Create 50 posts
        def posts = []
        5.times { index ->
            def postContent = "This is post number ${index + 1} added at " + System.currentTimeMillis()
            topicAccessClient.addPost(aTopic.topic_id, postContent)
        }

        when:
        // Get the topic to verify all posts
        Topic finalTopic = topicAccessClient.getTopic(aTopic.topic_id)


        then:
        finalTopic != null
        finalTopic.posts_count == 6  // Initial post + 5 additional posts

        Post topicPost = finalTopic.firstPost()
        topicPost.topic_id == aTopic.topic_id
    }

    def "should return topics created by xl8bot"() {
        given:
        Topic aTopic = topicTest.createAndFetchTopic("Find proxy user topics")
        String trackerUser = System.getProperty("TRACKER_USER")

        when:
        String queryForXl8Bot = "@${trackerUser}"
        List<String> topicIds = topicAccessClient.searchTriggers(queryForXl8Bot)

        then:
        topicIds != null
        !topicIds.isEmpty()
        topicIds.contains(aTopic.topic_id)
        Topic topic = topicAccessClient.getTopic(topicIds[0])
        topic.posts[0]?.username == trackerUser
    }

    def "should return topics created 2 seconds ago"() {
        given:
        long currentMillis = System.currentTimeMillis()
        String trackerUser = System.getProperty("TRACKER_USER")
        Topic aTopic = topicTest.createAndFetchTopic("Search topics created not more than 2 seconds ago")

        sleep(2000)  // Wait for 2 seconds

        when:

        List<String> topicIds = topicAccessClient.latestUpdated(currentMillis)

        then:
        topicIds != null
        !topicIds.isEmpty()
        topicIds.contains(aTopic.topic_id)

        // the number of topics should be limited
        topicIds.size() < 10

    }

    def "should only update the topic raw, but not the title"() {
        given:
        Topic aTopic = topicTest.createAndFetchTopic("Update raw")

        when:
        Long currentTimeMillis = System.currentTimeMillis()
        aTopic.raw = "This is the updated raw content of the topic " + currentTimeMillis
        Topic fetchedTopic = topicAccessClient.update(aTopic)


        then:
        fetchedTopic != null
        fetchedTopic.title == aTopic.title

        // check if the cooked is different (raw is not available)
        fetchedTopic.cooked != aTopic.cooked
        fetchedTopic.cooked.contains(currentTimeMillis.toString())
    }

    def "should add existing tags"() {
        given:
        Topic aTopic = topicTest.createAndFetchTopic("tag update check")

        when:
        Long currentTimeMillis = System.currentTimeMillis()
        aTopic.tags = ["testcase", "spock"]
        Topic fetchedTopic = topicAccessClient.update(aTopic)


        then:
        fetchedTopic != null
        fetchedTopic.title == aTopic.title
        fetchedTopic.tags?.contains("testcase")
        fetchedTopic.tags?.contains("spock")
    }
}