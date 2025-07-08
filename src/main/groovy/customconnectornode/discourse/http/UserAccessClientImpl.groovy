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

package customconnectornode.discourse.http

import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.api.UserAccessClient
import customconnectornode.discourse.domain.User

/**
 * Implementation of the UserAccessClient interface providing access to user information.
 * This class interacts with a Discourse platform to retrieve user details.
 * It delegates HTTP requests to a provided DiscourseClient instance.
 */
class UserAccessClientImpl implements UserAccessClient {

    // A client used to interact with the Discourse API for making HTTP requests.
    private final DiscourseClient discourseClient;

    /**
     * Constructor to initialize the UserAccessClientImpl with the required DiscourseClient.
     *
     * @param discourseClient The client responsible for communicating with the Discourse API.
     */
    UserAccessClientImpl(DiscourseClient discourseClient) {
        this.discourseClient = discourseClient;
    }

    /**
     * Retrieves a user's information, including details and email, from the Discourse platform.
     *
     * @param userId The unique identifier of the user to retrieve information for.
     * @return A User object containing user details and email information.
     * @throws DiscourseClientException if the requested user data or email cannot be retrieved or parsed.
     */
    @Override
    User getUser(String userId) {
        // Fetch the user details in JSON format from the Discourse API.
        Map userJson = discourseClient.get("/u/${userId}.json");
        if (!userJson || !userJson.containsKey("user")) {
            throw new DiscourseClientException("Request for User with ${userId} didn't result in a parseable user");
        }

        // Fetch the user's email details in JSON format from the Discourse API.  Note that the user
        Map emailJson = discourseClient.get("/users/${userId}/emails.json");
        if (!emailJson || !emailJson.containsKey("email")) {
            throw new DiscourseClientException("Request for User with ${userId} didn't result in a parseable information structure.  Is the proper authentication used to retrieve email information?");
        }

        // Create a User object using the fetched user details and email information.
        return User.fromJson(userJson.get("user"), emailJson.get("email"));
    }

    @Override
    String getUserEmailFromUserName(String userName) {
        if(!userName)
            return null
        Map emailJson = discourseClient.get("/users/${userName}/emails.json");
        if (!emailJson || !emailJson.containsKey("email")) {
            throw new DiscourseClientException("Request for User with ${userName} didn't result in a parseable information structure.  Is the proper authentication used to retrieve email information?");
        }
        return emailJson.get("email")
    }
}
