package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceSpec {
  String deploymentType;
  ArtifactListConfig artifacts;
}
