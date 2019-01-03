package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.READ_FILE_FROM_GCP_STORAGE_FAILED;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.service.impl.FileServiceUtils.FILE_PATH_SEPARATOR;
import static software.wings.service.impl.FileServiceUtils.GCS_ID_PREFIX;
import static software.wings.service.impl.FileServiceUtils.GoogleCloudFileIdComponent;
import static software.wings.service.impl.FileServiceUtils.parseGoogleCloudFileId;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BucketListOption;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;
import software.wings.beans.GcsFileMetadata;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.utils.BoundedInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use Google Cloud Storage as file/blob storage.
 *
 * @author marklu on 11/16/18
 */
@Singleton
public class GoogleCloudFileServiceImpl implements FileService {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudFileServiceImpl.class);

  private static final String METADATA_FILE_NAME = "fileName";
  private static final String GOOGLE_APPLICATION_CREDENTIALS_PATH = "GOOGLE_APPLICATION_CREDENTIALS";

  private WingsPersistence wingsPersistence;
  private MainConfiguration configuration;
  private volatile Storage storage;

  @Inject
  public GoogleCloudFileServiceImpl(WingsPersistence wingsPersistence, MainConfiguration configuration) {
    this.wingsPersistence = wingsPersistence;
    this.configuration = configuration;
  }

  @Override
  public String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket) {
    String accountId = fileMetadata.getAccountId();
    String gcsFileName = accountId + FILE_PATH_SEPARATOR + fileMetadata.getFileUuid();
    BlobId blobId = BlobId.of(getBucketName(fileBucket), gcsFileName);

    Map<String, String> metadata = new HashMap<>();
    metadata.put(METADATA_FILE_NAME, fileMetadata.getFileName());

    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                            .setContentType(fileMetadata.getMimeType())
                            .setMd5(fileMetadata.getChecksum())
                            .setMetadata(metadata)
                            .build();
    try {
      getStorage().create(blobInfo, IOUtils.toByteArray(in));
      return generateFileId(blobId);
    } catch (IOException e) {
      throw new WingsException(ErrorCode.SAVE_FILE_INTO_GCP_STORAGE_FAILED, e);
    }
  }

  @Override
  public String saveFile(BaseFile baseFile, InputStream uploadedInputStream, FileBucket fileBucket) {
    String accountId = baseFile.getAccountId();
    String gcsFileName = accountId + FILE_PATH_SEPARATOR + baseFile.getFileUuid();
    BlobId blobId = BlobId.of(getBucketName(fileBucket), gcsFileName);

    Map<String, String> metadata = new HashMap<>();
    metadata.put(METADATA_FILE_NAME, baseFile.getFileName());

    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                            .setContentType(baseFile.getMimeType())
                            .setMd5(baseFile.getChecksum())
                            .setMetadata(metadata)
                            .build();
    final String fileId;
    try {
      getStorage().create(blobInfo, IOUtils.toByteArray(uploadedInputStream));
      fileId = generateFileId(blobId);
    } catch (IOException e) {
      throw new WingsException(ErrorCode.SAVE_FILE_INTO_GCP_STORAGE_FAILED);
    }

    return fileId;
  }

  @Override
  public void deleteFile(String fileId, FileBucket fileBucket) {
    logger.info("Deleting file {}", fileId);
    // delete gcs file metadata first.
    deleteGcsFileMetadataByGcsFileId(fileId);

    BlobId blobId = getBlobIdFromFileId(fileId, fileBucket);
    getStorage().delete(blobId);
    logger.info("Deleted file {}", fileId);
  }

  @Override
  public File download(String fileId, File file, FileBucket fileBucket) {
    BlobId blobId = getBlobIdFromFileId(fileId, fileBucket);
    Blob blob = getStorage().get(blobId);
    if (blob == null) {
      throw new WingsException(
          " File with id " + fileId + " or blob id " + blobId + " can't be found in Google Cloud Storage.");
    }

    blob.downloadTo(file.toPath());
    return file;
  }

  @Override
  public void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket) {
    BlobId blobId = getBlobIdFromFileId(fileId, fileBucket);
    Blob blob = getStorage().get(blobId);
    if (blob == null) {
      throw new WingsException(
          " File with id " + fileId + " or blob id " + blobId + " can't be found in Google Cloud Storage.");
    }

    byte[] content = blob.getContent();
    try {
      IOUtils.write(content, op);
    } catch (IOException e) {
      throw new WingsException(READ_FILE_FROM_GCP_STORAGE_FAILED, e);
    }
  }

  @Override
  public InputStream openDownloadStream(String fileId, FileBucket fileBucket) {
    BlobId blobId = getBlobIdFromFileId(fileId, fileBucket);
    Blob blob = getStorage().get(blobId);
    if (blob == null) {
      throw new WingsException(
          " File with id " + fileId + " or blob id " + blobId + " can't be found in Google Cloud Storage.");
    }

    byte[] content = blob.getContent();
    return new ByteArrayInputStream(content);
  }

  @Override
  public FileMetadata getFileMetadata(String fileId, FileBucket fileBucket) {
    GoogleCloudFileIdComponent component = parseGoogleCloudFileId(fileId);
    BlobId blobId = BlobId.of(getBucketName(fileBucket), component.filePath);

    GcsFileMetadata gcsFileMetadata = getFileMetadataByFileId(fileId);
    if (gcsFileMetadata == null) {
      throw new WingsException(
          " File with id " + fileId + " or blob id " + blobId + " can't be found in Google Cloud Storage.");
    } else {
      return FileMetadata.builder()
          .fileName(gcsFileMetadata.getFileName())
          .fileLength(gcsFileMetadata.getFileLength())
          .checksum(gcsFileMetadata.getChecksum())
          .checksumType(gcsFileMetadata.getChecksumType())
          .mimeType(gcsFileMetadata.getMimeType())
          .accountId(component.accountId)
          .fileUuid(component.fileUuid)
          .metadata(gcsFileMetadata.getOthers())
          .build();
    }
  }

  @Override
  public boolean updateParentEntityIdAndVersion(Class entityClass, String entityId, Integer version, String fileId,
      Map<String, Object> others, FileBucket fileBucket) {
    // Note that 'entityClass' won't be persisted since it's not used at all.
    // Update the gcs file metadata with entity class/id/version info.
    return updateGcsFileMetadata(fileId, entityId, version, others);
  }

  @Override
  public List<String> getAllFileIds(String entityId, FileBucket fileBucket) {
    return getAllFileIdsFromGcsFileMetadata(entityId, fileBucket);
  }

  @Override
  public String getLatestFileId(String entityId, FileBucket fileBucket) {
    return getLatestFileIdFromGcsFileMetadata(entityId, fileBucket);
  }

  @Override
  public String getFileIdByVersion(String entityId, int version, FileBucket fileBucket) {
    return getFileIdFromGcsFileMetadataByVersion(entityId, version, fileBucket);
  }

  @Override
  public String uploadFromStream(
      String filename, BoundedInputStream in, FileBucket fileBucket, Map<String, Object> metaData) {
    Map<String, String> stringMap = new HashMap<>();
    for (Entry<String, Object> entry : metaData.entrySet()) {
      // Only put string format metadata in as GCS only access String/String metadata mappings
      if (entry.getValue() instanceof String) {
        stringMap.put(entry.getKey(), (String) entry.getValue());
      }
    }

    BlobId blobId = BlobId.of(getBucketName(fileBucket), filename);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setMetadata(stringMap).build();
    try {
      getStorage().create(blobInfo, IOUtils.toByteArray(in));
      return generateFileId(blobId);
    } catch (IOException e) {
      throw new WingsException(ErrorCode.SAVE_FILE_INTO_GCP_STORAGE_FAILED, e);
    }
  }

  @Override
  public void deleteAllFilesForEntity(String entityId, FileBucket fileBucket) {
    getAllFileIds(entityId, fileBucket).forEach(fileId -> { deleteFile(fileId, fileBucket); });
  }

  private String generateFileId(BlobId blobId) {
    String idString = GCS_ID_PREFIX + blobId.getName();
    return Base64.getEncoder().encodeToString(idString.getBytes(Charset.defaultCharset()));
  }

  private BlobId getBlobIdFromFileId(String fileId, FileBucket fileBucket) {
    GoogleCloudFileIdComponent component = parseGoogleCloudFileId(fileId);
    return BlobId.of(getBucketName(fileBucket), component.filePath);
  }

  void initialize() {
    String googleCredentialsPath = System.getenv(GOOGLE_APPLICATION_CREDENTIALS_PATH);
    if (isEmpty(googleCredentialsPath) || !new File(googleCredentialsPath).exists()) {
      throw new WingsException("Invalid credentials found at " + googleCredentialsPath);
    }

    if (isEmpty(configuration.getClusterName())) {
      throw new WingsException("Cluster name should be configured when google cloud storage is used for file storage!");
    }

    createFileBucketsInGCS();

    logger.info("Google cloud storage based file service has been initialized.");
  }

  /**
   * This method should create the buckets for all FileBucket types in Google Cloud Storage post Guice initialization of
   * this class. Default to NEARLINE storage class. See pricing in the following page:
   *    https://cloud.google.com/storage/docs/storage-classes
   */
  private void createFileBucketsInGCS() {
    String clusterName = configuration.getClusterName();
    Page<Bucket> buckets = getStorage().list(BucketListOption.pageSize(500), BucketListOption.prefix(clusterName));
    Set<String> bucketNames = new HashSet<>();
    for (Bucket bucket : buckets.iterateAll()) {
      bucketNames.add(bucket.getName());
    }

    FileBucket[] fileBuckets = FileBucket.values();
    for (FileBucket fileBucket : fileBuckets) {
      String bucketName = getBucketName(fileBucket);
      if (bucketNames.contains(bucketName)) {
        logger.info("Bucket with name '{}' exists in Google Cloud Storage already.", bucketName);
      } else {
        try {
          getStorage().create(
              BucketInfo.newBuilder(bucketName).setStorageClass(StorageClass.NEARLINE).setLocation("us").build());
          logger.info("Bucket with name '{}' created in Google Cloud Storage.", bucketName);
        } catch (Exception e) {
          logger.warn(
              "Creation of bucket in Google Cloud Storage '{}' failed with error: '{}', the bucket may have been created already "
                  + "or the current account doesn't have permission to create bucket.",
              bucketName, e.getMessage());
        }
      }
    }
  }

  private String getBucketName(FileBucket fileBucket) {
    return configuration.getClusterName() + "-" + fileBucket.representationName();
  }

  private Storage getStorage() {
    Storage result = storage;
    if (result == null) {
      synchronized (GoogleCloudFileServiceImpl.class) {
        result = storage;
        if (result == null) {
          storage = result = StorageOptions.getDefaultInstance().getService();
        }
      }
    }
    return result;
  }

  private boolean updateGcsFileMetadata(
      String gcsFileId, String entityId, Integer version, Map<String, Object> others) {
    GcsFileMetadata gcsFileMetadata =
        wingsPersistence.createQuery(GcsFileMetadata.class).filter("gcsFileId", gcsFileId).get();

    UpdateOperations<GcsFileMetadata> updateOperations = wingsPersistence.createUpdateOperations(GcsFileMetadata.class);
    if (entityId != null) {
      updateOperations.set("entityId", entityId);
    }
    if (version != null) {
      updateOperations.set("version", version);
    }
    if (isNotEmpty(others)) {
      updateOperations.set("others", others);
    }

    UpdateResults results = wingsPersistence.update(gcsFileMetadata, updateOperations);
    return results.getUpdatedCount() > 0;
  }

  private GcsFileMetadata getFileMetadataByFileId(String gcsFileId) {
    return wingsPersistence.createQuery(GcsFileMetadata.class, excludeAuthority).filter("gcsFileId", gcsFileId).get();
  }

  private List<String> getAllFileIdsFromGcsFileMetadata(String entityId, FileBucket fileBucket) {
    List<GcsFileMetadata> gcsFileMetadatas = wingsPersistence.createQuery(GcsFileMetadata.class, excludeAuthority)
                                                 .filter("entityId", entityId)
                                                 .filter("fileBucket", fileBucket)
                                                 .asList();

    return gcsFileMetadatas.stream()
        .map(gcsFileMetadata -> gcsFileMetadata.getGcsFileId())
        .distinct()
        .collect(Collectors.toList());
  }

  private String getLatestFileIdFromGcsFileMetadata(String entityId, FileBucket fileBucket) {
    GcsFileMetadata gcsFileMetadata = wingsPersistence.createQuery(GcsFileMetadata.class, excludeAuthority)
                                          .filter("entityId", entityId)
                                          .filter("fileBucket", fileBucket)
                                          .order("-createdAt")
                                          .get();

    return gcsFileMetadata == null ? null : gcsFileMetadata.getGcsFileId();
  }

  private String getFileIdFromGcsFileMetadataByVersion(String entityId, Integer version, FileBucket fileBucket) {
    GcsFileMetadata gcsFileMetadata = wingsPersistence.createQuery(GcsFileMetadata.class, excludeAuthority)
                                          .filter("entityId", entityId)
                                          .filter("version", version)
                                          .filter("fileBucket", fileBucket)
                                          .get();

    return gcsFileMetadata == null ? null : gcsFileMetadata.getGcsFileId();
  }

  private void deleteGcsFileMetadataByGcsFileId(String gcsFileId) {
    GcsFileMetadata mapping = wingsPersistence.createQuery(GcsFileMetadata.class).filter("gcsFileId", gcsFileId).get();
    if (mapping != null) {
      wingsPersistence.delete(mapping);
    }
  }
}
