/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.service.impl.FileServiceUtils.isMongoFileIdFormat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.FileMetadata;
import io.harness.file.HarnessFile;
import io.harness.file.dao.GcsHarnessFileMetadataDao;
import io.harness.stream.BoundedInputStream;

import software.wings.DataStorageMode;
import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a wrapper file service implementation which will dual-write files (such as artifact) into Mongo GridFs and
 * Google Cloud Storage. And read from Mongo GridFS if the is the Mongo object id format, from Google Cloud Storage if
 * it's the GCS file id format.
 *
 * @author marklu on 2018-12-04
 */

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class FileServiceImpl implements FileService {
  public static final String FILE_SERVICE_DATA_STORAGE_MODE = "FILE_SERVICE_DATA_STORAGE_MODE";
  private GcsHarnessFileMetadataDao gcsHarnessFileMetadataDao;
  private MongoFileServiceImpl mongoFileService;
  private GoogleCloudFileServiceImpl googleCloudFileService;
  private DataStorageMode dataStorageMode;

  private boolean gcsStorageEnabled;

  @Inject
  public FileServiceImpl(GcsHarnessFileMetadataDao gcsHarnessFileMetadataDao, MongoFileServiceImpl mongoFileService,
      GoogleCloudFileServiceImpl googleCloudFileService,
      @Named(FILE_SERVICE_DATA_STORAGE_MODE) DataStorageMode dataStorageMode) {
    this.gcsHarnessFileMetadataDao = gcsHarnessFileMetadataDao;
    this.mongoFileService = mongoFileService;
    this.googleCloudFileService = googleCloudFileService;
    this.dataStorageMode = dataStorageMode;

    if (dataStorageMode == DataStorageMode.GOOGLE_CLOUD_STORAGE) {
      gcsStorageEnabled = true;
      // Initialize storage and create necessary buckets if GCS storage is enabled.
      googleCloudFileService.initialize();
    }
  }

  @Override
  public String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket) {
    if (gcsStorageEnabled) {
      return googleCloudFileService.saveFile(fileMetadata, in, fileBucket);
    } else {
      return mongoFileService.saveFile(fileMetadata, in, fileBucket);
    }
  }

  @Override
  public boolean updateParentEntityIdAndVersion(Class entityClass, String entityId, Integer version, String fileId,
      Map<String, Object> others, FileBucket fileBucket) {
    boolean isMongoFileId = isMongoFileIdFormat(fileId);
    String mongoFileId;
    String gcsFileId;
    if (isMongoFileId) {
      mongoFileId = fileId;
      gcsFileId = gcsHarnessFileMetadataDao.getGcsFileIdByMongoFileId(mongoFileId);
    } else {
      // Won't reach here if not GCS storage enabled
      gcsFileId = fileId;
      mongoFileId = gcsHarnessFileMetadataDao.getMongoFileIdByGcsFileId(gcsFileId);
    }
    if (gcsStorageEnabled) {
      return googleCloudFileService.updateParentEntityIdAndVersion(
          entityClass, entityId, version, gcsFileId, others, fileBucket);
    } else {
      return mongoFileService.updateParentEntityIdAndVersion(
          entityClass, entityId, version, mongoFileId, others, fileBucket);
    }
  }

  @Override
  public String saveFile(HarnessFile baseFile, InputStream in, FileBucket fileBucket) {
    if (gcsStorageEnabled) {
      return googleCloudFileService.saveFile(baseFile, in, fileBucket);
    } else {
      return mongoFileService.saveFile(baseFile, in, fileBucket);
    }
  }

  @Override
  public void deleteFile(String fileId, FileBucket fileBucket) {
    boolean isMongoFileId = isMongoFileIdFormat(fileId);
    String mongoFileId, gcsFileId;
    if (isMongoFileId) {
      mongoFileId = fileId;
      gcsFileId = gcsHarnessFileMetadataDao.getGcsFileIdByMongoFileId(mongoFileId);
    } else {
      // Won't reach here if GCS storage is not enabled
      gcsFileId = fileId;
      mongoFileId = gcsHarnessFileMetadataDao.getMongoFileIdByGcsFileId(gcsFileId);
    }
    if (isNotEmpty(gcsFileId)) {
      googleCloudFileService.deleteFile(gcsFileId, fileBucket);
    }
    if (isNotEmpty(mongoFileId)) {
      mongoFileService.deleteFile(mongoFileId, fileBucket);
      gcsHarnessFileMetadataDao.deleteGcsFileMetadataByMongoFileId(mongoFileId);
    }
  }

  @Override
  public File download(String fileId, File file, FileBucket fileBucket) {
    boolean isMongoFileId = isMongoFileIdFormat(fileId);
    if (isMongoFileId) {
      return mongoFileService.download(fileId, file, fileBucket);
    } else {
      String gcsFileId = fileId;
      if (gcsStorageEnabled) {
        return googleCloudFileService.download(gcsFileId, file, fileBucket);
      } else {
        String mongoFileId = gcsHarnessFileMetadataDao.getMongoFileIdByGcsFileId(gcsFileId);
        return mongoFileService.download(mongoFileId, file, fileBucket);
      }
    }
  }

  @Override
  public void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket) {
    boolean isMongoFileId = isMongoFileIdFormat(fileId);
    if (isMongoFileId) {
      mongoFileService.downloadToStream(fileId, op, fileBucket);
    } else {
      String gcsFileId = fileId;
      if (gcsStorageEnabled) {
        googleCloudFileService.downloadToStream(gcsFileId, op, fileBucket);
      } else {
        String mongoFileId = gcsHarnessFileMetadataDao.getMongoFileIdByGcsFileId(gcsFileId);
        mongoFileService.downloadToStream(mongoFileId, op, fileBucket);
      }
    }
  }

  @Override
  public InputStream openDownloadStream(String fileId, FileBucket fileBucket) {
    boolean isMongoFileId = isMongoFileIdFormat(fileId);
    if (isMongoFileId) {
      return mongoFileService.openDownloadStream(fileId, fileBucket);
    } else {
      String gcsFileId = fileId;
      if (gcsStorageEnabled) {
        return googleCloudFileService.openDownloadStream(fileId, fileBucket);
      } else {
        String mongoFileId = gcsHarnessFileMetadataDao.getMongoFileIdByGcsFileId(gcsFileId);
        return mongoFileService.openDownloadStream(mongoFileId, fileBucket);
      }
    }
  }

  @Override
  public FileMetadata getFileMetadata(String fileId, FileBucket fileBucket) {
    boolean isMongoFileId = isMongoFileIdFormat(fileId);
    if (isMongoFileId) {
      return mongoFileService.getFileMetadata(fileId, fileBucket);
    } else {
      String gcsFileId = fileId;
      if (gcsStorageEnabled) {
        return googleCloudFileService.getFileMetadata(fileId, fileBucket);
      } else {
        String mongoFileId = gcsHarnessFileMetadataDao.getMongoFileIdByGcsFileId(gcsFileId);
        return mongoFileService.getFileMetadata(mongoFileId, fileBucket);
      }
    }
  }

  @Override
  public List<String> getAllFileIds(String entityId, FileBucket fileBucket) {
    List<String> allFileIds = mongoFileService.getAllFileIds(entityId, fileBucket);
    if (!gcsStorageEnabled || isNotEmpty(allFileIds)) {
      return allFileIds;
    } else {
      return googleCloudFileService.getAllFileIds(entityId, fileBucket);
    }
  }

  @Override
  public String getLatestFileId(String entityId, FileBucket fileBucket) {
    String latestFileId = mongoFileService.getLatestFileId(entityId, fileBucket);
    if (!gcsStorageEnabled || isNotEmpty(latestFileId)) {
      return latestFileId;
    } else {
      return googleCloudFileService.getLatestFileId(entityId, fileBucket);
    }
  }

  @Override
  public String getLatestFileIdByQualifier(String entityId, FileBucket fileBucket, String qualifier) {
    String latestFileId = mongoFileService.getLatestFileIdByQualifier(entityId, fileBucket, qualifier);
    if (!gcsStorageEnabled || isNotEmpty(latestFileId)) {
      return latestFileId;
    } else {
      return googleCloudFileService.getLatestFileIdByQualifier(entityId, fileBucket, qualifier);
    }
  }

  @Override
  public String getFileIdByVersion(String entityId, int version, FileBucket fileBucket) {
    String fileId = mongoFileService.getFileIdByVersion(entityId, version, fileBucket);
    if (!gcsStorageEnabled || isNotEmpty(fileId)) {
      return fileId;
    } else {
      return googleCloudFileService.getFileIdByVersion(entityId, version, fileBucket);
    }
  }

  @Override
  public String uploadFromStream(
      String filename, BoundedInputStream in, FileBucket fileBucket, Map<String, Object> metaData) {
    // This method is called by auditing code path and typically on AUDIT file bucket.
    String mongoFileId = mongoFileService.uploadFromStream(filename, in, fileBucket, metaData);
    if (gcsStorageEnabled) {
      InputStream inputStreamFromMongo = mongoFileService.openDownloadStream(mongoFileId, fileBucket);
      googleCloudFileService.uploadFromStream(
          filename, new BoundedInputStream(inputStreamFromMongo), fileBucket, metaData);
    }

    return mongoFileId;
  }

  @Override
  public void deleteAllFilesForEntity(String entityId, FileBucket fileBucket) {
    mongoFileService.deleteAllFilesForEntity(entityId, fileBucket);
    if (gcsStorageEnabled) {
      googleCloudFileService.deleteAllFilesForEntity(entityId, fileBucket);
    }
  }
}
