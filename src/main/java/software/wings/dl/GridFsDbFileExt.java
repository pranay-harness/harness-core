package software.wings.dl;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.ArrayUtils;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoGridFSException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import software.wings.exception.WingsException;

/**
 * Created by anubhaw on 3/3/16.
 */
public class GridFsDbFileExt {
  private MongoDatabase mongodb;
  private GridFSBucket gridFsBucket;
  private String fileCollectionName;
  private String chunkCollectionName;
  private int chunkSize;

  private static final Logger logger = LoggerFactory.getLogger(GridFsDbFileExt.class);

  public GridFsDbFileExt(MongoDatabase mongodb, String bucketName, int chunkSize) {
    this.mongodb = mongodb;
    this.chunkSize = chunkSize;
    gridFsBucket = GridFSBuckets.create(mongodb, bucketName);
    fileCollectionName = bucketName + ".files";
    chunkCollectionName = bucketName + ".chunks";
  }

  public void appendToFile(String fileName, String content) {
    GridFSFile file = gridFsBucket.find(eq("filename", fileName)).first();
    if (null == file) { // Write first chunk
      logger.info(
          String.format("No file found with name [%s]. Creating new file and writing initial chunks", fileName));
      put(fileName, content);
    } else {
      long fileLength = file.getLength();
      int chunkSize = file.getChunkSize();
      int existingChunksCount = (int) Math.ceil(fileLength / (double) chunkSize);

      int bytesFreeInLastChunk = fileLength % chunkSize == 0 ? 0 : (int) (chunkSize - fileLength % chunkSize);

      if (bytesFreeInLastChunk > 0) {
        appendToExistingChunk(file, existingChunksCount, content.substring(0, bytesFreeInLastChunk));
        content = content.substring(bytesFreeInLastChunk);
      }

      int newChunksRequired = (int) Math.ceil(content.length() / (double) chunkSize);

      int offset = 0;
      for (int i = 0; i < newChunksRequired; i++) {
        int subStrEndIdx = (offset + chunkSize > content.length() ? content.length() : offset + chunkSize);
        saveNewChunk(file, existingChunksCount + i, content.substring(offset, subStrEndIdx));
        offset += chunkSize;
      }
    }
  }

  public void put(String fileName, String content) {
    InputStream streamToUploadFrom;
    try {
      streamToUploadFrom = new ByteArrayInputStream(content.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WingsException("String to stream conversion failed", e.getCause());
    }
    gridFsBucket.uploadFromStream(fileName, streamToUploadFrom, new GridFSUploadOptions().chunkSizeBytes(chunkSize));
    logger.info(String.format("content [%s] for fileName [%s] saved in gridfs", content, fileName));
  }

  private void appendToExistingChunk(GridFSFile file, int existingChunksCount, String substring) {
    Document doc =
        mongodb.getCollection(chunkCollectionName)
            .find(and(eq("files_id", ((BsonObjectId) file.getId()).getValue()), eq("n", existingChunksCount - 1)))
            .first();
    byte[] data = ((Binary) doc.get("data")).getData();
    byte[] newData = substring.getBytes();
    byte[] combined = ArrayUtils.addAll(data, newData);
    Binary binData = new Binary(combined);

    mongodb.getCollection(chunkCollectionName)
        .updateOne(eq("_id", doc.get("_id")), new Document("$set", new Document("data", binData)));
    updateFileMetaData(file, substring.length());
  }

  private void saveNewChunk(GridFSFile file, int chunkIdx, String content) {
    Document doc = new Document()
                       .append("files_id", file.getId())
                       .append("n", chunkIdx)
                       .append("data", new Binary(content.getBytes()));
    mongodb.getCollection(chunkCollectionName).insertOne(doc);
    updateFileMetaData(file, content.length());
  }

  private void updateFileMetaData(GridFSFile file, int length) {
    mongodb.getCollection(fileCollectionName)
        .updateOne(
            eq("_id", ((BsonObjectId) file.getId()).getValue()), new Document("$inc", new Document("length", length)));
  }

  public GridFSFile get(String fileName) {
    return gridFsBucket.find(eq("filename", fileName)).first();
  }

  public void downloadToStream(String fileName, FileOutputStream fileOutputStream) {
    try {
      gridFsBucket.downloadToStreamByName(fileName, fileOutputStream);
    } catch (MongoGridFSException e) {
      if (!e.getMessage().startsWith("Chunk size data length is not the expected size")) { // TODO: Fixit
        logger.error(e.getMessage());
        throw e;
      }
    }
  }
}
