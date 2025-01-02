package customconnectornode.discourse.http

import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.api.UserAccessClient
import customconnectornode.discourse.domain.User

class UserAccessClientImpl implements UserAccessClient {
    private final DiscourseClient discourseClient

    UserAccessClientImpl(DiscourseClient discourseClient) {
        this.discourseClient = discourseClient
    }

    @Override
    User getUser(String userId) {
        Map userJson = discourseClient.get("/u/${userId}.json")
        if (!userJson || !userJson.containsKey("user")) {
            throw new DiscourseClientException("Request for User with ${userId} didn't result in a parseable user")
        }

        Map emailJson = discourseClient.get("/users/${userId}/emails.json")
        if (!emailJson || !emailJson.containsKey("email")) {

            throw new DiscourseClientException("Request for User with ${userId} didn't result in a parseable user")
        }
        return User.fromJson(userJson.get("user"), emailJson.get("email"))
    }
}
