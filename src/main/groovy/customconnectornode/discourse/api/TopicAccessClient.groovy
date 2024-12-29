package customconnectornode.discourse.api

import customconnectornode.discourse.domain.Topic


// TODO document the interface

interface TopicAccessClient {
    Topic getTopic(String topicId)

    // Return the topic id of the newly created topic
    String createTopic(String title, String raw, String category)

    Topic updateTopic(String topicId, Topic topic)

    Void deleteTopic(String topicId)

    void addPost(String topicId, String content)

    List<String> searchTriggers(String queryString)

    List<String> latestUpdated(Long since)
}

