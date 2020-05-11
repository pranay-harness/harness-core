package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.SftpConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface SftpBuildService extends BuildService<SftpConfig> {
  @Deprecated
  @Override
  @DelegateTaskType(TaskType.SFTP_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, SftpConfig sftpConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.SFTP_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(SftpConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.SFTP_GET_ARTIFACT_PATHS)
  List<String> getArtifactPathsByStreamType(
      SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails, String streamType);
}