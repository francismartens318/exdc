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

class UserAccessClientImpl implements UserAccessClient {
    private final DiscourseClient discourseClient

    UserAccessClientImpl(DiscourseClient discourseClient) {
        this.discourseClient = discourseClient
    }

    @Override
    User getUser(String userId) {
        Map userJson = discourseClient.get("/u/${userId}.json")
        if (!userJson || !userJson.containsKey("user")) {
            throw new DiscourseClientException("Request for User with ${userId} didn't result in a parseable user")
        }

        Map emailJson = discourseClient.get("/users/${userId}/emails.json")
        if (!emailJson || !emailJson.containsKey("email")) {
            throw new DiscourseClientException("Request for User with ${userId} didn't result in a parseable user")
        }
        return User.fromJson(userJson.get("user"), emailJson.get("email"))
    }
}
