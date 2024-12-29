package customconnectornode.discourse.transform

import com.exalate.api.domain.hubobject.v1_2.HubCustomFieldType
import com.exalate.api.domain.hubobject.v1_6.IHubCustomField
import com.exalate.basic.domain.hubobject.v1.BasicHubComment
import com.exalate.basic.domain.hubobject.v1.BasicHubCustomField
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import customconnectornode.discourse.domain.Topic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// convert a topic into a hub issue replica

class TopicReplicaBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TopicReplicaBuilder.class)

    static IHubCustomField buildCustomField (Long id, String name, Integer uid, String description, HubCustomFieldType type, String value) {
        IHubCustomField icf = new BasicHubCustomField()

        icf.id = id
        icf.name = name
        icf.uid = uid
        icf.type = type
        icf.value = value
        return icf
    }

    static BasicHubIssue build(Topic topic) {
        BasicHubIssue replica = new BasicHubIssue()

        //BasicIssueKey key = new BasicIssueKey(topic.id as String, topic.topic_id, DiscourseEntity.TOPIC.label)

        replica.setKey(topic.topic_id)
        replica.setCreated(Utils.getDateFromString(topic.created_at))
        replica.summary = topic.title
        replica.description = topic.cooked ?: topic.raw

        // add category
        replica.customFields.put("category",
                buildCustomField(1L, "Category", topic.category_id, "Category", HubCustomFieldType.STRING, topic.category)
        )

        // add posts as comments

        topic.posts?.each { post ->
                    def comment = new BasicHubComment()
                    comment.body = post.cooked ?: post.raw
                    comment.created = Utils.getDateFromString(post.created_at)
                    comment.updated = Utils.getDateFromString(post.updated_at)
                    comment.author = post.getHubUser()
                    replica.comments << comment
                }


        return replica
    }





    // given a topic, build a hub issue replica


}
