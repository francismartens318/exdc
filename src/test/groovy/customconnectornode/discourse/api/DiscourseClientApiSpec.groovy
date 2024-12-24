package customconnectornode.discourse.api

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import customconnectornode.discourse.domain.Post
import org.slf4j.LoggerFactory
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.http.DiscourseClientImpl
import io.github.cdimascio.dotenv.Dotenv
import spock.lang.Specification
import spock.lang.Subject

class DiscourseClientApiSpec extends Specification {

    static {
        // Set root logger to ERROR level
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.setLevel(Level.ERROR)

        // Enable DEBUG level for customconnectornode package
        Logger appLogger = (Logger) LoggerFactory.getLogger("customconnectornode")
        appLogger.setLevel(Level.DEBUG)
    }

    def setupSpec() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./ccnode")
                .load()

        System.setProperty("TRACKER_URL", dotenv.get("TRACKER_URL"))
        System.setProperty("TRACKER_API_KEY", dotenv.get("TRACKER_API_KEY"))
        System.setProperty("TRACKER_USER", dotenv.get("TRACKER_USER"))
    }

    @Subject
    DiscourseClient discourseClient = new DiscourseClientImpl(
        System.getProperty("TRACKER_URL"),
        System.getProperty("TRACKER_API_KEY"),
        System.getProperty("TRACKER_USER")
    )

def "should get an existing topic"() {
        given:
        String topicId = "7"
        def testTopic = new Topic().builder()
                .title("Test Topic to check the test cases")
                .category_id(1 as Integer)
                .topic_id(topicId)


        when:
        Topic aTopic = discourseClient.getTopic(testTopic.topic_id).block()
        Post topicPost = aTopic.firstPost()

        then:
        aTopic != null
        aTopic.title == testTopic.title
        aTopic.posts_count == 1
        aTopic.category_id == 4
        topicPost.topic_id == testTopic.topic_id

    }

def "should create a new topic"() {
    given:
        String title = "My Test Topic to check the test cases " + System.currentTimeMillis()
        def testTopic = new Topic().builder()
                .title(title)
                .raw("This is the description of my test topic " + System.currentTimeMillis())
                .category("4")
                .build()


    when:        Topic aTopic = discourseClient
                    .createTopic(testTopic)
                    .block()

    then:
        aTopic != null
        aTopic.raw == testTopic.raw
        aTopic.post_number == 1
        aTopic.id != null
        aTopic.id != 0

    }

def "should add a post to an existing topic"() {
    given:
        // Create a new topic first
        String title = "Topic for Post Test " + System.currentTimeMillis()
        def newTopic = new Topic().builder()
                .title(title)
                .raw("Initial topic content")
                .category("4")
                .build()
        Topic createdTopic = discourseClient.createTopic(newTopic).block()

        // Prepare post content
        def postContent = "This is a new post added to the topic " + System.currentTimeMillis()

    when:
        // Add new post to the created topic
        Topic updatedTopic = discourseClient
                .createPost(createdTopic.topic_id, postContent)
                .block()

    then:
        updatedTopic != null
        updatedTopic.post_number == 2  // Original post + new post
        updatedTopic.raw == postContent
    }

def "should add 5 posts to a single topic"() {
    given:
        // Create initial topic
        String title = "Topic for Multiple Posts Test " + System.currentTimeMillis()
        def initialTopic = new Topic().builder()
                .title(title)
                .raw("Initial topic content for multiple posts")
                .category("4")
                .build()
        Topic createdTopic = discourseClient.createTopic(initialTopic).block()

        // Create 50 posts
        def posts = []
        5.times { index ->
            def postContent = "This is post number ${index + 1} added at " + System.currentTimeMillis()
            Topic post = discourseClient
                    .createPost(createdTopic.topic_id, postContent)
                    .block()
            posts.add(post)
            sleep(1000)  // Add a delay between posts to avoid rate limiting

        }

    when:
        // Get the topic to verify all posts
    Topic finalTopic = discourseClient
                .getTopic(createdTopic.topic_id)
                .block()


    then:
        finalTopic != null
        finalTopic.posts_count == 6  // Initial post + 5 additional posts

        Post topicPost = finalTopic.firstPost()
        topicPost.topic_id == createdTopic.topic_id
        topicPost.cooked == createdTopic.cooked

        posts.size() == 5
        posts.every { it != null }
        posts.every { it.id != null }
    }


}