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

import com.exalate.basic.domain.hubobject.v1.BasicHubComment
import com.exalate.basic.domain.hubobject.v1.BasicHubUser
import customconnectornode.discourse.DiscourseApi
import customconnectornode.discourse.api.TopicAccessClient

class TopicTestUtil {

    TopicAccessClient topicAccessClient

    TopicTestUtil(DiscourseApi dc) {
        topicAccessClient = dc.getTopicAccessClient()
    }

    TopicTestUtil(TopicAccessClient tac) {
        topicAccessClient = tac
    }

    static BasicHubComment someComment(String commentBody) {
        BasicHubComment hubComment = new BasicHubComment()
        hubComment.author = new BasicHubUser()
        hubComment.author.displayName = "Kwak Dot Duck"
        hubComment.author.email = "kwak318@duck.com"
        hubComment.author.key = "4" // 4 is the user id of the test user kwak318
        hubComment.author.username = "kwak318"
        hubComment.body = commentBody
        hubComment.created = new Date()
        hubComment.internal = false
        return hubComment
    }

    // Create a generic topic - do not persist in Discourse

    static Topic createVirtualTopic(String includeTitle = "") {
        return new Topic().builder()
                        .title("My Test Topic to check the test case '${includeTitle}' at ${System.currentTimeMillis()}")
                        .raw("This is the description of my test topic " + System.currentTimeMillis())
                        .category("4")
                        .category_id(4)
                        .created_at()
                        .build()
    }


    // create a generic topic and do persist in Discourse

    Topic createAndFetchTopic(String includeTitle = "") {

        Topic aTopic = createVirtualTopic(includeTitle)
        Topic createdTopic = topicAccessClient.create(aTopic)
        return topicAccessClient.getTopic(createdTopic.topic_id)

    }
}
