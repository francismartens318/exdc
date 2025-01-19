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

package customconnectornode.discourse


import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.exalate.api.domain.hubobject.IHubIssuePayload
import com.exalate.api.domain.hubobject.IHubIssueReplica
import com.exalate.basic.domain.blob.BlobMetadata
import com.exalate.basic.domain.hubobject.v1.BasicHubIssue
import com.exalate.domain.node.storeblob.FileSystemStoredBlobMetaData
import com.exalate.domain.node.storeblob.StoredBlobMetaData
import com.exalate.replication.services.hubobject.ReplicaHelper
import customconnectornode.domain.StreamableFileMetadata
import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory

import java.nio.file.Paths
import java.util.function.Supplier

class TestUtils {

    static void setupSpec() {
        // Set root logger to ERROR level
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.setLevel(Level.ERROR)

        // Enable DEBUG level for customconnectornode package
        Logger appLogger = (Logger) LoggerFactory.getLogger("customconnectornode")
        appLogger.setLevel(Level.DEBUG)

        Dotenv dotenv = Dotenv.configure()
                .directory("./ccnode")
                .load()

        System.setProperty("TRACKER_URL", dotenv.get("TRACKER_URL"))
        System.setProperty("TRACKER_API_KEY", dotenv.get("TRACKER_API_KEY"))
        System.setProperty("TRACKER_USER", dotenv.get("TRACKER_USER"))
        System.setProperty("TRACKER_PASSWORD", dotenv.get("TRACKER_PASSWORD"))
    }

    static IHubIssueReplica getReplica(String payload) {
        IHubIssuePayload ihip =  new ReplicaHelper().toHubIssuePayload(payload)
        return ihip.getHubIssueReplica()
    }

//    static List<StreamableFileMetadata> getBlobMetadataList(String payload) {
//        StreamableFileMetadata blobMetadata = new StreamableFileMetadata(
//                new BlobMetadata(//TODO collect information),
//                new FileSystemStoredBlobMetaData('/opt/customconnectornode/data/1_sync_2008'),
//                new Supplier<Source<ByteString, ?>>() {
//                    @Override
//                    Source<ByteString, ?> get() {
//                        FileIO.fromPath(Paths.get('/opt/customconnectornode/data/1_sync_2008'), 1024)
//                    }
//                }
//        )
//
//        return [ blobMetadata ]
//    }

}