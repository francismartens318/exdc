package customconnectornode.notion.domain

// TODO: Define User, FileObject, EmojiObject, and Parent classes
class Page {
    String object
    String id
    String created_time
    User created_by
    String last_edited_time
    User last_edited_by
    boolean archived
    boolean in_trash
    Object icon // FileObject or EmojiObject
    FileObject cover
    Map properties
    Parent parent
    String url
    String public_url
}
