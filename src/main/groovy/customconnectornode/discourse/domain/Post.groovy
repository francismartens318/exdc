package customconnectornode.discourse.domain

import groovy.transform.CompileStatic
import groovy.transform.builder.Builder

@CompileStatic
@Builder
class Post {
    Long id
    Long topic_id
    String name
    String username
    String created_at
    String updated_at
    String cooked      // HTML content
    String raw         // Raw content
    Integer post_number
    Integer reply_count
    Integer quote_count
    Integer incoming_link_count
    Integer reads
    Integer readers_count
    Integer score
    Boolean yours
    String topic_slug
    String topic_title
    String display_username
    String version
    Boolean can_edit
    Boolean can_delete
    Boolean can_recover
    Boolean can_wiki
    Boolean read
    Boolean user_title
    List<String> actions_summary
    Boolean moderator
    Boolean admin
    Boolean staff
    Integer user_id
    Boolean hidden
    Boolean trust_level
    String deleted_at
    String user_deleted
    Boolean wiki
    Map<String, Object> polls
    List<String> reactions

    @Override
    String toString() {
        return "Post(id: $id, topic_id: $topic_id, username: $username)"
    }
}
