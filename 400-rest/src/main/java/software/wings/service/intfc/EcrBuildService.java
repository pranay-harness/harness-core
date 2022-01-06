/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 7/16/17.
 */
@OwnedBy(CDC)
public interface EcrBuildService extends BuildService<AwsConfig> {
  /**
   * Gets builds.
   *
   * @param appId                    the app id
   * @param artifactStreamAttributes the artifact stream attributes
   * @param awsConfig                the aws cloud provider config
   * @return the builds
   */
  @Override
  @DelegateTaskType(TaskType.ECR_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.ECR_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(
      AwsConfig config, List<EncryptedDataDetail> encryptionDetails, ArtifactStreamAttributes artifactStreamAttributes);

  @Override
  @DelegateTaskType(TaskType.ECR_GET_PLANS)
  Map<String, String> getPlans(AwsConfig config, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.ECR_GET_ARTIFACT_PATHS)
  List<String> getArtifactPaths(
      String region, String groupId, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.ECR_GET_LABELS)
  List<Map<String, String>> getLabels(ArtifactStreamAttributes artifactStreamAttributes, List<String> buildNos,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}
