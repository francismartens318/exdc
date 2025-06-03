package customconnectornode.notion.domain

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class EmojiObject {
    String type // "emoji" or "custom_emoji"
    String emoji // if type is "emoji"
    CustomEmoji custom_emoji // if type is "custom_emoji"
}

@ToString(includeNames = true, ignoreNulls = true)
class CustomEmoji {
    String id
    String name
    String url
}
