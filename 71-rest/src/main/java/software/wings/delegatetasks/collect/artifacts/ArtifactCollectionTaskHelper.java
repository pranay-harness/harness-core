package software.wings.delegatetasks.collect.artifacts;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnknownArtifactStreamTypeException;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageFileInfo;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class that has common collection logic that's used by all the artifact collection tasks.
 * @author rktummala
 */
@Singleton
@Slf4j
public class ArtifactCollectionTaskHelper {
  @Inject private DelegateFileManager delegateFileManager;
  @Inject private AmazonS3Service amazonS3Service;
  @Inject private DelegateLogService logService;
  @Inject private ArtifactoryService artifactoryService;
  @Inject private AzureArtifactsService azureArtifactsService;

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
    if (fileRes == null || fileRes.getFileId() == null) {
      logger.error(
          "Failed to upload file name {} for artifactPath {} to manager. Artifact files will be uploaded during the deployment of Artifact Check Step",
          fileInfo.getKey(), artifactPath);
    } else {
      logger.info("Uploaded the file name {} and fileUuid {} for artifactPath {}", fileInfo.getKey(),
          fileRes.getFileId(), artifactPath);
      ArtifactFile artifactFile = new ArtifactFile();
      artifactFile.setFileUuid(fileRes.getFileId());
      artifactFile.setName(fileInfo.getKey());
      res.addData(artifactFile);
    }
  }

  public Pair<String, InputStream> downloadArtifactAtRuntime(ArtifactStreamAttributes artifactStreamAttributes,
      String accountId, String appId, String activityId, String commandUnitName, String hostName) {
    Map<String, String> metadata = artifactStreamAttributes.getMetadata();
    Pair<String, InputStream> pair;
    ArtifactStreamType artifactStreamType =
        ArtifactStreamType.valueOf(artifactStreamAttributes.getArtifactStreamType());
    switch (artifactStreamType) {
      case AMAZON_S3:
        logger.info("Downloading artifact [{}] from bucket :[{}]", metadata.get(ArtifactMetadataKeys.artifactFileName),
            metadata.get(ArtifactMetadataKeys.bucketName));
        saveExecutionLog("Metadata only option set for AMAZON_S3. Starting download of artifact: "
                + metadata.get(ArtifactMetadataKeys.artifactFileName)
                + " from bucket: " + metadata.get(ArtifactMetadataKeys.bucketName)
                + " with key: " + metadata.get(ArtifactMetadataKeys.key),
            RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        pair = amazonS3Service.downloadArtifact((AwsConfig) artifactStreamAttributes.getServerSetting().getValue(),
            artifactStreamAttributes.getArtifactServerEncryptedDataDetails(),
            metadata.get(ArtifactMetadataKeys.bucketName), metadata.get(ArtifactMetadataKeys.key));
        if (pair != null) {
          saveExecutionLog(
              "AMAZON_S3: Download complete for artifact: " + metadata.get(ArtifactMetadataKeys.artifactFileName)
                  + " from bucket: " + metadata.get(ArtifactMetadataKeys.bucketName)
                  + " with key: " + metadata.get(ArtifactMetadataKeys.key),
              RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        } else {
          saveExecutionLog(
              "AMAZON_S3: Download failed for artifact: " + metadata.get(ArtifactMetadataKeys.artifactFileName)
                  + " from bucket: " + metadata.get(ArtifactMetadataKeys.bucketName)
                  + " with key: " + metadata.get(ArtifactMetadataKeys.key),
              FAILURE, accountId, appId, activityId, commandUnitName, hostName);
        }
        return pair;
      case ARTIFACTORY:
        logger.info("Downloading artifact [{}] from artifactory at path :[{}]",
            metadata.get(ArtifactMetadataKeys.artifactFileName), metadata.get(ArtifactMetadataKeys.artifactPath));
        saveExecutionLog("Metadata only option set for ARTIFACTORY. Starting download of artifact: "
                + metadata.get(ArtifactMetadataKeys.artifactPath),
            RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        pair = artifactoryService.downloadArtifact(
            (ArtifactoryConfig) artifactStreamAttributes.getServerSetting().getValue(),
            artifactStreamAttributes.getArtifactServerEncryptedDataDetails(), artifactStreamAttributes.getJobName(),
            metadata);
        if (pair != null) {
          saveExecutionLog(
              "ARTIFACTORY: Download complete for artifact: " + metadata.get(ArtifactMetadataKeys.artifactFileName),
              RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        } else {
          saveExecutionLog(
              "ARTIFACTORY: Download failed for artifact: " + metadata.get(ArtifactMetadataKeys.artifactFileName),
              FAILURE, accountId, appId, activityId, commandUnitName, hostName);
        }
        return pair;
      case AZURE_ARTIFACTS:
        logger.info("Downloading artifact [{}] from azure artifacts with version: [{}]",
            metadata.get(ArtifactMetadataKeys.artifactFileName), metadata.get(ArtifactMetadataKeys.version));
        saveExecutionLog("Metadata only option set for AZURE ARTIFACTS. Starting download of artifact: "
                + metadata.get(ArtifactMetadataKeys.artifactFileName),
            RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        pair = azureArtifactsService.downloadArtifact(
            (AzureArtifactsConfig) artifactStreamAttributes.getServerSetting().getValue(),
            artifactStreamAttributes.getArtifactServerEncryptedDataDetails(), artifactStreamAttributes, metadata);
        if (pair != null) {
          saveExecutionLog(
              "AZURE ARTIFACTS: Download complete for artifact: " + metadata.get(ArtifactMetadataKeys.artifactFileName),
              RUNNING, accountId, appId, activityId, commandUnitName, hostName);
        } else {
          saveExecutionLog(
              "AZURE ARTIFACTS: Download failed for artifact: " + metadata.get(ArtifactMetadataKeys.artifactFileName),
              FAILURE, accountId, appId, activityId, commandUnitName, hostName);
        }
        return pair;
      default:
        throw new UnknownArtifactStreamTypeException(artifactStreamType.name());
    }
  }

  public Long getArtifactFileSize(ArtifactStreamAttributes artifactStreamAttributes) {
    Map<String, String> metadata = artifactStreamAttributes.getMetadata();
    ArtifactStreamType artifactStreamType =
        ArtifactStreamType.valueOf(artifactStreamAttributes.getArtifactStreamType());
    switch (artifactStreamType) {
      case AMAZON_S3:
        logger.info("Getting artifact file size for artifact " + metadata.get(ArtifactMetadataKeys.artifactFileName)
            + " in bucket: " + metadata.get(ArtifactMetadataKeys.bucketName)
            + " with key: " + metadata.get(ArtifactMetadataKeys.key));
        return amazonS3Service.getFileSize((AwsConfig) artifactStreamAttributes.getServerSetting().getValue(),
            artifactStreamAttributes.getArtifactServerEncryptedDataDetails(),
            metadata.get(ArtifactMetadataKeys.bucketName), metadata.get(ArtifactMetadataKeys.key));
      case ARTIFACTORY:
        logger.info("Getting artifact file size for artifact: " + metadata.get(ArtifactMetadataKeys.artifactPath));
        return artifactoryService.getFileSize(
            (ArtifactoryConfig) artifactStreamAttributes.getServerSetting().getValue(),
            artifactStreamAttributes.getArtifactServerEncryptedDataDetails(), artifactStreamAttributes.getMetadata());
      case AZURE_ARTIFACTS:
        logger.info("Getting artifact file size for artifact: " + metadata.get(ArtifactMetadataKeys.version));
        if (!metadata.containsKey(ArtifactMetadataKeys.artifactFileName)
            || isBlank(metadata.get(ArtifactMetadataKeys.artifactFileName))) {
          throw new InvalidArgumentsException(ImmutablePair.of(ArtifactMetadataKeys.artifactFileName, "not found"));
        }

        List<AzureArtifactsPackageFileInfo> fileInfos = azureArtifactsService.listFiles(
            (AzureArtifactsConfig) artifactStreamAttributes.getServerSetting().getValue(),
            artifactStreamAttributes.getArtifactServerEncryptedDataDetails(), artifactStreamAttributes,
            artifactStreamAttributes.getMetadata(), false);
        if (isEmpty(fileInfos)) {
          throw new InvalidArgumentsException(ImmutablePair.of(
              ArtifactMetadataKeys.version, metadata.getOrDefault(ArtifactMetadataKeys.version, "unknown")));
        }

        String fileName = metadata.get(ArtifactMetadataKeys.artifactFileName);
        Optional<AzureArtifactsPackageFileInfo> optional =
            fileInfos.stream().filter(fileInfo -> fileName.equals(fileInfo.getName())).findFirst();
        if (!optional.isPresent()) {
          throw new InvalidArgumentsException(ImmutablePair.of(
              ArtifactMetadataKeys.version, metadata.getOrDefault(ArtifactMetadataKeys.version, "unknown")));
        }

        return optional.get().getSize();
      default:
        throw new UnknownArtifactStreamTypeException(artifactStreamType.name());
    }
  }

  private void saveExecutionLog(String line, CommandExecutionStatus commandExecutionStatus, String accountId,
      String appId, String activityId, String commandUnitName, String hostName) {
    logService.save(accountId,
        aLog()
            .withAppId(appId)
            .withActivityId(activityId)
            .withLogLevel(LogLevel.INFO)
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .withCommandUnitName(commandUnitName)
            .withHostName(hostName)
            .build());
  }
}
