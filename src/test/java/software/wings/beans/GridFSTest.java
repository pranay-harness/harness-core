package software.wings.beans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

public class GridFSTest {
  @Test
  public void testGridFS() throws IOException {
    MongoClient mongoClient = new MongoClient("localhost", 12345);
    GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.getDatabase("test"), "artifacts");

    // Get the input stream
    InputStream streamToUploadFrom = new FileInputStream(new File("/Users/rishi/Desktop/FullSizeRender.png"));

    // Create some custom options
    GridFSUploadOptions options =
        new GridFSUploadOptions().chunkSizeBytes(16 * 1024).metadata(new Document("type", "presentation"));

    ObjectId fileId = gridFSBucket.uploadFromStream("mongodb-tutorial", streamToUploadFrom, options);
    System.out.println(fileId.toHexString());
    streamToUploadFrom.close();
  }

  @Test
  public void getData() throws IOException {
    MongoClient mongoClient = new MongoClient("localhost", 12345);
    GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.getDatabase("test"), "artifacts");

    FileOutputStream streamToDownloadTo = new FileOutputStream("/Users/rishi/abc-" + System.currentTimeMillis());

    gridFSBucket.downloadToStream(new ObjectId("5677ddb1b5f9222e9569ca9d"), streamToDownloadTo);
    streamToDownloadTo.close();
    System.out.println(streamToDownloadTo.toString());
  }
}
