package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.state.ManifestListConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceSpec {
  String deploymentType;
  ArtifactListConfig artifacts;
  private ManifestListConfig manifests;
}
