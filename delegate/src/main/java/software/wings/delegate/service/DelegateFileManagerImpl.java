package software.wings.delegate.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import okhttp3.MediaType;
import okhttp3.MultipartBody.Part;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.exception.WingsException;
import software.wings.managerclient.ManagerClient;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
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

  @Inject
  public DelegateFileManagerImpl(ManagerClient managerClient, DelegateConfiguration delegateConfiguration) {
    this.managerClient = managerClient;
    this.delegateConfiguration = delegateConfiguration;
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this ::deleteCachedArtifacts, 1000, 5 * 60 * 1000,
        TimeUnit.MILLISECONDS); // periodic cleanup for cached artifacts
  }

  @Override
  public InputStream downloadByFileId(FileBucket bucket, String fileId, String accountId, boolean encrypted)
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

      InputStream inputStream = downloadByFileIdInternal(bucket, fileId, accountId, encrypted);
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
      throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", "File couldn't be downloaded");
    }
  }

  @Override
  public String getFileIdByVersion(FileBucket fileBucket, String entityId, int version, String accountId)
      throws IOException {
    return execute(managerClient.getFileIdByVersion(entityId, fileBucket, version, accountId)).getResource();
  }

  private InputStream downloadByFileIdInternal(FileBucket bucket, String fileId, String accountId, boolean encrypted)
      throws IOException {
    Response<ResponseBody> response = null;
    try {
      response = managerClient.downloadFile(fileId, bucket, accountId, encrypted).execute();
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

  private void upload(DelegateFile delegateFile, File content) throws IOException {
    RequestBody filename = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), "file");
    logger.info("Uploading file name {} ", filename);
    // create RequestBody instance from file
    RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), content);

    // MultipartBody.Part is used to send also the actual file name
    Part part = Part.createFormData("file", delegateFile.getFileName(), requestFile);
    Response<RestResponse<String>> response =
        managerClient
            .uploadFile(delegateFile.getDelegateId(), delegateFile.getTaskId(), delegateFile.getAccountId(), part)
            .execute();
    delegateFile.setFileId(response.body().getResource());
    logger.info("Uploaded delegate file id {} ", delegateFile.getFileId());
  }

  @Override
  public DelegateFile upload(DelegateFile delegateFile, InputStream contentSource) {
    File file = new File(delegateConfiguration.getLocalDiskPath(), generateUuid());
    logger.info("File local name {} for delegate file id {} created", file.getName(), delegateFile.getFileId());
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
