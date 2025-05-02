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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.exalate.api.domain.IBlobMetadata
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.api.domain.twintrace.TraceAction
import com.exalate.api.domain.twintrace.TraceType
import com.exalate.basic.domain.BasicNonPersistentTrace
import com.exalate.basic.domain.hubobject.v1.BasicHubAttachment
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.domain.http.MultiPartUploadGroovyHttpRequest
import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.domain.StreamableFileMetadata
import spock.lang.Specification
import spock.lang.Subject

class AttachmentClientTest extends Specification {

    @Subject
    AttachmentClient attachmentClient

    DiscourseClient discourseClient

    def setup() {
        discourseClient = Mock(DiscourseClient)
        attachmentClient = new AttachmentClient(discourseClient)
    }

    def "toAttachmentTrace should create a trace for an attachment"() {
        given:
        def responseBody = [url: "/uploads/default/original/1X/abc123.jpg"]
        def hubAttachment = new BasicHubAttachment()
        hubAttachment.setRemoteId("remote-123")

        when:
        def result = AttachmentClient.toAttachmentTrace(responseBody, hubAttachment)

        then:
        result instanceof BasicNonPersistentTrace
        result.localId == "abc123.jpg"
        result.remoteId == "remote-123"
        result.action == TraceAction.NONE
        result.type == TraceType.ATTACHMENT
        result.toSynchronize
    }

    def "hubAttachmentToMultiPartList should convert hub attachment to multipart list"() {
        given:
        // Create a real Source instance instead of mocking
        def source = Source.single(ByteString.fromString("test data"))
        def hubAttachment = new BasicHubAttachment(
                filename: "test.jpg",
                mimetype: "image/jpeg",
                filesize: 12345
        )

        when:
        def result = AttachmentClient.hubAttachmentToMultiPartList(source, hubAttachment)

        then:
        result.size() == 2
        result[0] instanceof MultiPartUploadGroovyHttpRequest.SourceFilePart
        result[1] instanceof MultiPartUploadGroovyHttpRequest.DataPart

        def filePart = result[0] as MultiPartUploadGroovyHttpRequest.SourceFilePart
        filePart.filename == "test.jpg"
        filePart.contentType == "image/jpeg"
        filePart.fileSize == 12345
        
        def dataPart = result[1] as MultiPartUploadGroovyHttpRequest.DataPart
        dataPart.key == "type"
        dataPart.value == "composer"
    }

    def "getAttachmentId should extract ID from URL"() {
        given:
        def map = [url: "/uploads/default/original/1X/abc123.jpg"]

        when:
        def result = AttachmentClient.getAttachmentId(map)

        then:
        result == "abc123.jpg"
    }

    def "addNewAttachmentsToTrace should add traces for new attachments"() {
        given:
        def hubAttachment = new BasicHubAttachment(
                filename: "logo-history.png",  // Changed to match actual filename
                mimetype: "image/png",         // Changed to match actual mimetype
                filesize: 14775                // Changed to match actual filesize
        )
        hubAttachment.setRemoteId("12923")    // Set the remote ID to match actual data
        
        def hubIssue = new BasicHubIssue()
        hubIssue.addedAttachments = [hubAttachment]
        
        def traces = []

        def source = Source.single(ByteString.fromString("test data"))

        def blobMetadata = Mock(IBlobMetadata)
        blobMetadata.getBlobId() >> "12923"   // Changed to match actual remoteId
        
        def fileMetadata = Mock(StreamableFileMetadata)
        fileMetadata.getStreamFn() >> { -> { -> source } }
        fileMetadata.blobMetaData() >> blobMetadata
        
        def blobMetadataList = [fileMetadata]
        
        def uploadResponse = [url: "/uploads/default/original/1X/5efd57efeab26d5c7e369592da01303bc42b7acc.png"]

        when:
        def result = attachmentClient.addNewAttachmentsToTrace(hubIssue, traces, blobMetadataList)

        then:
        1 * discourseClient.uploadAttachment({ List<MultiPartUploadGroovyHttpRequest.IFormPart> parts ->
            parts.size() == 2 &&
            parts[0].filename == "logo-history.png" &&
            parts[0].contentType == "image/png"
        }) >> uploadResponse

        result.size() == 1
        result[0].localId == "5efd57efeab26d5c7e369592da01303bc42b7acc.png"
        result[0].remoteId == "12923"
    }

