package customconnectornode.discourse.api

import customconnectornode.discourse.domain.Topic
import reactor.core.publisher.Mono


interface DiscourseClient {
    Mono<Topic> getTopic(Long topicId)
    Mono<Topic> createTopic(Topic topic)
    Mono<Topic> updateTopic(Long topicId, Topic topic)
    Mono<Void> deleteTopic(Long topicId)
}
