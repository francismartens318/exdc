package customconnectornode.discourse.http

class DiscourseClientException extends Exception {
    DiscourseClientException(String message) {
        super(message)
    }

    DiscourseClientException(String message, Throwable cause) {
        super(message, cause)
    }
}