package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.SmbConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface SmbBuildService extends BuildService<SmbConfig> {
  @Override
  @DelegateTaskType(TaskType.SMB_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, SmbConfig smbConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.SMB_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(SmbConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.SMB_GET_SMB_PATHS)
  List<String> getSmbPaths(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails);
}
