package customconnectornode.discourse.domain

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
@Builder
class User {

    private static final Logger log = LoggerFactory.getLogger(User.class)

    Long id
    String username
    String name
    String display_name
    String email
    Date created_at
    Boolean admin
    Boolean moderator
    Boolean staff

    @JsonAnySetter
    Map<String, Object> unknownFields = new HashMap<>()

    static User fromJson(Map userData, String emailData) {
        ObjectMapper mapper = new ObjectMapper()
        User user = mapper.convertValue(userData, User.class)
        user.email = emailData
        user.display_name = user.display_name ?: user.name
        return user
    }
}
