/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.DockerConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 1/6/17.
 */
@OwnedBy(CDC)
public interface DockerBuildService extends BuildService<DockerConfig> {
  @Override
  @DelegateTaskType(TaskType.DOCKER_GET_BUILDS)
  List<BuildDetails> getBuilds(String appId, ArtifactStreamAttributes artifactStreamAttributes,
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails);

  @Override
  @DelegateTaskType(TaskType.DOCKER_VALIDATE_ARTIFACT_SERVER)
  boolean validateArtifactServer(DockerConfig config, List<EncryptedDataDetail> encryptedDataDetails);

  @Override
  @DelegateTaskType(TaskType.DOCKER_VALIDATE_ARTIFACT_STREAM)
  boolean validateArtifactSource(DockerConfig config, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes);

  @Override
  @DelegateTaskType(TaskType.DOCKER_GET_LABELS)
  List<Map<String, String>> getLabels(ArtifactStreamAttributes artifactStreamAttributes, List<String> buildNos,
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails);
}
