package customconnectornode.discourse.api

import customconnectornode.discourse.domain.User
import reactor.core.publisher.Mono

interface UserAccessClient {
    User getUser(String userId)
}