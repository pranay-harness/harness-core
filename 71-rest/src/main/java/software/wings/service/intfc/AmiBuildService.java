package software.wings.service.intfc;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

/**
 * Created by sgurubelli on 12/15/17.
 */
public interface AmiBuildService extends BuildService<AwsConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param awsConfig                the aws cloud provider config
   * @return the builds
   */
  @Override
  @DelegateTaskType(TaskType.AMI_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails);
}
