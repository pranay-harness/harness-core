package software.wings.service.intfc;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.SftpBuildParameters;
import software.wings.beans.SftpConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface SftpBuildService extends BuildService<SftpConfig> {
  @Deprecated
  @Override
  default List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getBuilds(SftpBuildParameters.builder()
                         .appId(appId)
                         .artifactStreamAttributes(artifactStreamAttributes)
                         .sftpConfig(sftpConfig)
                         .encryptionDetails(encryptionDetails)
                         .build());
  }

  @DelegateTaskType(TaskType.SFTP_GET_BUILDS) List<BuildDetails> getBuilds(SftpBuildParameters build);

  @Override
  @DelegateTaskType(TaskType.SFTP_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(SftpConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.SFTP_GET_ARTIFACT_PATHS)
  List<String> getArtifactPathsByStreamType(
      SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails, String streamType);
}