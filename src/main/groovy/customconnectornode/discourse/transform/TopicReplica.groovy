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

package customconnectornode.discourse.transform

import com.exalate.api.domain.IIssueKey
import com.exalate.api.domain.hubobject.v1_2.HubCustomFieldType
import com.exalate.api.domain.hubobject.v1_2.IHubLabel
import com.exalate.api.domain.hubobject.v1_2.IHubUser
import com.exalate.api.domain.hubobject.v1_6.IHubCustomField
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.hubobject.v1.BasicHubComment
import com.exalate.basic.domain.hubobject.v1.BasicHubCustomField
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.basic.domain.hubobject.v1.BasicHubLabel
import com.exalate.basic.domain.hubobject.v1.BasicHubUser
import customconnectornode.discourse.domain.Post
import customconnectornode.discourse.domain.Topic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// convert a topic into a hub issue replica

class TopicReplica {
    private static final Logger logger = LoggerFactory.getLogger(TopicReplica.class)


    private static IHubUser getHubUser(String display_username, String username, Long user_id) {

        IHubUser user = new BasicHubUser()
        user.setDisplayName(display_username)
        user.setUsername(username)
        user.setKey(user_id as String)
        return user
    }

    static IHubCustomField buildCustomField (Long id, String name, String uid, String description, HubCustomFieldType type, String value) {
        IHubCustomField icf = new BasicHubCustomField()

        icf.id = id
        icf.name = name
        icf.uid = uid
        icf.type = type
        icf.value = value
        return icf
    }

    static void addCategory(BasicHubIssue replica, String category) {
        replica.customFields.put("category",
                buildCustomField(1L, "Category", category, "Category section", HubCustomFieldType.STRING, category)
        )
    }
    static BasicHubIssue toReplica(Topic topic) {
        BasicHubIssue replica = new BasicHubIssue()


        replica.id = topic.id as String
        replica.key = topic.topic_id
        replica.created = Utils.getDateFromString(topic.created_at)
        replica.updated = Utils.getDateFromString(topic.updated_at)
        replica.summary = topic.title
        replica.description = topic.cooked
        replica.labels = topic.tags.collect { String tag ->
                                                IHubLabel label = new BasicHubLabel()
                                                label.label = tag
                                            }


        addCategory(replica, topic.category_id as String)

        // add posts as comments, excluding the first post because that is the topic itself

        topic.posts?.drop(1)?.each { post ->
                    def comment = new BasicHubComment()
                    comment.id = post.id
                    comment.body = post.cooked ?: post.raw
                    comment.created = Utils.getDateFromString(post.created_at)
                    comment.updated = Utils.getDateFromString(post.updated_at)
                    comment.author = getHubUser(post.display_username, post.username, post.user_id)
                    replica.comments << comment
                }



        replica.entityKey = toEntityKey(topic)
        replica.setEntityUrl(topic.origin_url)
        return replica
    }


    /*
    **      convert a hub issue replica into a topic
    **      TODO - check the first post (which should be the topic itself)
     */

    static Topic toTopic(BasicHubIssue basicHubIssue) {

        List<Post> posts = []

        posts.add(new Post().builder()
                        .created_at(Utils.getStringFromDate(basicHubIssue.created))
                        .updated_at(Utils.getStringFromDate(basicHubIssue.updated))
                        .raw(basicHubIssue.description)
                        .display_username(basicHubIssue.reporter?.displayName)
                        .username(basicHubIssue.reporter?.username)
                        .topic_id(basicHubIssue.key)
                        .build())

        basicHubIssue.getComments()
                .each { comment ->

                    posts.add(new Post().builder()
                        .id(comment.id)
                        .created_at(Utils.getStringFromDate(comment.created))
                        .updated_at(Utils.getStringFromDate(comment.updated))
                        .raw(comment.body)
                        .topic_id(basicHubIssue.key)
                        .display_username(comment.author.displayName)
                        .username(comment.author.username)
                        .build())
                }



        def tagList = basicHubIssue.getLabels().collect { it.label.toString() }


        Topic topic = new Topic().builder()
                .id(basicHubIssue.id as Long)
                .topic_id(basicHubIssue.key)
                .title(basicHubIssue.summary)
                .raw(basicHubIssue.description)
                .created_at(Utils.getStringFromDate(basicHubIssue.created))
                .updated_at(Utils.getStringFromDate(basicHubIssue.updated))
                .category(basicHubIssue.customFields?.get("category")?.uid as String)
                .category_id(basicHubIssue.customFields?.get("category")?.uid as Integer)
                .tags(tagList)
                .posts(posts)
                .post_number(posts.size())
                .build()
        return topic

    }

    static  IIssueKey toEntityKey(Topic topic) {
        return new BasicIssueKey(topic.id as String, topic.topic_id, "topic")
    }
}
