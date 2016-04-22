package software.wings.service.impl;

import com.google.inject.Singleton;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FileMetadata;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;

@Singleton
public class FileServiceImpl implements FileService {
  private static final String FILE_BUCKET = "lob";
  private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  private GridFSBucket gridFSBucket;

  public FileServiceImpl() {
    this.gridFSBucket = wingsPersistence.createGridFSBucket(FILE_BUCKET);
  }

  @Override
  public String saveFile(FileMetadata fileMetadata, InputStream in) {
    Document metadata = new Document();
    if (StringUtils.isNotBlank(fileMetadata.getFileDataType())) {
      metadata.append("fileDataType", fileMetadata.getFileDataType());
    }
    if (StringUtils.isNotBlank(fileMetadata.getFileRefId())) {
      metadata.append("fileDataRefId", fileMetadata.getFileRefId());
    }
    if (StringUtils.isNotBlank(fileMetadata.getChecksum()) && fileMetadata.getChecksumType() != null) {
      metadata.append("checksum", fileMetadata.getChecksum());
      metadata.append("checksumType", fileMetadata.getChecksumType());
    }

    GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(16 * 1024 * 1024).metadata(metadata);

    ObjectId fileId = gridFSBucket.uploadFromStream(fileMetadata.getFileName(), in, options);
    return fileId.toHexString();
  }

  @Override
  public File download(String fileId, File file) {
    try {
      FileOutputStream streamToDownload = new FileOutputStream(file);
      gridFSBucket.downloadToStream(new ObjectId(fileId), streamToDownload);
      streamToDownload.close();
      return file;
    } catch (IOException e) {
      logger.error("Error in download", e);
      return null;
    }
  }

  @Override
  public void downloadToStream(String fileId, OutputStream outputStream) {
    gridFSBucket.downloadToStream(new ObjectId(fileId), outputStream);
  }

  @Override
  public GridFSFile getGridFsFile(String fileId) {
    GridFSFindIterable filemetaData = gridFSBucket.find(Filters.eq("_id", new ObjectId(fileId)));
    return filemetaData.first();
  }
}
