package software.wings.service.impl;

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
import software.wings.app.WingsBootstrap;
import software.wings.beans.FileMetadata;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Singleton
public class FileServiceImpl implements FileService {
  private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

  @Override
  public File download(String fileId, File file, FileBucket fileBucket) {
    try {
      FileOutputStream streamToDownload = new FileOutputStream(file);
      fileBucket.getGridFSBucket().downloadToStream(new ObjectId(fileId), streamToDownload);
      streamToDownload.close();
      return file;
    } catch (IOException e) {
      logger.error("Error in download", e);
      return null;
    }
  }

  @Override
  public void downloadToStream(String fileId, OutputStream outputStream, FileBucket fileBucket) {
    fileBucket.getGridFSBucket().downloadToStream(new ObjectId(fileId), outputStream);
  }

  @Override
  public GridFSFile getGridFsFile(String fileId, FileBucket fileBucket) {
    GridFSFindIterable filemetaData = fileBucket.getGridFSBucket().find(Filters.eq("_id", new ObjectId(fileId)));
    return filemetaData.first();
  }

  @Override
  public String uploadFromStream(String filename, InputStream in, FileBucket fileBucket, GridFSUploadOptions options) {
    ObjectId fileId = fileBucket.getGridFSBucket().uploadFromStream(filename, in, options);
    return fileId.toHexString();
  }

  @Override
  public List<DBObject> getFilesMetaData(List<String> fileIDs, FileBucket fileBucket) {
    List<ObjectId> objIDs = new ArrayList<>();
    for (String id : fileIDs) {
      objIDs.add(new ObjectId(id));
    }
    BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", objIDs));
    List<DBObject> dbObjects =
        WingsBootstrap.lookup(WingsPersistence.class).getCollection("configs.files").find(query).toArray();
    return dbObjects;
  }

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

    GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(16 * 1024 * 1024).metadata(metadata);

    ObjectId fileId = fileBucket.getGridFSBucket().uploadFromStream(fileMetadata.getFileName(), in, options);
    return fileId.toHexString();
  }
}
