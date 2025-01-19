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

import com.exalate.api.exception.CategorizedException

/**
 * Custom exception class for handling errors specific to the DiscourseClient API interactions.
 *
 * This exception extends {@link CategorizedException}, likely allowing errors to be categorized
 * based on their context or type. It is designed to provide meaningful error messages and
 * optional metadata (e.g., documentation links, root cause types) when an error occurs during
 * Discourse API operations.
 */
class TopicAccessClientException extends CategorizedException {

    /**
     * Constructor for creating an exception instance with a specific error message.
     *
     * @param message A detailed error message describing what went wrong during the DiscourseClient operation.
     */
    TopicAccessClientException(String message) {
        super("<b>${message}</b><br>")
    }

    /**
     * Placeholder method for returning the link to documentation associated with this exception type.
     *
     * NOTE: This method is currently not implemented and returns a hardcoded placeholder string.
     * In a production-grade application, this would ideally return a URL pointing to relevant
     * documentation or troubleshooting steps.
     *
     * @return A placeholder string ("Not implemented").
     */
    @Override
    String getDocsLink() {
        // TODO: Implement this method to return a URL pointing to relevant documentation or troubleshooting steps.
        return "https://google.com"
    }

    /**
     * Placeholder method for determining the root cause type of this exception.
     *
     * NOTE: This method is currently not implemented and returns a hardcoded placeholder string.
     * This could be used to categorize error types (e.g., "ValidationError", "NetworkError") for
     * improved debugging and error handling in a production environment.
     *
     * @return A placeholder string ("Not implemented").
     */
    @Override
    String getRootCauseErrorTypeName() {
        // TODO: Implement this method to categorize error types (e.g., "ValidationError", "NetworkError") for improved debugging and error handling.
        return "Not implemented"
    }
}