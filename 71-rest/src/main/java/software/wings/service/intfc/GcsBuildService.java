package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
public interface GcsBuildService extends BuildService<GcpConfig> {
  @Override
  @DelegateTaskType(TaskType.GCS_GET_BUCKETS)
  Map<String, String> getBuckets(GcpConfig config, String projectId, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.GCS_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String bucketName, String groupId, GcpConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.GCS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, GcpConfig config,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.GCS_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, GcpConfig config,
      List<EncryptedDataDetail> encryptionDetails, int limit);

  @Override @DelegateTaskType(TaskType.GCS_GET_PROJECT_ID) String getProjectId(GcpConfig gcpConfig);
}
