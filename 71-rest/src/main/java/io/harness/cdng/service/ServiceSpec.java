package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.state.ManifestListConfig;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder
public class ServiceSpec implements Serializable {
  String deploymentType;
  ArtifactListConfig artifacts;
  private ManifestListConfig manifests;
}
