package customconnectornode.notion.domain

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class Parent {
    String type // "database_id", "page_id", "workspace", "block_id"
    String database_id
    String page_id
    Boolean workspace
    String block_id
}
