package customconnectornode.discourse.config

import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.http.DiscourseClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DiscourseConfig {
    @Bean
    DiscourseClient discourseClient(
            @Value('${discourse.base-url}') String baseUrl,
            @Value('${discourse.api-key}') String apiKey,
            @Value('${discourse.api-userName}') String apiUserName) {
        new DiscourseClientImpl(baseUrl, apiKey, apiUserName)
    }
}