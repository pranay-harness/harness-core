package software.wings.delegate.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.InvalidRequestException;
import okhttp3.MediaType;
import okhttp3.MultipartBody.Part;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.common.Constants;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.managerclient.ManagerClient;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rishi on 12/19/16.
 */
@Singleton
@ValidateOnExecution
public class DelegateFileManagerImpl implements DelegateFileManager {
  private static final int DEFAULT_MAX_CACHED_ARTIFACT = 2;
  private ManagerClient managerClient;
  private DelegateConfiguration delegateConfiguration;

  private static final LoadingCache<String, Object> fileIdLocks =
      CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build(CacheLoader.from(Object::new));

  private static final String ARTIFACT_REPO_BASE_DIR = "./repository/artifacts/";
  private static final String ARTIFACT_REPO_TMP_DIR = "./repository/artifacts/tmp/";
  private static final Logger logger = LoggerFactory.getLogger(DelegateFileManagerImpl.class);

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  @Inject
  public DelegateFileManagerImpl(ManagerClient managerClient, DelegateConfiguration delegateConfiguration) {
    this.managerClient = managerClient;
    this.delegateConfiguration = delegateConfiguration;
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this ::deleteCachedArtifacts, 1000, 5 * 60 * 1000,
        TimeUnit.MILLISECONDS); // periodic cleanup for cached artifacts
  }

  @Override
  public InputStream downloadArtifactByFileId(FileBucket bucket, String fileId, String accountId)
      throws IOException, ExecutionException {
    logger.info("Downloading file:[{}] , bucket:[{}], accountId:[{}]", fileId, bucket, accountId);
    synchronized (fileIdLocks.get(fileId)) { // Block all thread only one gets to enter
      File file = new File(ARTIFACT_REPO_BASE_DIR, fileId);
      logger.info("check if file:[{}] exists at location: [{}]", fileId, file.getAbsolutePath());
      if (!file.isDirectory() && file.exists()) {
        logger.info("file:[{}] found locally", fileId);
        return new FileInputStream(file);
      }
      logger.info("file:[{}] doesn't exist locally. Download from manager", fileId);

      InputStream inputStream = downloadByFileId(bucket, fileId, accountId);

      logger.info("Input stream acquired for file:[{}]. Saving locally", fileId);

      File downloadedFile = new File(ARTIFACT_REPO_TMP_DIR, fileId);
      FileUtils.copyInputStreamToFile(inputStream, downloadedFile);
      logger.info("file:[{}] saved in tmp location:[{}]", fileId, downloadedFile.getAbsolutePath());

      FileUtils.moveFile(downloadedFile, file);
      logger.info("file:[{}] moved to  final destination:[{}]", fileId, file.getAbsolutePath());

      logger.info("file:[{}] is ready for read access", fileId);

      logger.info("check if downloaded fileId[{}] exists locally", fileId);
      if (!file.isDirectory() && file.exists()) {
        logger.info("fileId[{}] found locally", fileId);
        return new FileInputStream(file);
      }

      logger.error("fileId[{}] could not be found", fileId);
      throw new InvalidRequestException("File couldn't be downloaded");
    }
  }

  @Override
  public InputStream downloadArtifactAtRuntime(ArtifactStreamAttributes artifactStreamAttributes, String accountId,
      String appId, String activityId, String commandUnitName, String hostName) throws IOException, ExecutionException {
    Map<String, String> metadata = artifactStreamAttributes.getMetadata();
    String artifactFileSize = metadata.get(Constants.ARTIFACT_FILE_SIZE);
    if (Long.parseLong(artifactFileSize) > Constants.ARTIFACT_FILE_SIZE_LIMIT) {
      throw new InvalidRequestException("Artifact file size exceeds 4GB. Not downloading file.");
    }
    String buildNo = metadata.get(Constants.BUILD_NO);
    String key = "_" + artifactStreamAttributes.getArtifactStreamId() + "-" + buildNo;
    synchronized (fileIdLocks.get(key)) {
      File file = new File(ARTIFACT_REPO_BASE_DIR, key);
      logger.info("check if artifact:[{}] exists at location: [{}]", key, file.getAbsolutePath());
      if (!file.isDirectory() && file.exists()) {
        logger.info("artifact:[{}] found locally", key);
        return new FileInputStream(file);
      }
      logger.info("file:[{}] doesn't exist locally. Download from manager", key);

      Pair<String, InputStream> pair = artifactCollectionTaskHelper.downloadArtifactAtRuntime(
          artifactStreamAttributes, accountId, appId, activityId, commandUnitName, hostName);
      logger.info("Input stream acquired for file:[{}]. Saving locally", key);
      File downloadedFile = new File(ARTIFACT_REPO_TMP_DIR, key);
      FileUtils.copyInputStreamToFile(pair.getRight(), downloadedFile);
      logger.info("file:[{}] saved in tmp location:[{}]", key, downloadedFile.getAbsolutePath());

      FileUtils.moveFile(downloadedFile, file);
      logger.info("file:[{}] moved to  final destination:[{}]", key, file.getAbsolutePath());

      logger.info("file:[{}] is ready for read access", key);

      logger.info("check if downloaded file [{}] exists locally", key);
      if (!file.isDirectory() && file.exists()) {
        logger.info("file[{}] found locally", key);
        return new FileInputStream(file);
      }

      logger.error("file[{}] could not be found", key);
      throw new InvalidRequestException("File couldn't be downloaded");
    }
  }

  public Long getArtifactFileSize(ArtifactStreamAttributes artifactStreamAttributes) {
    return artifactCollectionTaskHelper.getArtifactFileSize(artifactStreamAttributes);
  }

  @Override
  public String getFileIdByVersion(FileBucket fileBucket, String entityId, int version, String accountId)
      throws IOException {
    return execute(managerClient.getFileIdByVersion(entityId, fileBucket, version, accountId)).getResource();
  }

  @Override
  public InputStream downloadByFileId(FileBucket bucket, String fileId, String accountId) throws IOException {
    Response<ResponseBody> response = null;
    try {
      response = managerClient.downloadFile(fileId, bucket, accountId).execute();
      if (response.body() == null) {
        return null;
      }
      return response.body().byteStream();
    } finally {
      if (response != null && !response.isSuccessful()) {
        response.errorBody().close();
      }
    }
  }

  @Override
  public InputStream downloadByConfigFileId(String fileId, String accountId, String appId, String activityId)
      throws IOException {
    Response<ResponseBody> response = null;
    try {
      response = managerClient.downloadFile(fileId, accountId, appId, activityId).execute();
      return response.body().byteStream();
    } finally {
      if (response != null && !response.isSuccessful()) {
        response.errorBody().close();
      }
    }
  }

  @Override
  public DelegateFile getMetaInfo(FileBucket fileBucket, String fileId, String accountId) throws IOException {
    return execute(managerClient.getMetaInfo(fileId, fileBucket, accountId)).getResource();
  }

  @Override
  public void deleteCachedArtifacts() {
    try {
      File[] files = new File(ARTIFACT_REPO_BASE_DIR).listFiles();
      Integer maxCachedArtifacts = delegateConfiguration.getMaxCachedArtifacts() != null
          ? delegateConfiguration.getMaxCachedArtifacts()
          : DEFAULT_MAX_CACHED_ARTIFACT;
      maxCachedArtifacts += 1; // adjustment for internal 'temp' directory
      if (files != null && files.length > maxCachedArtifacts) {
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int idx = 0; idx < files.length - maxCachedArtifacts; idx++) {
          File file = files[idx];
          synchronized (fileIdLocks.get(file.getName())) {
            if (file.exists() && !file.isDirectory()) {
              boolean deleted = file.delete();
              if (deleted) {
                logger.info("Successfully deleted Artifact file: [{}]", file.getName());
              }
            }
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Error in deleting cached artifact file", ex);
    }
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  private void upload(DelegateFile delegateFile, File content) throws IOException {
    RequestBody filename = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), "file");
    logger.info("Uploading file name {} ", delegateFile.getLocalFilePath());
    // create RequestBody instance from file
    RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), content);

    // MultipartBody.Part is used to send also the actual file name
    Part part = Part.createFormData("file", delegateFile.getFileName(), requestFile);
    Response<RestResponse<String>> response = managerClient
                                                  .uploadFile(delegateFile.getDelegateId(), delegateFile.getTaskId(),
                                                      delegateFile.getAccountId(), delegateFile.getBucket(), part)
                                                  .execute();
    delegateFile.setFileId(response.body().getResource());
    logger.info("Uploaded delegate file id {} ", delegateFile.getFileId());
  }

  @Override
  public DelegateFile upload(DelegateFile delegateFile, InputStream contentSource) {
    File file = new File(delegateConfiguration.getLocalDiskPath(), generateUuid());
    logger.info("File local name {} for delegate file created", file.getName());
    try {
      FileOutputStream fout = new FileOutputStream(file);
      IOUtils.copy(contentSource, fout);
      fout.close();
      upload(delegateFile, file);
      logger.info("File name {} with file id {} uploaded successfully", file.getName(), delegateFile.getFileId());
    } catch (Exception e) {
      logger.warn("Error uploading file: " + file.getName(), e);
    } finally {
      try {
        if (!file.delete()) {
          logger.warn("Could not delete file: {}", file.getName());
        }
      } catch (Exception e) {
        logger.warn("Error deleting file: " + file.getName(), e);
      }
    }

    return delegateFile;
  }
}
