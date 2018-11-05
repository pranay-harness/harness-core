package software.wings.service.intfc;

import software.wings.beans.SmbConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface SmbBuildService extends BuildService<SmbConfig> {
  @DelegateTaskType(TaskType.SMB_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, SmbConfig smbConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.SMB_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(SmbConfig config, List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.SMB_GET_SMB_PATHS)
  List<String> getSmbPaths(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails);
}
