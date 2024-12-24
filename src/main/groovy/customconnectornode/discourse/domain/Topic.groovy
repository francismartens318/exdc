package customconnectornode.discourse.domain

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.util.logging.Slf4j

@CompileStatic
@Builder
class Topic {
    List actions_summary
    Boolean admin
    Boolean archived
    String archetype
    String avatar_template
    List bookmarks
    Boolean bookmarked
    Boolean can_delete
    Boolean can_edit
    Boolean can_recover
    Boolean can_see_hidden_post
    Boolean can_view_edit_history
    Boolean can_vote
    Boolean can_wiki
    String category
    Integer category_id
    Integer chunk_size
    Boolean closed
    String cooked
    String created_at
    Integer current_post_number
    String deleted_at
    String deleted_by
    Map details
    String display_username
    String discourse_zendesk_plugin_zendesk_id
    String discourse_zendesk_plugin_zendesk_url
    String draft
    String draft_key
    Integer draft_sequence
    String edit_reason
    String fancy_title
    String featured_link
    String flair_bg_color
    String flair_color
    Integer flair_group_id
    String flair_name
    String flair_url
    Boolean has_deleted
    Boolean has_summary
    Boolean hidden
    Integer highest_post_number
    Long id
    String image_url
    Integer incoming_link_count
    Integer last_read_post_id
    Integer last_read_post_number
    String last_posted_at
    Integer like_count
    Integer message_bus_last_id
    Boolean moderator
    String name
    Integer participant_count
    List pending_posts
    Boolean pinned
    Boolean pinned_globally
    String pinned_at
    String pinned_until
    List<Post> posts
    Integer post_number
    Integer posts_count
    Map post_stream
    Boolean posted
    Integer post_type
    String primary_group_name
    Integer queued_posts_count
    Integer quote_count
    String raw
    Integer readers_count
    Integer reads
    List related_topics
    Integer reply_count
    Integer reply_to_post_number
    Integer score
    String slow_mode_enabled_until
    Integer slow_mode_seconds
    String slug
    Boolean show_read_indicator
    Boolean staff
    List suggested_topics
    Boolean summarizable
    List<String> tags
    Map<String, String> tags_descriptions
    String thumbnails
    List timeline_lookup
    String title
    String topic_id
    String topic_timer
    String topic_slug
    Integer trust_level
    String unpinned
    String updated_at
    Integer user_id
    Boolean user_deleted
    Boolean user_voted
    String user_title
    String username
    List valid_reactions
    Integer version
    Integer views
    Boolean visible
    Boolean wiki
    Integer vote_count
    Integer word_count
    Boolean yours

    @JsonAnySetter
    Map<String, Object> unknownFields = new HashMap<>()


    @Override
    String toString() {
        return "Topic(id: $id, title: $title, category: $category, raw: ${raw?.take(20)}...)"
    }


    String isValidForCreation() {
        String result = ""

        if (!this.title) {
            result += "title is required, "
        }

        if (!this.raw) {
            result += "raw is required, "
        }

        if (!this.category) {
            result += "category is required, "
        }

        return result
    }




    static Topic fromJson(Map topicData) {
        ObjectMapper mapper = new ObjectMapper()
        Topic topic = mapper.convertValue(topicData, Topic.class)

        // convert post_stream.posts to List<Post>
        if (topic.post_stream && topic.post_stream.posts) {
            topic.posts = topic.post_stream.posts.collect { postData ->
                return Post.fromJson(postData as Map)
            }
        }


        // ensure topic_id is set if not yet set
        return topic
    }

    Post firstPost() {
        return posts.find { it.post_number == 1 }
    }

}