package customconnectornode.discourse.api

import customconnectornode.discourse.TestUtils
import customconnectornode.discourse.domain.User
import customconnectornode.discourse.http.DiscourseClientImpl
import customconnectornode.discourse.http.UserAccessClientImpl
import spock.lang.Specification

class UserAccessClientTest extends Specification {

    UserAccessClient userAccessClient

    def setup() {
        TestUtils.setupSpec()

        userAccessClient = new UserAccessClientImpl(new DiscourseClientImpl())
    }

    def "should get user by username"() {
        given:
        def username = "kwak318"


        when:
        User result = userAccessClient.getUser(username)

        then:
        result.id == 4
        result.username == username
        result.name == "Kwak Dot Duck"
        result.email == "kwak318@duck.com"
    }
}