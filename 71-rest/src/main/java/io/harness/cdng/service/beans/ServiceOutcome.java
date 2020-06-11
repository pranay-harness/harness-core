package io.harness.cdng.service.beans;

import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class ServiceOutcome implements Outcome {
  String identifier;
  String displayName;
  String deploymentType;
  Artifacts artifacts;
  List<ManifestAttributes> manifests;
  Override overrides;

  @Data
  @Builder
  public static class Artifacts {
    ArtifactOutcome primary;
    @Singular Map<String, ArtifactOutcome> sidecars;
  }

  @Data
  @Builder
  public static class Override {
    List<ManifestAttributes> manifests;
  }
}
