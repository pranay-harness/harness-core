package io.harness.cdng.service;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StageOverridesConfig {
  List<ManifestConfigWrapper> manifests;
  ArtifactListConfig artifacts;
  List<String> useManifestOverrideSets;
  List<String> useArtifactOverrideSets;
}
