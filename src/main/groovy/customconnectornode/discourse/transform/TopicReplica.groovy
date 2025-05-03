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
import com.exalate.api.domain.twintrace.TraceAction
import com.exalate.api.domain.twintrace.TraceType
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.basic.domain.BasicNonPersistentTrace
import com.exalate.basic.domain.hubobject.v1.BasicHubAttachment
import com.exalate.basic.domain.hubobject.v1.BasicHubComment
import com.exalate.basic.domain.hubobject.v1.BasicHubCustomField
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.basic.domain.hubobject.v1.BasicHubLabel
import com.exalate.basic.domain.hubobject.v1.BasicHubUser
import customconnectornode.discourse.domain.AttachmentMetaData
import customconnectornode.discourse.domain.Post
import customconnectornode.discourse.domain.Topic
import customconnectornode.discourse.http.DiscourseClientException
import customconnectornode.discourse.http.TopicAccessClientException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// convert a topic into a hub issue replica

class TopicReplica {
    private static final Logger log = LoggerFactory.getLogger(TopicReplica.class)


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

    static BasicNonPersistentTrace toCommentTrace(String localCommentId, String remoteCommentId) {
        BasicNonPersistentTrace trace = new BasicNonPersistentTrace()
                .setType(TraceType.COMMENT)
                .setToSynchronize(true)
                .setLocalId(localCommentId as String)
                .setRemoteId(remoteCommentId as String)
                .setAction(TraceAction.NONE)
        return trace
    }

    static void addCategory(BasicHubIssue replica, Integer category_id, String category) {
        if (!category || !category_id) {
            throw DiscourseClientException("Nor category_id nor category provided ... ")
        }

        replica.category_id = category_id
        replica.category = category

        replica.customFields.put("category",
                buildCustomField(1L, "DiscourseCategory", category_id as String, "DiscourseCategory section", HubCustomFieldType.STRING, category)
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
                                                return label
                                            }


        addCategory(replica, topic.category_id , topic.category)


        Integer commentsAdded = 0

        // add posts as comments, excluding the first post because that is the topic itself
        topic.posts?.each { post ->
                    def comment = new BasicHubComment()
                    comment.id = post.id
                    comment.body = post.cooked ?: post.raw
                    comment.created = Utils.getDateFromString(post.created_at)
                    comment.updated = Utils.getDateFromString(post.updated_at)
                    comment.author = getHubUser(post.display_username, post.username, post.user_id)

                    addAttachmentsToReplica(replica, comment, post.attachmentIDs)

                    // Don't add the topic itself as a comment
                    if (commentsAdded++ > 0) replica.comments << comment
                }

        replica.entityKey = toEntityKey(topic)

        // INTSNS-70 - ensure the url of the entity is the appropriate one
        replica.setEntityUrl(topic.origin_url?.replaceAll('\\.json$', ''))
        replica.accepted_answer = topic.accepted_answer
        return replica
    }


    private static void addAttachmentsToReplica(BasicHubIssue replica, BasicHubComment sourceComment, List<String> postAttachments) {

        // first collect all potential attachments from the post
        List<BasicHubAttachment> attachments = postAttachments.collect { String attachmentID ->
            BasicHubAttachment attachment = new BasicHubAttachment()
            attachment.id = attachmentID
            attachment.filename = attachmentID

            // TODO - get the mimetype from the attachment
            attachment.mimetype = "application/octet-stream"

            attachment.author = sourceComment.author
            attachment.created = sourceComment.created
            attachment.internal = false // there is no such thing as internal comments
            attachment.zip = false

            attachment
        }


        // check what attachments are new and add them to the replica.attachment list
        List<BasicHubAttachment> attachmentsToAdd = replica.getAttachments()
        attachments.each { BasicHubAttachment attachment ->
            if (!attachmentsToAdd.find { it.filename == attachment.filename}) {
                attachmentsToAdd << attachment
            }
        }

        replica.setAttachments(attachmentsToAdd)
    }

    /*
    **      convert a hub issue replica into a topic
    **      TODO - check the first post (which should be the topic itself)
     */

    static Topic toTopic(BasicHubIssue basicHubIssue) {

        List<Post> posts = []

        validateBasicHubIssue(basicHubIssue)

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
                        .topic_id(basicHubIssue.key as String)
                        .display_username(comment.author.displayName)
                        .remote_id(comment.remoteId as String)
                        .username(comment.author.username)
                        .build())
                }

        def tagList = basicHubIssue.getLabels().collect { it.label.toString() }


        Topic topic = new Topic().builder()
                .id(basicHubIssue.id as Long)
                .topic_id(basicHubIssue.key as String)
                .title(basicHubIssue.summary as String)
                .raw(basicHubIssue.description as String)
                .created_at(Utils.getStringFromDate(basicHubIssue.created as Date))
                .updated_at(Utils.getStringFromDate(basicHubIssue.updated as Date))
                .category_id(basicHubIssue.category_id)
                .category(basicHubIssue.category)
                .tags(tagList)
                .posts(posts)
                .post_number(posts.size())
                .build()
        return topic

    }

    static  IIssueKey toEntityKey(Topic topic) {
        log.debug("Creating issue key for topic ${topic.id}")

        return new BasicIssueKey(topic.id as String, topic.id as String, "topic")
    }

    /**
     * Validates if a BasicHubIssue meets the requirements to be considered a valid topic.
     *
     * @param basicHubIssue The BasicHubIssue to validate
     * @throws TopicAccessClientException if any validation fails:
     *         - If basicHubIssue is null
     *         - If summary is null or less than 15 characters
     *         - If description is null or less than 20 characters
     *         - If category_id is null
     */

    private static void validateBasicHubIssue(BasicHubIssue basicHubIssue) {
        if (!basicHubIssue) {
            throw new TopicAccessClientException("BasicHubIssue is null")
        }

        if (!basicHubIssue.summary) {
            throw new TopicAccessClientException("Summary is null")
        }

        if (basicHubIssue.summary.length() < 15) {
            throw new TopicAccessClientException("Summary is too short, needs to be at least 15 characters")
        }

        if (!basicHubIssue.description) {
            throw new TopicAccessClientException("Description is null")
        }

        if (basicHubIssue.description.length() < 20) {
            throw new TopicAccessClientException("Description is too short, needs to be at least 20 characters")
        }

        if (!basicHubIssue.category_id) {
            throw new TopicAccessClientException("Category_id is null")
        }
    }

    static void checkBasicHubIssueIsGoodTopic(BasicHubIssue basicHubIssue) {
        if (!basicHubIssue) {
            throw new TopicAccessClientException("BasicHubIssue is null")
        }

        if (!basicHubIssue.summary) {
            throw new TopicAccessClientException("Summary is null")
        }

        if (basicHubIssue.summary.length() < 15) {
            throw new TopicAccessClientException("Summary is too short, needs to be at least 15 characters")
        }

        if (!basicHubIssue.description) {
            throw new TopicAccessClientException("Description is null")
        }

        if (basicHubIssue.description.length() < 20) {
            throw new TopicAccessClientException("Description is too short, needs to be at least 20 characters")
        }

        if (!basicHubIssue.category_id) {
            throw new TopicAccessClientException("Category_id is null")
        }

    }
}
