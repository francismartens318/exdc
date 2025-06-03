package customconnectornode.notion.domain

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class FileObject {
    String type // "file", "file_upload", or "external"
    NotionHostedFile file // if type is "file"
    FileUploadData file_upload // if type is "file_upload"
    ExternalFile external // if type is "external"
    String name // Optional: name of the file as displayed to the user
    String caption // Optional: caption for the file
}

@ToString(includeNames = true, ignoreNulls = true)
class NotionHostedFile {
    String url
    String expiry_time
}

@ToString(includeNames = true, ignoreNulls = true)
class FileUploadData {
    String id // UUID of a File Upload object
}

@ToString(includeNames = true, ignoreNulls = true)
class ExternalFile {
    String url
}
