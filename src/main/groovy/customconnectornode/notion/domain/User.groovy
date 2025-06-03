package customconnectornode.notion.domain

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class User {
    String object // "user"
    String id
    String type // "person" or "bot"
    String name
    String avatar_url
    Person person // Only for type "person"
    Bot bot // Only for type "bot"
}

@ToString(includeNames = true, ignoreNulls = true)
class Person {
    String email
}

@ToString(includeNames = true, ignoreNulls = true)
class Bot {
    Owner owner
    String workspace_name
    // workspace_limits can be added if needed
}

@ToString(includeNames = true, ignoreNulls = true)
class Owner {
    String type // "workspace" or "user"
    Boolean workspace // if type is "workspace"
    String user_id // if type is "user" - though docs say "user", actual API might differ or this is for future use
}
