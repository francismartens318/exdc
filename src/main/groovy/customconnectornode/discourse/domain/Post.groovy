package customconnectornode.discourse.domain

import com.fasterxml.jackson.annotation.JsonAnySetter
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import com.fasterxml.jackson.databind.ObjectMapper

@CompileStatic
@Builder
class Post {
    Boolean admin
    String avatar_template
    Boolean bookmarked
    Boolean can_delete
    Boolean can_edit
    Boolean can_recover
    Boolean can_see_hidden_post
    Boolean can_view_edit_history
    Boolean can_wiki
    String cooked      // HTML content
    String created_at
    String deleted_at
    String display_username
    String edit_reason
    Boolean hidden
    Long id
    Integer incoming_link_count
    Boolean moderator
    String name
    Integer post_number
    Integer post_type
    String primary_group_name
    Integer quote_count
    String raw         // Raw content
    Boolean read
    Integer readers_count
    Integer reads
    Integer reply_count
    Integer reply_to_post_number
    Integer score
    Boolean staff
    String topic_slug
    String topic_id
    Boolean trust_level
    String updated_at
    Boolean user_deleted
    Long user_id
    Boolean user_title
    String username
    Boolean version
    Boolean wiki
    Boolean yours

    @JsonAnySetter
    Map<String, Object> unknownFields = new HashMap<>()



    static Post fromJson(Map postData) {
        ObjectMapper mapper = new ObjectMapper()
        return mapper.convertValue(postData, Post.class)
    }


    @Override
    String toString() {
        return "Post(id: $id, topic_id: $topic_id, username: $username)"
    }



}
