package customconnectornode.discourse.api


interface DiscourseClient {
    Map get(String path, Map<String, Object> params)
    Map post(String path, Object payload,  Map<String, Object> params)
    Map doPut(String path, Object payload,  Map<String, Object> params)
}