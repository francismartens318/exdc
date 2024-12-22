package customconnectornode.discourse.domain

import groovy.transform.CompileStatic
import groovy.transform.builder.Builder

@CompileStatic
@Builder
class Topic {
    Long id
    String title
    String fancy_title
    String raw
    String category
    Integer category_id
    String last_posted_at
    String created_at
    String deleted_at
    Integer posts_count
    Integer views
    Integer reply_count
    Integer like_count
    Integer word_count
    List<String> tags
    Map<String, String> tags_descriptions
    Boolean visible
    Boolean closed
    Boolean archived
    Boolean has_summary
    String slug

    // Stream and lookup
    Map post_stream
    List<Post> posts
    List timeline_lookup
    List suggested_topics

    // Additional fields from the response
    String archetype
    Integer user_id
    String featured_link
    Boolean pinned_globally
    String pinned_at
    String pinned_until
    String image_url
    Integer slow_mode_seconds
    String draft
    String draft_key
    Integer draft_sequence
    Boolean posted
    String unpinned
    Boolean pinned
    Integer current_post_number
    Integer highest_post_number
    Integer last_read_post_number
    Integer last_read_post_id
    String deleted_by
    Boolean has_deleted
    List actions_summary
    Integer chunk_size
    Boolean bookmarked
    String topic_timer
    Integer message_bus_last_id
    Integer participant_count
    Integer queued_posts_count
    Boolean show_read_indicator
    String thumbnails
    String slow_mode_enabled_until
    List related_topics
    Boolean summarizable
    List valid_reactions
    Boolean can_vote
    Integer vote_count
    Boolean user_voted
    String discourse_zendesk_plugin_zendesk_id
    String discourse_zendesk_plugin_zendesk_url
    Map details
    List pending_posts
    List bookmarks

    @Override
    String toString() {
        return "Topic(id: $id, title: $title, category_id: $category_id)"
    }


    void mapPosts() {
        if (post_stream && post_stream.posts) {
            posts = post_stream.posts.collect { postData ->
                Post.builder()
                        .id(postData['id'] as Long)
                        .topic_id(id)
                        .name(postData['name'] as String)
                        .username(postData['username'] as String)
                        .created_at(postData['created_at'] as String)
                        .updated_at(postData['updated_at'] as String)
                        .cooked(postData['cooked'] as String)
                        .raw(postData['raw'] as String)
                        .post_number(postData['post_number'] as Integer)
                        .build()
            }
        }
    }


}