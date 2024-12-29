package customconnectornode.discourse.config

import customconnectornode.discourse.api.TopicAccessClient
import customconnectornode.discourse.http.TopicAccessClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DiscourseConfig {
    @Bean
    TopicAccessClient discourseClient(
            @Value('${discourse.base-url}') String baseUrl,
            @Value('${discourse.api-key}') String apiKey,
            @Value('${discourse.api-userName}') String apiUserName) {
        new TopicAccessClientImpl(baseUrl, apiKey, apiUserName)
    }
}