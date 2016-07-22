package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.ErrorCodes.FILE_INTEGRITY_CHECK_FAILED;

import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;
import software.wings.dl.FileBucketHelper;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class FileServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class FileServiceImpl implements FileService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileBucketHelper fileBucketHelper;

  /**
   * {@inheritDoc}
   */
  @Override
  public File download(String fileId, File file, FileBucket fileBucket) {
    try {
      FileOutputStream streamToDownload = new FileOutputStream(file);
      fileBucketHelper.getOrCreateFileBucket(fileBucket).downloadToStream(new ObjectId(fileId), streamToDownload);
      streamToDownload.close();
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
    fileBucketHelper.getOrCreateFileBucket(fileBucket).downloadToStream(new ObjectId(fileId), outputStream);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GridFSFile getGridFsFile(String fileId, FileBucket fileBucket) {
    GridFSFindIterable filemetaData =
        fileBucketHelper.getOrCreateFileBucket(fileBucket).find(Filters.eq("_id", new ObjectId(fileId)));
    return filemetaData.first();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String uploadFromStream(String filename, InputStream in, FileBucket fileBucket, GridFSUploadOptions options) {
    ObjectId fileId = fileBucketHelper.getOrCreateFileBucket(fileBucket).uploadFromStream(filename, in, options);
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
    List<DBObject> dbObjects = wingsPersistence.getCollection("configs.files").find(query).toArray();
    return dbObjects;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket) {
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

    ObjectId fileId =
        fileBucketHelper.getOrCreateFileBucket(fileBucket).uploadFromStream(fileMetadata.getFileName(), in, options);
    return fileId.toHexString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String saveFile(BaseFile baseFile, InputStream inputStream, FileBucket bucket) {
    GridFSUploadOptions gridFSOptions = new GridFSUploadOptions().chunkSizeBytes(bucket.getChunkSize());
    String fileId = fileBucketHelper.getOrCreateFileBucket(bucket)
                        .uploadFromStream(baseFile.getFileName(), inputStream, gridFSOptions)
                        .toHexString();
    GridFSFile gridFsFile = getGridFsFile(fileId, bucket);
    verifyFileIntegrity(baseFile, gridFsFile);
    baseFile.setChecksum(gridFsFile.getMD5());
    baseFile.setFileUuid(fileId);
    baseFile.setSize(gridFsFile.getLength());
    return fileId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteFile(String fileId, FileBucket fileBucket) {
    fileBucketHelper.getOrCreateFileBucket(fileBucket).delete(new ObjectId(fileId));
  }

  private void verifyFileIntegrity(BaseFile baseFile, GridFSFile gridFsFile) {
    if (!isNullOrEmpty(baseFile.getChecksum()) && !gridFsFile.getMD5().equals(baseFile.getChecksum())) {
      throw new WingsException(FILE_INTEGRITY_CHECK_FAILED);
    }
  }
}
