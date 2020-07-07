package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
@Builder
public class ServiceSpec implements Serializable {
  String deploymentType;
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;
}
