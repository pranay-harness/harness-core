package software.wings.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.FILE_INTEGRITY_CHECK_FAILED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import io.harness.exception.WingsException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class FileServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class FileServiceImpl implements FileService {
  private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;

  /**
   * {@inheritDoc}
   */
  @Override
  public File download(String fileId, File file, FileBucket fileBucket) {
    try (FileOutputStream streamToDownload = new FileOutputStream(file)) {
      wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
          .downloadToStream(new ObjectId(fileId), streamToDownload);
      return file;
    } catch (IOException ex) {
      logger.error("Error in download", ex);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void downloadToStream(String fileId, OutputStream outputStream, FileBucket fileBucket) {
    wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
        .downloadToStream(new ObjectId(fileId), outputStream);
  }

  @Override
  public InputStream openDownloadStream(String fileId, FileBucket fileBucket) {
    return wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
        .openDownloadStream(new ObjectId(fileId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GridFSFile getGridFsFile(String fileId, FileBucket fileBucket) {
    GridFSFindIterable filemetaData = wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
                                          .find(Filters.eq("_id", new ObjectId(fileId)));
    return filemetaData.first();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getAllFileIds(String entityId, FileBucket fileBucket) {
    GridFSFindIterable filemetaData = wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
                                          .find(Filters.eq("metadata.entityId", entityId));
    return stream(filemetaData.sort(descending("uploadDate")).spliterator(), false)
        .map(gridFSFile -> gridFSFile.getObjectId().toHexString())
        .collect(toList());
  }

  @Override
  public String getLatestFileId(String entityId, FileBucket fileBucket) {
    final GridFSFile first = wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
                                 .find(Filters.and(Filters.eq("metadata.entityId", entityId)))
                                 .sort(orderBy(descending("uploadDate")))
                                 .limit(1)
                                 .first();
    if (first == null) {
      return null;
    }
    return first.getId().asObjectId().getValue().toHexString();
  }

  @Override
  public String getFileIdByVersion(String entityId, int version, FileBucket fileBucket) {
    final GridFSFile first =
        wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
            .find(Filters.and(Filters.eq("metadata.entityId", entityId), Filters.eq("metadata.version", version)))
            .limit(1)
            .first();
    if (first == null) {
      return null;
    }
    return first.getId().asObjectId().getValue().toHexString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String uploadFromStream(
      String filename, BoundedInputStream in, FileBucket bucket, Map<String, Object> metaData) {
    GridFSUploadOptions gridFSOptions =
        new GridFSUploadOptions().chunkSizeBytes(bucket.getChunkSize()).metadata(new Document(metaData));
    ObjectId fileId = wingsPersistence.getOrCreateGridFSBucket(bucket.representationName())
                          .uploadFromStream(filename, in, gridFSOptions);
    return fileId.toHexString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DBObject> getFilesMetaData(List<String> fileIDs, FileBucket fileBucket) {
    List<ObjectId> objIDs = new ArrayList<>();
    for (String id : fileIDs) {
      objIDs.add(new ObjectId(id));
    }
    BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", objIDs));
    return wingsPersistence.getCollection(fileBucket.representationName() + ".files").find(query).toArray();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket) {
    logger.info("Saving file {} ", fileMetadata);
    Document metadata = new Document();

    if (isNotBlank(fileMetadata.getChecksum()) && fileMetadata.getChecksumType() != null) {
      metadata.append("checksum", fileMetadata.getChecksum());
      metadata.append("checksumType", fileMetadata.getChecksumType().name());
    }

    if (isNotBlank(fileMetadata.getMimeType())) {
      metadata.append("mimeType", fileMetadata.getMimeType());
    }

    if (isNotBlank(fileMetadata.getRelativePath())) {
      metadata.append("relativePath", fileMetadata.getRelativePath());
    }

    GridFSUploadOptions options =
        new GridFSUploadOptions().chunkSizeBytes(fileBucket.getChunkSize()).metadata(metadata);

    ObjectId fileId = wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName())
                          .uploadFromStream(fileMetadata.getFileName(), in, options);
    logger.info("Saved file {}. Returning fileId {}", fileMetadata.getFileName(), fileId.toHexString());
    return fileId.toHexString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean updateParentEntityIdAndVersion(Class entityClass, String entityId, Integer version, String fileId,
      Map<String, Object> others, FileBucket fileBucket) {
    DBCollection collection =
        wingsPersistence.getDatastore().getDB().getCollection(fileBucket.representationName() + ".files");

    // TODO: creating this index here makes no sense
    collection.createIndex(
        new BasicDBObject(of("metadata.entityId", 1, "metadata.version", 1)), new BasicDBObject("background", true));

    Map<String, Object> updateMap = new HashMap<>();
    if (isNotEmpty(others)) {
      updateMap.putAll(others);
    }

    if (entityClass != null) {
      updateMap.put("metadata.class", entityClass.getCanonicalName());
    }
    if (entityId != null) {
      updateMap.put("metadata.entityId", entityId);
    }
    if (version != null) {
      updateMap.put("metadata.version", version);
    }

    return collection
               .update(new BasicDBObject("_id", new ObjectId(fileId)),
                   new BasicDBObject("$set", new BasicDBObject(updateMap)))
               .getN()
        > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String saveFile(BaseFile baseFile, InputStream inputStream, FileBucket bucket) {
    GridFSUploadOptions gridFSOptions = new GridFSUploadOptions().chunkSizeBytes(bucket.getChunkSize());
    String fileId = wingsPersistence.getOrCreateGridFSBucket(bucket.representationName())
                        .uploadFromStream(baseFile.getFileName(), inputStream, gridFSOptions)
                        .toHexString();
    GridFSFile gridFsFile = getGridFsFile(fileId, bucket);
    verifyFileIntegrity(baseFile, gridFsFile);
    baseFile.setChecksum(gridFsFile.getMD5());
    baseFile.setFileUuid(fileId);
    return fileId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteFile(String fileId, FileBucket fileBucket) {
    logger.info("Deleting file {}", fileId);
    wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName()).delete(new ObjectId(fileId));
    logger.info("Deleting file {} success", fileId);
  }

  @Override
  public void deleteAllFilesForEntity(String entityId, FileBucket fileBucket) {
    final GridFSBucket bucket = wingsPersistence.getOrCreateGridFSBucket(fileBucket.representationName());
    getAllFileIds(entityId, fileBucket).forEach(fileUuid -> bucket.delete(new ObjectId(fileUuid)));
  }

  private void verifyFileIntegrity(BaseFile baseFile, GridFSFile gridFsFile) {
    if (isNotBlank(baseFile.getChecksum()) && !gridFsFile.getMD5().equals(baseFile.getChecksum())) {
      throw new WingsException(FILE_INTEGRITY_CHECK_FAILED);
    }
  }
}
