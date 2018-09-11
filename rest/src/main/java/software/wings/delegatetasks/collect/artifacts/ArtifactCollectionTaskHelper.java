package software.wings.delegatetasks.collect.artifacts;

import static java.lang.String.format;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * Helper class that has common collection logic that's used by all the artifact collection tasks.
 * @author rktummala
 */
@Singleton
public class ArtifactCollectionTaskHelper {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionTaskHelper.class);
  @Inject private DelegateFileManager delegateFileManager;
  @Inject private AmazonS3Service amazonS3Service;
  @Inject private DelegateLogService logService;

  public void addDataToResponse(Pair<String, InputStream> fileInfo, String artifactPath, ListNotifyResponseData res,
      String delegateId, String taskId, String accountId) throws FileNotFoundException {
    if (fileInfo == null) {
      throw new FileNotFoundException("Unable to get artifact for path " + artifactPath);
    }
    InputStream in = fileInfo.getValue();
    logger.info("Uploading the file {} for artifact path {}", fileInfo.getKey(), artifactPath);

    DelegateFile delegateFile = aDelegateFile()
                                    .withBucket(FileBucket.ARTIFACTS)
                                    .withFileName(fileInfo.getKey())
                                    .withDelegateId(delegateId)
                                    .withTaskId(taskId)
                                    .withAccountId(accountId)
                                    .build(); // TODO: more about delegate and task info
    DelegateFile fileRes = delegateFileManager.upload(delegateFile, in);
    logger.info("Uploaded the file name {} and fileUuid {} for artifactPath {}", fileInfo.getKey(), fileRes.getFileId(),
        artifactPath);
    ArtifactFile artifactFile = new ArtifactFile();
    artifactFile.setFileUuid(fileRes.getFileId());
    artifactFile.setName(fileInfo.getKey());
    res.addData(artifactFile);
  }

  public Pair<String, InputStream> downloadArtifactAtRuntime(ArtifactStreamAttributes artifactStreamAttributes,
      String accountId, String appId, String activityId, String commandUnitName, String hostName) {
    switch (ArtifactStreamType.valueOf(artifactStreamAttributes.getArtifactStreamType())) {
      case AMAZON_S3:
        Map<String, String> metadata = artifactStreamAttributes.getMetadata();
        logger.info("Downloading artifact [{}] from bucket :[{}]", metadata.get(Constants.ARTIFACT_FILE_NAME),
            metadata.get(Constants.BUCKET_NAME));
        saveExecutionLog("Metadata only option set for AMAZON_S3. Starting download of artifact: "
                + metadata.get(Constants.ARTIFACT_FILE_NAME) + " from bucket: " + metadata.get(Constants.BUCKET_NAME)
                + " with key: " + metadata.get(Constants.KEY),
            RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        Pair<String, InputStream> pair =
            amazonS3Service.downloadArtifact((AwsConfig) artifactStreamAttributes.getServerSetting().getValue(),
                artifactStreamAttributes.getArtifactServerEncryptedDataDetails(), metadata.get(Constants.BUCKET_NAME),
                metadata.get(Constants.KEY));
        if (pair != null) {
          saveExecutionLog("AMAZON_S3: Download complete for artifact: " + metadata.get(Constants.ARTIFACT_FILE_NAME)
                  + " from bucket: " + metadata.get(Constants.BUCKET_NAME)
                  + " with key: " + metadata.get(Constants.KEY),
              RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        } else {
          saveExecutionLog("AMAZON_S3: Download failed for artifact: " + metadata.get(Constants.ARTIFACT_FILE_NAME)
                  + " from bucket: " + metadata.get(Constants.BUCKET_NAME)
                  + " with key: " + metadata.get(Constants.KEY),
              FAILURE, accountId, appId, activityId, commandUnitName, hostName);
        }
        return pair;
      default: { throw new WingsException(ErrorCode.UNKNOWN_ARTIFACT_TYPE); }
    }
  }

  public Long getArtifactFileSize(ArtifactStreamAttributes artifactStreamAttributes) {
    switch (ArtifactStreamType.valueOf(artifactStreamAttributes.getArtifactStreamType())) {
      case AMAZON_S3:
        Map<String, String> metadata = artifactStreamAttributes.getMetadata();
        logger.info("Getting artifact file size for artifact " + metadata.get(Constants.ARTIFACT_FILE_NAME)
            + " in bucket: " + metadata.get(Constants.BUCKET_NAME) + " with key: " + metadata.get(Constants.KEY));
        return amazonS3Service.getFileSize((AwsConfig) artifactStreamAttributes.getServerSetting().getValue(),
            artifactStreamAttributes.getArtifactServerEncryptedDataDetails(), metadata.get(Constants.BUCKET_NAME),
            metadata.get(Constants.KEY));
      default: { throw new WingsException(ErrorCode.UNKNOWN_ARTIFACT_TYPE); }
    }
  }

  private void saveExecutionLog(String line, CommandExecutionStatus commandExecutionStatus, String accountId,
      String appId, String activityId, String commandUnitName, String hostName) {
    logService.save(accountId,
        aLog()
            .withAppId(appId)
            .withActivityId(activityId)
            .withLogLevel(LogLevel.INFO)
            .withLogLine(format(line))
            .withExecutionResult(commandExecutionStatus)
            .withCommandUnitName(commandUnitName)
            .withHostName(hostName)
            .build());
  }
}
