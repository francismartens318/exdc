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
import com.exalate.api.domain.twintrace.INonPersistentTrace
import com.exalate.api.domain.twintrace.TraceAction
import com.exalate.api.domain.twintrace.TraceType
import com.exalate.basic.domain.BasicNonPersistentTrace
import com.exalate.basic.domain.hubobject.v1.BasicHubAttachment
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.domain.http.GroovyHttpResponse
import com.exalate.domain.http.MultiPartUploadGroovyHttpRequest
import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.domain.StreamableFileMetadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
** Blatently copied from the Exalate Basic Connector Node made by Klerman
 */
class AttachmentClient {
    static Logger logger = LoggerFactory.getLogger(AttachmentClient.class)


    private DiscourseClient discourseClient

    // Constructor to initialize the client with the DiscourseClient dependency.
    AttachmentClient(DiscourseClient dc) {
        this.discourseClient = dc
    }

    static BasicNonPersistentTrace toAttachmentTrace(Map<String, Object> responseBody, BasicHubAttachment hubAttachment) {

        String attachmentId =  getAttachmentId(responseBody)
        BasicNonPersistentTrace trace = new BasicNonPersistentTrace()
                .setLocalId(attachmentId)
                .setRemoteId(hubAttachment.remoteIdStr)
                .setAction(TraceAction.NONE)
                .setType(TraceType.ATTACHMENT)
                .setToSynchronize(true)
        return trace
    }



    static List<MultiPartUploadGroovyHttpRequest.IFormPart> hubAttachmentToMultiPartList(Source<ByteString, ?> source, BasicHubAttachment hubAttachment) {
        List<MultiPartUploadGroovyHttpRequest.IFormPart> parts = [
                new MultiPartUploadGroovyHttpRequest.SourceFilePart(
                        "file",
                        hubAttachment.filename,
                        hubAttachment.mimetype,
                        source,
                        hubAttachment.filesize,
                        null
                ),
                new MultiPartUploadGroovyHttpRequest.DataPart("type", "composer"),

        ]
        return parts
    }

    static String getAttachmentId(Map<String, Object> stringObjectMap) {
        String url = stringObjectMap.url
        return  url.tokenize('/')[-1]
    }

    List<INonPersistentTrace> addNewAttachmentsToTrace(BasicHubIssue entity, List<INonPersistentTrace> traces, List<StreamableFileMetadata> blobMetadataList) {

        entity.getAddedAttachments().inject(traces) {
            List<INonPersistentTrace> result, BasicHubAttachment hubAttachment ->

            checkAttachmentAllowed(hubAttachment)

            StreamableFileMetadata fileMetadata = findFileMetadata(blobMetadataList, hubAttachment.remoteIdStr)
            if (fileMetadata) {
                def source = fileMetadata.getStreamFn().get()
                if (source) {
                    List<MultiPartUploadGroovyHttpRequest.IFormPart> parts = hubAttachmentToMultiPartList(source, hubAttachment)
                    Map outcome = discourseClient.uploadAttachment(parts)
                    BasicNonPersistentTrace trace = toAttachmentTrace(outcome, hubAttachment)
                    result.add(trace)
                }
            }
            result
        }
    }

    private static StreamableFileMetadata findFileMetadata(List<StreamableFileMetadata> fileMetadataList, String attachmentRemoteId) {
        fileMetadataList.find { fileMetaData ->  fileMetaData.blobMetaData().blobId == attachmentRemoteId }
    }

    // TODO - document that only attachments with extension .jpg, .jpeg, .png, .gif, .heic, .heif, .webp, .avif are allowed, and provide a hint how to filter these out.
    private static void checkAttachmentAllowed(BasicHubAttachment basicHubAttachment) {
        if (!basicHubAttachment || !basicHubAttachment.filename) {
            throw new DiscourseClientException("Error - attachment (or its name) is null")
        }

        def extension = basicHubAttachment.filename.toLowerCase().tokenize('.')[-1]
        def allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'heic', 'heif', 'webp', 'avif']
        if (!allowedExtensions.contains(extension)) {
            throw new DiscourseClientException("Error - uploading file to discourse.<br> The file <b>${basicHubAttachment.filename}</b> with extension <b>'${extension}'</b> is not allowed.<br>Allowed extensions are: <b>${allowedExtensions.join(', ')}</b><br>")
        }

    }

//    List<INonPersistentTrace> removeAttachmentsFromTrace(BasicHubIssue entity, List<INonPersistentTrace> traces) {
//        entity.getRemovedAttachments().each {
//            hubAttachment ->
//                String attachmentId = hubAttachment.idStr
//                String deletedAttachmentId = deleteAttachment(attachmentId)
//                INonPersistentTrace foundTrace = findAttachmentOnTraces(traces, deletedAttachmentId)
//                traces.remove(foundTrace)
//        }
//        return traces
//    }
//
//    private static INonPersistentTrace findAttachmentOnTraces(List<INonPersistentTrace> traces, String attachmentId) {
//        return traces.find {
//            String localId = it.getLocalId()
//            if (isAttachmentOnComment(localId)) {
//                localId = extractAttachmentIdPart(localId)
//            }
//            it.getType().name() == "ATTACHMENT" && localId == attachmentId
//        }
//    }
//
//    private String deleteAttachment(String attachmentId) {
//        if (isAttachmentOnComment(attachmentId)) {
//            attachmentId = extractAttachmentIdPart(attachmentId)
//        }
//        restApiClient.deleteAttachment(attachmentId)
//        return attachmentId
//    }

}

