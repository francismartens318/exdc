package customconnectornode.discourse.http


import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.domain.Topic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import org.springframework.http.MediaType

class DiscourseClientImpl implements DiscourseClient {
    private static final Logger log = LoggerFactory.getLogger(DiscourseClientImpl.class);

    private final WebClient webClient
    private final String baseUrl
    private final String apiKey
    private final String apiUsername

    DiscourseClientImpl(String baseUrl, String apiKey, String apiUsername) {
        this.baseUrl = baseUrl
        this.apiKey = apiKey
        this.apiUsername = apiUsername

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader('Api-Key', apiKey)
                .defaultHeader('Api-Username', apiUsername)
                .build()
    }

    @Override
    Mono<Topic> getTopic(Long topicId) {
        webClient
                .get()
                .uri("/t/${topicId}.json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Topic.class)
                .map { createdTopic ->
                    createdTopic.mapPosts()
                    return createdTopic
                }
    }

    @Override
    Mono<Topic> createTopic(Topic topic) {
        webClient.post()
                .uri('/posts')
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(topic)
                .retrieve()
                .bodyToMono(Topic)
    }

    @Override
    Mono<Topic> updateTopic(Long topicId, Topic topic) {
        webClient.put()
                .uri("/t/${topicId}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(topic)
                .retrieve()
                .bodyToMono(Topic)
    }

    @Override
    Mono<Void> deleteTopic(Long topicId) {
        webClient.delete()
                .uri("/t/${topicId}")
                .retrieve()
                .bodyToMono(Void)
    }
}
