package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class StageOverridesConfig implements Serializable {
  private List<ManifestConfigWrapper> manifests;
  private ArtifactListConfig artifacts;
}
