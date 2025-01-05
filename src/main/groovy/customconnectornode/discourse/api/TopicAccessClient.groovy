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

package customconnectornode.discourse.api

import com.exalate.api.domain.twintrace.INonPersistentTrace
import customconnectornode.discourse.domain.Topic

import java.sql.Timestamp


// TODO document the interface

/*
 * Interface for managing and interacting with topics.
 * Provides functionality to create, retrieve, update, search,
 * and add content (posts) to topics in a topic management system.
 */
interface TopicAccessClient {

    /**
     * Retrieves the details of a topic based on its unique identifier.
     *
     * @param topicId the unique ID of the topic to retrieve.
     * @return the topic object containing its details.
     */
    Topic getTopic(String topicId);

    /**
     * Creates a new topic using the provided details (e.g., title, content, and category).
     *
     * @param topic the topic object containing the necessary data for creation.
     * @return the newly created topic object.
     */
    Topic create(Topic topic);

    /**
     * Updates an existing topic with new information (e.g., modified content or metadata).
     * Non-persistent traces can optionally be included to monitor changes during the update process.
     *
     * @param topic the topic object containing the updated data.
     * @param traces a list of non-persistent trace objects to track update changes.
     * @return a map containing the results or status of the update operation.
     */
    Map<String, Object> update(Topic topic, List<INonPersistentTrace> traces);

    /**
     * Adds a new post or comment to an existing topic.
     *
     * @param topicId the unique identifier of the topic to add the post to.
     * @param content the content of the post to be added.
     * @return a response or confirmation indicating the result of the post addition.
     */
    String addPost(String topicId, String content);

    /**
     * Searches for topics based on a query string and an optional time filter.
     * The time filter allows narrowing down results to topics created or updated after the specified timestamp.
     *
     * @param query the query string used to filter topics by content or title.
     * @param since a timestamp to filter topics created/updated after this time (optional).
     * @return a list of topics that match the search criteria.
     */
    List<Topic> search(String query, Timestamp since);
}
