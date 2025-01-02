package customconnectornode.discourse.api

import customconnectornode.discourse.domain.Topic


// TODO document the interface

interface TopicAccessClient {
    Topic getTopic(String topicId)

    /*
    *   Create the topic using the title, raw and category
     */
    Topic create(Topic topic)

    /*
     * Update the topic as follows
     */

    Topic update(Topic topic)

    Void deleteTopic(String topicId)

    void addPost(String topicId, String content)

    List<String> searchTriggers(String queryString)

    List<String> latestUpdated(Long since)
}

