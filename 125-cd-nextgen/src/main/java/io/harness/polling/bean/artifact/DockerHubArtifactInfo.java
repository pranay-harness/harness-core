/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.bean.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class DockerHubArtifactInfo implements ArtifactInfo {
  String connectorRef;
  String imagePath;

  @Override
  public ArtifactSourceType getType() {
    return ArtifactSourceType.DOCKER_REGISTRY;
  }

  @Override
  public ArtifactConfig toArtifactConfig() {
    return DockerHubArtifactConfig.builder()
        .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
        .imagePath(ParameterField.<String>builder().value(imagePath).build())
        .build();
  }
}
