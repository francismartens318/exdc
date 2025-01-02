package customconnectornode.discourse.domain

import customconnectornode.discourse.DiscourseApi
import customconnectornode.discourse.api.TopicAccessClient
import org.spockframework.runtime.SpecificationContext

class TopicTest {

    TopicAccessClient topicAccessClient
    SpecificationContext specificationContext

    TopicTest(DiscourseApi dc) {
        topicAccessClient = dc.getTopicAccessClient()
    }

    TopicTest(TopicAccessClient tac) {
        topicAccessClient = tac
    }


    // Create a generic topic - do not persist in Discourse

    static Topic createVirtualTopic(String includeTitle = "") {
        return new Topic().builder()
                        .title("My Test Topic to check the test case '${includeTitle}' at ${System.currentTimeMillis()}")
                        .raw("This is the description of my test topic " + System.currentTimeMillis())
                        .category("4")
                        .category_id(4)
                        .created_at()
                        .build()
    }


    // create a generic topic and do persist in Discourse

    Topic createAndFetchTopic(String includeTitle = "") {

        Topic aTopic = createVirtualTopic(includeTitle)
        Topic createdTopic = topicAccessClient.create(aTopic)
        return topicAccessClient.getTopic(createdTopic.topic_id)

    }
}
