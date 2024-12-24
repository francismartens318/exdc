package customconnectornode.discourse.api

import customconnectornode.discourse.domain.Topic
import reactor.core.publisher.Mono


interface DiscourseClient {
    Mono<Topic> getTopic(String topicId)
    Mono<Topic> createTopic(Topic topic)
    Mono<Topic> updateTopic(String topicId, Topic topic)
    Mono<Void> deleteTopic(String topicId)
    Mono<Topic> createPost(String topicId, String content)
}
