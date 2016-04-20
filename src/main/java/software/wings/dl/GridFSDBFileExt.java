package software.wings.dl;

import static com.mongodb.client.model.Filters.eq;
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
import software.wings.exception.WingsException;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by anubhaw on 3/3/16.
 */
@Singleton
public class GridFSDBFileExt {
  @Inject private WingsPersistence wingsPersistence;
  private String fileCollectionName = LOGS.getName() + ".files";
  private String chunkCollectionName = LOGS.getName() + ".chunks";
  private int chunkSize = (int) LOGS.getChunkSize();

  private Logger LOGGER = LoggerFactory.getLogger(getClass());

  public void appendToFile(String fileName, String content) {
    GridFSFile file = LOGS.getGridFSBucket().find(eq("filename", fileName)).first();
    if (null == file) { // Write first chunk
      LOGGER.info(
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

  private void updateFileMetaData(GridFSFile file, int length) {
    BasicDBObject query = new BasicDBObject(ID_KEY, ((BsonObjectId) file.getId()).getValue());
    BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject("length", length));
    wingsPersistence.getCollection(fileCollectionName).update(query, update);
  }

  private void saveNewChunk(GridFSFile file, int chunkIdx, String content) {
    BasicDBObject doc = new BasicDBObject()
                            .append("files_id", file.getId())
                            .append("n", chunkIdx)
                            .append("data", new Binary(content.getBytes()));
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
    byte[] newData = substring.getBytes();
    byte[] combined = ArrayUtils.addAll(data, newData);
    Binary binData = new Binary(combined);

    wingsPersistence.getCollection(chunkCollectionName)
        .update(
            new BasicDBObject("_id", doc.get("_id")), new BasicDBObject("$set", new BasicDBObject("data", binData)));
    updateFileMetaData(file, substring.length());
  }

  public void put(String fileName, String content) {
    InputStream streamToUploadFrom;
    try {
      streamToUploadFrom = new ByteArrayInputStream(content.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WingsException("String to stream conversion failed", e.getCause());
    }
    LOGS.getGridFSBucket().uploadFromStream(
        fileName, streamToUploadFrom, new GridFSUploadOptions().chunkSizeBytes(chunkSize));
    LOGGER.info(String.format("content [%s] for fileName [%s] saved in gridfs", content, fileName));
  }

  public GridFSFile get(String fileName) {
    return LOGS.getGridFSBucket().find(eq("filename", fileName)).first();
  }

  public void downloadToStream(String fileName, FileOutputStream fileOutputStream) {
    try {
      LOGS.getGridFSBucket().downloadToStreamByName(fileName, fileOutputStream);
    } catch (MongoGridFSException e) {
      if (!e.getMessage().startsWith("Chunk size data length is not the expected size")) { // TODO: Fixit
        LOGGER.error(e.getMessage());
        throw e;
      }
    }
  }
}