    def "addNewAttachmentsToTrace should not add trace when file metadata is not found"() {
        given:
        def hubAttachment = new BasicHubAttachment(
                filename: "test.jpg",
                mimetype: "image/jpeg",
                filesize: 12345
        )
        hubAttachment.setRemoteId("remote-123")
        
        def hubIssue = new BasicHubIssue()
        hubIssue.addedAttachments = [hubAttachment]
        
        def traces = []
        def blobMetadataList = []

        when:
        def result = attachmentClient.addNewAttachmentsToTrace(hubIssue, traces, blobMetadataList)

        then:
        result.size() == 0
        0 * discourseClient.uploadAttachment(_)
    }

    def "addNewAttachmentsToTrace should not add trace when source is null"() {
        given:
        def hubAttachment = new BasicHubAttachment(
                filename: "test.jpg",
                mimetype: "image/jpeg",
                filesize: 12345
        )
        hubAttachment.setRemoteId("remote-123")

        def hubIssue = new BasicHubIssue()
        hubIssue.addedAttachments = [hubAttachment]
        
        def traces = []
        
        def blobMetadata = Mock(IBlobMetadata)
        blobMetadata.getBlobId() >> "remote-123"
        
        def fileMetadata = Mock(StreamableFileMetadata)
        fileMetadata.getStreamFn() >> { -> { -> null } }
        fileMetadata.blobMetaData() >> blobMetadata
        
        def blobMetadataList = [fileMetadata]

        when:
        def result = attachmentClient.addNewAttachmentsToTrace(hubIssue, traces, blobMetadataList)

        then:
        result.size() == 0
        0 * discourseClient.uploadAttachment(_)
    }

    def "checkAttachmentAllowed should throw exception when attachment is null"() {
        when:
        AttachmentClient.checkAttachmentAllowed(null)

        then:
        thrown(DiscourseClientException)
    }

    def "checkAttachmentAllowed should throw exception when filename is null"() {
        given:
        def attachment = new BasicHubAttachment(filename: null)

        when:
        AttachmentClient.checkAttachmentAllowed(attachment)

        then:
        thrown(DiscourseClientException)
    }

    def "checkAttachmentAllowed should throw exception for disallowed extension"() {
        given:
        def attachment = new BasicHubAttachment(filename: "test.pdf")

        when:
        AttachmentClient.checkAttachmentAllowed(attachment)

        then:
        def exception = thrown(DiscourseClientException)
        exception.message.contains("pdf")
        exception.message.contains("not allowed")
    }

    def "checkAttachmentAllowed should not throw exception for allowed extension"() {
        given:
        def attachment = new BasicHubAttachment(filename: "test.jpg")

        when:
        AttachmentClient.checkAttachmentAllowed(attachment)

        then:
        noExceptionThrown()
    }

    def "findFileMetadata should find metadata by attachment remote ID"() {
        given:
        def blobMetadata1 = Mock(IBlobMetadata)
        blobMetadata1.getBlobId() >> "remote-123"
        
        def blobMetadata2 = Mock(IBlobMetadata)
        blobMetadata2.getBlobId() >> "remote-456"
        
        def fileMetadata1 = Mock(StreamableFileMetadata)
        fileMetadata1.blobMetaData() >> blobMetadata1
        
        def fileMetadata2 = Mock(StreamableFileMetadata)
        fileMetadata2.blobMetaData() >> blobMetadata2
        
        def fileMetadataList = [fileMetadata1, fileMetadata2]

        when:
        def result = AttachmentClient.findFileMetadata(fileMetadataList, "remote-123")

        then:
        result == fileMetadata1
    }

    def "findFileMetadata should return null when no matching metadata is found"() {
        given:
        def blobMetadata = Mock(IBlobMetadata)
        blobMetadata.getBlobId() >> "remote-123"
        
        def fileMetadata = Mock(StreamableFileMetadata)
        fileMetadata.blobMetaData() >> blobMetadata
        
        def fileMetadataList = [fileMetadata]

        when:
        def result = AttachmentClient.findFileMetadata(fileMetadataList, "remote-456")

        then:
        result == null
    }
}