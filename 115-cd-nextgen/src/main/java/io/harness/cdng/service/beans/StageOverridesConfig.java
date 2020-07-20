package io.harness.cdng.service.beans;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StageOverridesConfig {
  List<String> useArtifactOverrideSets;
  ArtifactListConfig artifacts;
  List<String> useManifestOverrideSets;
  List<ManifestConfigWrapper> manifests;
}
