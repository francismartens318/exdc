package customconnectornode.discourse.api


import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.http.DiscourseClientImpl
import spock.lang.Specification
import spock.lang.Subject

class DiscourseClientApiSpec extends Specification {

    @Subject
    DiscourseClient discourseClient = new DiscourseClientImpl(
            "https://community.exalate.com",
            "0b63fdeff9ce8385fc856b47c8138fa09c0f337d528ce82933997fd391022995",
            "xl8bot"
    )

def "should get an existing topic"() {
        given:
        Long topicId = 5005L
        def testTopic = new Topic().builder()
                .title("Stop Sync if the issue is closed")
                .category_id(1 as Integer)
                .id(topicId)


        when:
        Topic aTopic = discourseClient.getTopic(testTopic.id).block()

        then:
        aTopic != null
        aTopic.title == testTopic.title
        aTopic.posts_count == 2
        aTopic.category_id == 6
        aTopic.id == testTopic.id
    }

def "should create a new topic"() {
    given:
        def testTopic = new Topic().builder()
                .title("My Test Topic")
                .category_id(6 as Integer)
                .build()


    when:
        Topic aTopic = discourseClient
                    .createTopic(testTopic)
                    .block()

    then:
        aTopic != null
        aTopic.title == testTopic.title
        aTopic.posts_count == 1
        aTopic.category_id == 6
        aTopic.id != null
        aTopic.id != 0

    }


}
