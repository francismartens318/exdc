/*
 * Copyright (c) 2024 Exalate (https://exalate.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package customconnectornode.discourse.domain

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/**
 * Represents a topic in a Discourse forum.
 * This class models the numerous properties and metadata that describe a topic, its states,
 * related posts, and additional dynamic or optional fields.
 *
 * It also provides functionality for:
 * - Parsing JSON data to populate the topic object fields from an external source.
 * - Extracting key-related information, such as content, posts, and category identifiers.
 * - Customizing behavior when dealing with unknown or dynamic fields from JSON responses.
 */
@CompileStatic
@Builder
class Topic {
    private static final Logger log = LoggerFactory.getLogger(Topic.class)

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
    List thumbnails
    List timeline_lookup
    String title
    String topic_id

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
    String origin_url // not a field in the json
    Map<String, String> accepted_answer
//TODO - clean the structure of the topic object
    @JsonAnySetter
    Map<String, Object> unknownFields = new HashMap<>()


    /**
     * Identifies if a topic has an accepted/solved answer and creates a standardized acceptance structure.
     *
     * The resulting topic will contain an 'accepted' field with:
     * - username: the user who provided the accepted answer
     * - displayName: the full name of the user
     * - date: timestamp when the answer was accepted
     * - url: full URL to the accepted answer post
     *
     * @param topic The Topic object to process
     * @return The updated Topic with standardized acceptance information
     */
    static private Topic identify_if_solved(Topic topic) {
        if (topic.accepted_answer) {
            int postNumber = topic.accepted_answer.post_number as int

            if (topic.posts) {
                Post acceptedPost = topic.posts.find { post ->
                    post.post_number == postNumber
                }

                if (acceptedPost) {
                    String baseUrl = (topic.origin_url ?: '').split('/t')[0]
                    String postUrl = "${baseUrl}/t/${topic.slug}/${topic.id}/${postNumber}".toString()

                    // add the extra fields from the post
                    topic.accepted_answer += [
                            displayName: acceptedPost.display_username,
                            date: acceptedPost.created_at,
                            url: postUrl
                    ]

                    // remove the name field because it is always null and confusing
                    topic.accepted_answer.remove('name')
                }
            }
        }

        return topic
    }
    /**
     * Converts the Topic object into a readable string representation.
     *
     * This method overrides `toString()` for debugging purposes or when logging
     * detailed information about a specific topic. It summarizes key values
     * such as the ID, title, and category ID for quick reference.
     *
     * @return String - A concise representation of a Topic instance.
     */
    @Override
    String toString() {
        return "Topic(id: $id, title: $title, category_id: $category_id ...)"
    }

    /**
     * Factory method for creating a `Topic` instance from JSON-like data.
     *
     * This method accepts a map of topic data (as returned from a JSON API) and
     * converts it into a structured `Topic` object. It also processes nested structures,
     * such as `post_stream.posts`, to extract `Post` objects.
     *
     * Additional metadata, such as the origin URL, can also be assigned to the
     * `Topic` object for tracing data sources.
     *
     * @param topicData - A `Map` containing key-value pairs representing topic data.
     * @param sourceUrl - (Optional) The URL source from where the topic data originated.
     * @return Topic - A fully instantiated `Topic` object.
     */
    static Topic fromJson(Map topicData, String sourceUrl = null) {
        ObjectMapper mapper = new ObjectMapper()

        // Normalize tags: Discourse API may return tags as objects (e.g., [{"name": "tag1", ...}])
        // instead of simple strings (e.g., ["tag1", "tag2"])
        if (topicData.tags instanceof List) {
            topicData.tags = (topicData.tags as List).collect { tag ->
                tag instanceof Map ? tag.name?.toString() : tag?.toString()
            }
        }

        Topic topic = mapper.convertValue(topicData, Topic.class)

        if (!topic) {
            log.error("Failed to create Topic from JSON data: $topicData")
            return null
        }

        // Process nested field `post_stream.posts` and convert to Post objects.
        if (topic.post_stream && topic.post_stream.posts) {
            topic.posts = topic.post_stream.posts.collect { postData ->
                return Post.fromJson(postData as Map)
            }
        }
        // Assign the source URL for record-keeping if provided.
        topic.origin_url = sourceUrl

        topic = identify_if_solved(topic)

        return topic
    }

    /**
     * Retrieves the first post in the topic's list of posts, if available.
     *
     * This method is useful for extracting the initial post of a topic, often
     * considered the original or primary post when no explicit post ordering
     * exists.
     *
     * @return Post - The first post within the `posts` list, or `null` if no posts exist.
     */
    Post firstPost() {
        return posts.find { it.post_number == 1 }
    }

    /**
     * Fetches the unique identifier of the topic (Topic ID).
     *
     * If the topic's ID field (`topic_id`) is not explicitly set, this method
     * attempts to retrieve it based on the first post's topic ID. This ensures
     * a fallback mechanism when topic-level data is incomplete.
     *
     * @return String - The identifier for the topic.
     */
    String getTopic_id() {
        return topic_id ?: posts?.first()?.topic_id
    }

    /**
     * Retrieves the cooked (rendered HTML) content of the topic.
     *
     * Cooked content refers to the processed, finalized version of a topic's
     * content (e.g., HTML formatted). This method derives it dynamically
     * from the first post in the topic's posts list.
     *
     * @return String - The cooked (HTML-rendered) content for the topic.
     */
    String getCooked() {
        return posts?.first()?.cooked
    }

    /**
     * Retrieves the raw (unprocessed) content of the topic.
     *
     * Raw content is typically the plain-text or unformatted version of a
     * topic's content, often retrieved from the topic's first post. This raw
     * data may include Markdown or another lightweight syntax, depending on
     * the input format.
     *
     * @return String - The raw (unprocessed) content for the topic.
     */
    String getRaw() {
        return posts?.first()?.raw ?: raw
    }

    /**
     * Returns the category ID of the topic.
     *
     * This method acts as an accessor for the `category_id` field, representing
     * the unique identifier for the topic's category.
     *
     * @return Integer - The category ID for the topic.
     */
    Integer getCategory_id() {
        return category_id
    }
}