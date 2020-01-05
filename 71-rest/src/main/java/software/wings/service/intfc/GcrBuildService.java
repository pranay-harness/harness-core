package software.wings.service.intfc;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 8/2/17
 */
public interface GcrBuildService extends BuildService<GcpConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param gcpConfig                the gcp config
   * @return the builds
   */
  @Override
  @DelegateTaskType(TaskType.GCR_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, GcpConfig gcpConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.GCR_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(
      GcpConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes);

  @Override
  @DelegateTaskType(TaskType.GCR_GET_PLANS)
  Map<String, String> getPlans(GcpConfig config, List<EncryptedDataDetail> encryptionDetails);
}
