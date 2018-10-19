package software.wings.service.intfc;

import software.wings.beans.EcrConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 7/16/17.
 */
public interface EcrClassicBuildService extends BuildService<EcrConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param ecrConfig             the ecr config
   * @return the builds
   */
  @DelegateTaskType(TaskType.ECR_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, EcrConfig ecrConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @DelegateTaskType(TaskType.ECR_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(EcrConfig config, List<EncryptedDataDetail> encryptedDataDetails);

  @DelegateTaskType(TaskType.ECR_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(
      EcrConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes);

  @DelegateTaskType(TaskType.ECR_GET_PLANS)
  Map<String, String> getPlans(EcrConfig config, List<EncryptedDataDetail> encryptionDetails);
}