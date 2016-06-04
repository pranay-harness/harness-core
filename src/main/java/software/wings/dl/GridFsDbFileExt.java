package software.wings.dl;

import static com.mongodb.client.model.Filters.eq;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.intfc.FileService.FileBucket.LOGS;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoGridFSException;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.apache.commons.lang.ArrayUtils;
import org.bson.BsonObjectId;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/3/16.
 */

@Singleton
public class GridFsDbFileExt {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileBucketHelper fileBucketHelper;
  private String fileCollectionName = LOGS.getName() + ".files";
  private String chunkCollectionName = LOGS.getName() + ".chunks";
  private int chunkSize = (int) LOGS.getChunkSize();

  /**
   * Append to file.
   *
   * @param fileName the file name
   * @param content  the content
   */
  public void appendToFile(String fileName, String content) {
    GridFSFile file = fileBucketHelper.getOrCreateFileBucket(LOGS).find(eq("filename", fileName)).first();
    if (null == file) { // Write first chunk
      logger.info("No file found with name {}. Create new file and write initial chunks", fileName);
      put(fileName, content);
    } else {
      long fileLength = file.getLength();
      int chunkSize = file.getChunkSize();
      int existingChunksCount = (int) Math.ceil(fileLength / (double) chunkSize);

      int bytesFreeInLastChunk = fileLength % chunkSize == 0 ? 0 : (int) (chunkSize - fileLength % chunkSize);
      int subStrEndIdx;

      if (bytesFreeInLastChunk > 0) {
        subStrEndIdx = bytesFreeInLastChunk > content.length() ? content.length() : bytesFreeInLastChunk;
        appendToExistingChunk(file, existingChunksCount, content.substring(0, subStrEndIdx));
        content = content.substring(subStrEndIdx);
      }

      int newChunksRequired = (int) Math.ceil(content.length() / (double) chunkSize);

      int offset = 0;
      for (int i = 0; i < newChunksRequired; i++) {
        subStrEndIdx = (offset + chunkSize > content.length() ? content.length() : offset + chunkSize);
        saveNewChunk(file, existingChunksCount + i, content.substring(offset, subStrEndIdx));
        offset += chunkSize;
      }
    }
  }

  private void updateFileMetaData(GridFSFile file, int length) {
    BasicDBObject query = new BasicDBObject(ID_KEY, ((BsonObjectId) file.getId()).getValue());
    BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject("length", length));
    wingsPersistence.getCollection(fileCollectionName).update(query, update);
  }

  private void saveNewChunk(GridFSFile file, int chunkIdx, String content) {
    BasicDBObject doc = new BasicDBObject()
                            .append("files_id", file.getId())
                            .append("n", chunkIdx)
                            .append("data", new Binary(content.getBytes(UTF_8)));
    wingsPersistence.getCollection(chunkCollectionName).insert(doc);
    updateFileMetaData(file, content.length());
  }

  private void appendToExistingChunk(GridFSFile file, int existingChunksCount, String substring) {
    BasicDBObject clause1 = new BasicDBObject("files_id", file.getId());
    BasicDBObject clause2 = new BasicDBObject("n", existingChunksCount - 1);
    BasicDBList clauses = new BasicDBList();
    clauses.add(clause1);
    clauses.add(clause2);
    BasicDBObject query = new BasicDBObject("and", clauses);

    DBObject doc = wingsPersistence.getCollection(chunkCollectionName).findOne(query);
    byte[] data = ((Binary) doc.get("data")).getData();
    byte[] newData = substring.getBytes(UTF_8);
    byte[] combined = ArrayUtils.addAll(data, newData);
    Binary binData = new Binary(combined);

    wingsPersistence.getCollection(chunkCollectionName)
        .update(
            new BasicDBObject("_id", doc.get("_id")), new BasicDBObject("$set", new BasicDBObject("data", binData)));
    updateFileMetaData(file, substring.length());
  }

  /**
   * Put.
   *
   * @param fileName the file name
   * @param content  the content
   */
  public void put(String fileName, String content) {
    InputStream streamToUploadFrom;
    streamToUploadFrom = new ByteArrayInputStream(content.getBytes(UTF_8));
    fileBucketHelper.getOrCreateFileBucket(LOGS).uploadFromStream(
        fileName, streamToUploadFrom, new GridFSUploadOptions().chunkSizeBytes(chunkSize));
    logger.info(String.format("content [%s] for fileName [%s] saved in gridfs", content, fileName));
  }

  /**
   * Gets the.
   *
   * @param fileName the file name
   * @return the grid fs file
   */
  public GridFSFile get(String fileName) {
    return fileBucketHelper.getOrCreateFileBucket(LOGS).find(eq("filename", fileName)).first();
  }

  /**
   * Download to stream.
   *
   * @param fileName         the file name
   * @param fileOutputStream the file output stream
   */
  public void downloadToStream(String fileName, FileOutputStream fileOutputStream) {
    try {
      fileBucketHelper.getOrCreateFileBucket(LOGS).downloadToStreamByName(fileName, fileOutputStream);
    } catch (MongoGridFSException ex) {
      if (!ex.getMessage().startsWith("Chunk size data length is not the expected size")) { // TODO: Fixit
        logger.error(ex.getMessage());
        throw ex;
      }
    }
  }
}
