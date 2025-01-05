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

import com.exalate.api.domain.hubobject.v1_2.IHubUser
import com.exalate.basic.domain.hubobject.v1.BasicHubUser
import com.fasterxml.jackson.annotation.JsonAnySetter
import groovy.transform.CompileStatic
import groovy.transform.builder.Builder
import com.fasterxml.jackson.databind.ObjectMapper
/**
 * Represents a Post model that maps JSON attributes to class members.
 *
 * This class utilizes the Jackson library for serialization and deserialization,
 * allowing seamless conversion between JSON data and class instances. Any fields
 * in the JSON that are not explicitly declared as members of this class will be
 * captured in the `unknownFields` map using the `@JsonAnySetter` annotation.
 *
 * For details about the JSON structure and available fields in the Post object,
 * refer to the official Discourse API documentation:
 * https://docs.discourse.org/
 */

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
    String remote_id // being used to track the posts that have been synced
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



    /**
     * Creates a `Post` instance from a JSON map using the Jackson library.
     *
     * @param postData - A map containing JSON data for a post.
     * @return An instance of the `Post` class.
     */
    static Post fromJson(Map postData) {
        ObjectMapper mapper = new ObjectMapper()
        return mapper.convertValue(postData, Post.class)
    }

    /**
     * Returns a string representation of the `Post` instance.
     *
     * This implementation provides basic information about the post ID,
     * topic ID, and username to assist with debugging and logging.
     *
     * @return A string representation of the post.
     */
    @Override
    String toString() {
        return "Post(id: $id, topic_id: $topic_id, username: $username)"
    }
}

