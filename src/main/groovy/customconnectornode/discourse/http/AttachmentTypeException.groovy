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
class AttachmentTypeException extends CategorizedException {

    AttachmentTypeException(String message) {
        super(message)
    }

    @Override
    String getDocsLink() {
        // TODO: Implement this method to return a URL pointing to relevant documentation or troubleshooting steps.
        return "Not implemented"
    }

    @Override
    String getRootCauseErrorTypeName() {
        return "Attachment Type Error"
    }
}